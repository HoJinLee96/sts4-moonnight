package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.security.principal.AuthDetails;
import net.chamman.moonnight.global.util.CookieUtil;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractAccessTokenFilter<AuthDetails> {

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
			throws ServletException, IOException {
		log.debug("* JwtAuthFilter.doFilterInternal 실행.");

		// ClientIp
//		String clientIp = (String) req.getAttribute("clientIp");

		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");

		String authToken = null;
		if (isMobileApp) {
			authToken = req.getHeader("X-Auth-Token");
		} else {
			Cookie cookie = WebUtils.getCookie(req, "X-Auth-Token");
			if (cookie != null) {
				authToken = cookie.getValue();
			}
		}

//		 1. 토큰 null 체크
		log.debug("* JwtAuthFilter 토큰 널 체크.");

		if (authToken == null || authToken.isBlank()) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4011, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

//		 2. 블랙리스트 확인
		log.debug("* JwtAuthFilter 토큰 블랙리스트 체크.");

		String value = tokenProvider.getBlackListValue(authToken);
		if (value != null) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

		// 3. 토큰 확인 및 Set
		try {
			setAuthentication(authToken);

			filterChain.doFilter(req, res);
			return;

			// 4. Access Token 만료.
		} catch (TimeOutJwtException e) {
			CookieUtil.addCookie(res, "X-Auth-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}

	}

	@Override
	protected AuthDetails buildUserDetails(Map<String, Object> claims) {
		log.debug("*JwtAuthFilter.buildUserDetails 실행.");

		Object subjectRaw = claims.get("subject");
		if (subjectRaw == null) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. subject");
		}
		int verificationId = Integer.parseInt(subjectRaw.toString());
		if (verificationId == 0) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - subject");
		}

		Object rolesObj = claims.get("roles");
		if (!(rolesObj instanceof List)) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - roles");

		}
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) rolesObj;
		List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		String recipient = (String) claims.get("recipient");
		if (recipient == null || recipient.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL, "JWT AuthUserDetails 생성중 오류 발생. - recipient");
		}

		return new AuthDetails(verificationId, recipient, authorities);
	}

	@Override
	protected void setAuthentication(String authToken) {
		Map<String, Object> claims = jwtProvider.validateAuthToken(authToken);
		AuthDetails userDetails = buildUserDetails(claims);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		return !(uri.startsWith("/api/spem/private/auth") 
				|| uri.startsWith("/api/estimate/private/auth")
				|| uri.startsWith("/api/spem/private/auth/**")
				|| uri.startsWith("/api/estimate/private/auth/**"));
	}

}
