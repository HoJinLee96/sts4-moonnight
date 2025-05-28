package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.security.principal.SilentUserDetails;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;

@Slf4j
@Component
public class SilentAuthenticationFilter extends AbstractAccessTokenFilter<SilentUserDetails>{
	
	@Autowired
	protected JwtProvider jwtTokenProvider;
	@Autowired
	protected TokenProvider tokenStore;
	@Autowired
	protected SignLogService signLogService;
	@Autowired
	protected GuidanceService guidanceService;
	@Autowired
	protected SignService signService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		System.out.println("==========SilentAuthenticationFilter===========");
		
		// ClientIp
		String clientIp = (String) request.getAttribute("clientIp");
		
		// Mobile Or Web
		String clientType = request.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		// AccessToken
		String accessToken = getAccessToken(isMobileApp, request);
		String refreshToken = getRefreshToken(isMobileApp, request);
		
		// 1. 토큰 null 체크
		if(!validateTokens(new String[] {accessToken,refreshToken})) {
			initTokenToCookie(response);
			filterChain.doFilter(request, response);
			return;
		}
		
		// 2. 블랙리스트 확인
		if(!isBlackList(accessToken, clientIp)) {
			initTokenToCookie(response);
			filterChain.doFilter(request, response);
			return;
		}
		
		try {
			// Context.setAuthentication(User)
			setAuthentication(accessToken);
			
			System.out.println("Slient Auth 정상 처리.");
			filterChain.doFilter(request, response);
			
		} catch (ExpiredJwtException e) {
			try {
				// Creating NewTokens 
				Map<String, String> newTokens = signService.refresh(accessToken, refreshToken, clientIp);
				
				// Setting NewTokens 
				setTokenToResponse(newTokens, response, isMobileApp);
				
				// Context.setAuthentication(User)
				setAuthentication(newTokens.get(newTokens.get("accessToken")));
				
				filterChain.doFilter(request, response);
				
			} catch (Exception refreshEx) {
				// Refresh 실패 시 (Refresh Token 만료, 변조 등)
				initTokenToCookie(response);
				log.info("Failed to refresh token. IP: {}, Reason: {}", clientIp, refreshEx.getMessage());
				filterChain.doFilter(request, response);
				return;
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			initTokenToCookie(response);
			filterChain.doFilter(request, response);
			return;
		}
	}
	
	@Override
	protected SilentUserDetails buildUserDetails(Map<String, Object> claims) {
		
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
				|| matcher.match("/favicon.ico", uri);
	}
	
	
}
