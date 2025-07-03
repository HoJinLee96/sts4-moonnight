package net.chamman.moonnight.global.security.fillter;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Component
@Slf4j
public class JwtFilter extends AbstractAccessTokenFilter<CustomUserDetails> {
	
	@Override
	protected CustomUserDetails buildUserDetails(Map<String, Object> claims) {
		log.debug("* JwtFilter.buildUserDetails 실행.");
		
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
		AntPathMatcher matcher = new AntPathMatcher();
		String PRIVATE_PATTERN = "/api/**/private/**";
		Set<String> SKIP_PATHS = Set.of(
		        "/api/spem/private/auth",
		        "/api/estimate/private/auth",
		        "/api/spem/private/auth/**",
		        "/api/estimate/private/auth/**"
		);
		
	    String uri = request.getRequestURI();
	    boolean skipResult = false;
	    for (String pattern : SKIP_PATHS) {
	        if (matcher.match(pattern, uri)) {
	        	skipResult = true; 
	        }
	    }
	    // 1) /api/**/private/** 패턴과 일치하고
	    // 2) 예외 리스트에 없으면 → 필터 적용
	    boolean mustFilter = matcher.match(PRIVATE_PATTERN, uri) && !skipResult;

	    // shouldNotFilter가 true면 **필터를 건너뜀**이므로
	    // mustFilter를 뒤집어서 반환
	    return !mustFilter;
	}
	
}
