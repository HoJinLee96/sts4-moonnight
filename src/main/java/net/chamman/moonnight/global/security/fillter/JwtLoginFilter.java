package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Component
public class JwtLoginFilter extends AbstractAccessTokenFilter<CustomUserDetails> {
	
	@Override
	protected CustomUserDetails buildUserDetails(Map<String, Object> claims) {
		System.out.println("==========JwtLoginFilter.buildUserDetails===========");
		
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
		// API 경로 패턴
		boolean isApiPrivateRoute = uri.startsWith("/api/") && uri.contains("/private/");
		return !(isApiPrivateRoute);
	}
	
}
