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
import net.chamman.moonnight.global.exception.jwt.CustomJwtException;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.security.principal.AuthUserDetails;

@Component
public class JwtAuthPhoneFilter extends AbstractAccessTokenFilter<AuthUserDetails> {
  
  @Override
  protected AuthUserDetails buildUserDetails(Map<String, Object> claims) {
    System.out.println("==========JwtAuthPhoneFilter.buildUserDetails===========");

    try {
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
    	
    	String name = (String) claims.get("name");
    	if (name == null || name.isEmpty()) {
    		throw new IllegalJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - name");
    	}
    	
    	return new AuthUserDetails(verificationId, phone, name, authorities);
    } catch (Exception e) {
    	throw new CustomJwtException(JWT_ILLEGAL,"JWT AuthUserDetails 생성중 오류 발생. - 알수없음.",e);
    }
  }
  
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
      String uri = request.getRequestURI();
      return !(uri.startsWith("/api/spem/private/auth/") || uri.startsWith("/api/estimate/private/auth/"));
  }

}
