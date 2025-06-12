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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.token.TimeOutTokenException;
import net.chamman.moonnight.global.security.principal.SilentUserDetails;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SilentAuthenticationFilter extends AbstractAccessTokenFilter<SilentUserDetails>{
	
	protected JwtProvider jwtTokenProvider;
	protected TokenProvider tokenStore;
	protected SignLogService signLogService;
	protected GuidanceService guidanceService;
	protected SignService signService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
			FilterChain filterChain) throws ServletException, IOException {
		log.debug("*SilentAuthenticationFilter.doFilterInternal 실행.");
		
		// ClientIp
		String clientIp = (String) req.getAttribute("clientIp");
		
		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		// AccessToken
		String accessToken = getAccessToken(isMobileApp, req);
		String refreshToken = getRefreshToken(isMobileApp, req);
		
		// 1. 토큰 null 체크
		if(!validateTokens(new String[] {accessToken,refreshToken})) {
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
		
//		 2. 블랙리스트 확인
		String value = tokenProvider.getBlackListValue(accessToken);
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
				// 리프레쉬 토큰 통해 SignIn Tokens 재발급 실패
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
		} catch (TimeOutTokenException e) {
			try {
//				리프레쉬 
				refresh(refreshToken, clientIp, res, isMobileApp);
				filterChain.doFilter(req, res);
				return;

			} catch (Exception refreshEx) {
				// 리프레쉬 토큰 통해 SignIn Tokens 재발급 실패
				initTokenToCookie(res);
				filterChain.doFilter(req, res);
				return;
			}
		} catch (Exception e) {
			log.error("AT validate 실패 또는 UserDetails 생성 중 실패.",e);
			initTokenToCookie(res);
			filterChain.doFilter(req, res);
			return;
		}
	}
	
	@Override
	protected SilentUserDetails buildUserDetails(Map<String, Object> claims) {
		log.debug("*SilentAuthenticationFilter.buildUserDetails 실행.");

		Object subjectRaw = claims.get("subject");
		if (subjectRaw == null) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT SilentUserDetails 생성중 오류 발생. subject");
		}
		
		int subject = Integer.parseInt(subjectRaw.toString());
		if(subject==0) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT SilentUserDetails 생성중 오류 발생. subject");
		}
		
		String name = (String) claims.get("name");
		if (name == null || name.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT SilentUserDetails 생성중 오류 발생. name");
		}
		
		Object rolesObj = claims.get("roles");
		if (!(rolesObj instanceof List)) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT SilentUserDetails 생성중 오류 발생. roles");
		}
		
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) rolesObj;
		List<GrantedAuthority> authorities =
				roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
		
		return new SilentUserDetails(subject, name, authorities);
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
				|| matcher.match("/favicon.ico", uri);
	}
	
	
}
