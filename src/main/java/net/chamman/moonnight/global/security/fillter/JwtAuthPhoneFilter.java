package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import net.chamman.moonnight.global.security.principal.AuthPhoneDetails;
import net.chamman.moonnight.global.util.HttpServletUtil;

@Component
@Slf4j
public class JwtAuthPhoneFilter extends AbstractAccessTokenFilter<AuthPhoneDetails> {
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
		log.debug("*JwtAuthPhoneFilter.doFilterInternal 실행.");
		
		// ClientIp
		String clientIp = (String) req.getAttribute("clientIp");
		
		// Mobile Or Web
		String clientType = req.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		
		String authPhoneToken = null;
		if(isMobileApp) {
			authPhoneToken = req.getHeader("X-Auth-Phone-Token");
		}else {
			Cookie cookie = WebUtils.getCookie(req, "X-Auth-Phone-Token");
			if(cookie != null) {
				authPhoneToken = cookie.getValue();
			}
		}
		
//		 1. 토큰 null 체크
		if (authPhoneToken == null || authPhoneToken.isBlank()) {
			HttpServletUtil.resSetCookie(res,"X-Auth-Phone-Token", "", Duration.ZERO);
			setErrorResponse(res, 4011, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}
		
//		 2. 블랙리스트 확인
		String value = tokenProvider.getBlackListValue(authPhoneToken);
		if(value!=null) {
			HttpServletUtil.resSetCookie(res,"X-Auth-Phone-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}
		
		// 3. 토큰 확인 및 Set
		try {
			setAuthentication(authPhoneToken);
			
			filterChain.doFilter(req, res);
			return;
			
		// 4. Access Token 만료.
		} catch (TimeOutJwtException e) {
			HttpServletUtil.resSetCookie(res,"X-Auth-Phone-Token", "", Duration.ZERO);
			setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
			filterChain.doFilter(req, res);
			return;
		}
			
	}

	
	@Override
	protected AuthPhoneDetails buildUserDetails(Map<String, Object> claims) {
		log.debug("*JwtAuthPhoneFilter.buildUserDetails 실행.");
		
		Object subjectRaw = claims.get("subject");
		if (subjectRaw == null) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. subject");
		}
		int verificationId = Integer.parseInt(subjectRaw.toString());
		if(verificationId==0) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - subject");
		}
		
		Object rolesObj = claims.get("roles");
		if (!(rolesObj instanceof List)) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - roles");
			
		}
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) rolesObj;
		List<GrantedAuthority> authorities =
				roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
		
		String phone = (String) claims.get("phone");
		if (phone == null || phone.isEmpty()) {
			throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - phone");
		}
		
		return new AuthPhoneDetails(verificationId, phone, authorities);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String uri = request.getRequestURI();
		return !(uri.startsWith("/api/spem/private/auth/") || uri.startsWith("/api/estimate/private/auth/"));
	}

}
