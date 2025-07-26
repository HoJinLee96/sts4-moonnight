package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.token.TokenProvider.TokenType;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Slf4j
@Component
public class SilentAuthenticationFilter extends AbstractAccessTokenFilter<CustomUserDetails>{
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
			FilterChain filterChain) throws ServletException, IOException {
		
		// ClientIp
		String clientIp = customInterceptor.extractClientIp(req);
		
		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		log.debug("* [{}] SilentAuthenticationFilter 실행 - clientIp: [{}], isMobileApp: [{}]", req.getRequestURI(), clientIp, isMobileApp);

		// AccessToken
		String accessToken = getAccessToken(isMobileApp, req);
		String refreshToken = getRefreshToken(isMobileApp, req);
		
		log.debug("* AccessToken: [{}], RefreshToken: [{}]", LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM), LogMaskingUtil.maskToken(refreshToken, MaskLevel.MEDIUM));

		// 1. 토큰 null 체크
		if(!validateTokens(new String[] {accessToken,refreshToken})) {
			log.warn("* 토큰 누락 또는 빈 값.");
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
		
//		 2. 블랙리스트 확인
		String value = tokenProvider.getBlackListValue(accessToken);
		log.debug("* 블랙리스트 조회 결과 value: [{}]",value);
//		 2-1. 로그아웃한 토큰
		if(Objects.equals(value, "SIGNOUT")) {
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
//		2-2. 유저 정보 업데이트된 토큰
		} else if(Objects.equals(value, "UPDATE")) {
			try {
				refresh(refreshToken, clientIp, res, isMobileApp);
				tokenProvider.removeToken(TokenType.JWT_BLACKLIST, accessToken);
				filterChain.doFilter(req, res);
				return;
			} catch (Exception refreshEx) {
				log.error("* AccessToken UPDATE 인해 Refresh 중 익셉션 발생.",refreshEx);
				initTokenToCookie(res);
				filterChain.doFilter(req, res);
				return;
			}
		}
		
		// 3. 토큰 확인 및 Set
		try {
			setAuthentication(accessToken);
			
			filterChain.doFilter(req, res);
			return;
			
		// 4. Access Token 만료.
		} catch (TimeOutJwtException e) {
			try {
				log.debug("* AccessToken 만료 확인.");
				refresh(refreshToken, clientIp, res, isMobileApp);
				filterChain.doFilter(req, res);
				return;

			} catch (Exception refreshEx) {
				log.error("* AccessToken 만료 인해 Refresh 중 익셉션 발생.", refreshEx);
				initTokenToCookie(res);
				filterChain.doFilter(req, res);
				return;
			}
		} catch (Exception e) {
			log.error("* AccessToken validate 실패 또는 UserDetails 생성 중 실패.", e);
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
	}
	
	@Override
	protected CustomUserDetails buildUserDetails(Map<String, Object> claims) {
		log.debug("* SilentAuthenticationFilter.buildUserDetails 실행.");
		
		// 복호화 때문에 claims.getSbuject()가 아님.
		Object subjectRaw = claims.get("subject");
		if (subjectRaw == null) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT CustomUserDetails 생성중 오류 발생. subject");
		}
		int userId = Integer.parseInt(subjectRaw.toString());
		if(userId==0) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT CustomUserDetails 생성중 오류 발생. subject");
		}
		
		Object rolesObj = claims.get("roles");
		if (!(rolesObj instanceof List)) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - roles");
		}
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) rolesObj;
		List<GrantedAuthority> authorities =
				roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
		
		String providerStr = (String) claims.get("provider");
		if (providerStr == null || providerStr.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - provider");
		}
		UserProvider userProvider = UserProvider.valueOf(providerStr); // 문자열을 Provider Enum으로 변환
		String email = (String) claims.get("email");
		if (email == null || email.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - email");
		}
		String name = (String) claims.get("name");
		if (name == null || name.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - name");
		}
		
		return new CustomUserDetails(userId, userProvider, email, name, authorities);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		
		// Spring의 AntPathMatcher를 사용
		AntPathMatcher matcher = new AntPathMatcher();
		
		return matcher.match("/css/**", uri)
				|| matcher.match("/js/**", uri)
				|| matcher.match("/images/**", uri)
				|| matcher.match("/swagger-ui/**", uri)
				|| matcher.match("/api-docs/**", uri)
				|| matcher.match("/v3/api-docs/**", uri)
				|| matcher.match("/openapi.yaml", uri)
				|| matcher.match("/.well-known/**", uri)
				|| matcher.match("/favicon.ico", uri)
				|| matcher.match("/jpg/**", uri)
				|| matcher.match("/api/*/private/**", uri);
	}
	
	
}
