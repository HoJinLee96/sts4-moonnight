package net.chamman.moonnight.global.security.fillter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;

@Slf4j
public abstract class AbstractAccessTokenFilter <T extends UserDetails> extends OncePerRequestFilter{
  protected abstract T buildUserDetails(Map<String, Object> claims);
  
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
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
      FilterChain filterChain) throws ServletException, IOException {
    
    System.out.println("==========AbstractAccessTokenFilter===========");
    
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
    	setErrorResponse(res, 4011, "유효하지 않은 요청 입니다.");
    	filterChain.doFilter(req, res);
    	return;
    }

    // 2. 블랙리스트 확인
    if(!isBlackList(accessToken, clientIp)) {
    	initTokenToCookie(res);
    	setErrorResponse(res, 4012, "유효하지 않은 요청 입니다.");
    	filterChain.doFilter(req, res);
    	return;
    }
    
    try {
    	// 3. 토큰 확인
        setAuthentication(accessToken);

    	filterChain.doFilter(req, res);
    	
    	// 4. Access Token 만료.
    } catch (ExpiredJwtException e) {
        try {
        	// 5. 리프레쉬 토큰 통해 SignIn Tokens 재발급
            Map<String, String> newTokens = signService.refresh(accessToken, refreshToken, clientIp);
            
            // 6. 새로운 토큰 Set
            setTokenToResponse(newTokens, res, isMobileApp);

            setAuthentication(newTokens.get(newTokens.get("accessToken")));

            filterChain.doFilter(req, res);
        } catch (Exception refreshEx) {
            // 리프레쉬 토큰 통해 SignIn Tokens 재발급 실패
        	initTokenToCookie(res);
        	setErrorResponse(res, 4010, "유효하지 않은 요청 입니다.");
        	filterChain.doFilter(req, res);
            return;
        }
    } catch (Exception e) {
    	initTokenToCookie(res);
    	setErrorResponse(res, 4010, "유효하지 않은 요청 입니다.");
    	filterChain.doFilter(req, res);
    	return;
    }
  }
  
  // 토큰 null 체크
  protected boolean validateTokens(String[] tokens) {
    for(String token : tokens) {
      if (token == null || token.isBlank()) {
        return false;
      }
    }
    return true;
  }
  
  // 블랙 리스트 확인
  protected boolean isBlackList(String accessToken, String clientIp) throws IOException {
    if (tokenStore.isBlackList(accessToken)) {
      log.warn("[블랙리스트 토큰 접근] IP: {}, token: {}", clientIp, accessToken);
      
      // 로그인 로그 남기기
      signLogService.registerSignLog(
          UserProvider.LOCAL, null, clientIp, SignResult.BLACKLISTED_TOKEN);
      
      // 관리자 알림 전송
      try {
          guidanceService.sendSecurityAlert("블랙리스트 토큰 접근 시도\nIP: " + clientIp + "\naccessToken: " + accessToken);
      } catch (Exception e) {
          log.warn("보안 알림 전송 실패: {}", e.getMessage());
      }
      return false;
    }
    return true;
  }
  
  
  protected void setTokenToResponse(Map<String, String> tokens, HttpServletResponse res, boolean isMobileApp) {
    String accessToken = tokens.get("accessToken");
    String refreshToken = tokens.get("refreshToken");
        
    if(isMobileApp) {
      res.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
      res.setHeader("X-Refresh-Token", refreshToken);
    }else {
    ResponseCookie accessCookie = ResponseCookie.from("X-Access-Token", accessToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMinutes(30))
        .sameSite("Lax")
        .build();
    ResponseCookie refreshCookie = ResponseCookie.from("X-Refresh-Token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofDays(14))
        .sameSite("Lax")
        .build();
    
    res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
  }
    
  protected void initTokenToCookie(HttpServletResponse res) {
    ResponseCookie accessCookie = ResponseCookie.from("X-Access-Token", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("Lax")
        .build();
    ResponseCookie refreshCookie = ResponseCookie.from("X-Refresh-Token", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("Lax")
        .build();

    res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

  protected String getAccessToken(boolean isMobileApp, HttpServletRequest req) {
    if(isMobileApp) {
      return req.getHeader("X-Access-Token");
    }else {
      Cookie cookie = WebUtils.getCookie(req, "X-Access-Token");
      if(cookie != null) {
        return cookie.getValue();
      }
    }
    return null;
  }
  
  protected String getRefreshToken(boolean isMobileApp, HttpServletRequest req) {
    if(isMobileApp) {
      return req.getHeader("X-Refresh-Token");
    }else {
      Cookie cookie = WebUtils.getCookie(req, "X-Refresh-Token");
      if(cookie != null) {
        return cookie.getValue();
      }
    }
    return null;
  }
  
  protected void setErrorResponse(HttpServletResponse res, int code, String message) throws IOException {
    res.setStatus(HttpStatus.UNAUTHORIZED.value());
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    
    Map<String, Object> body = Map.of(
        "code", code,
        "message", message
    );
    res.getWriter().write(new ObjectMapper().writeValueAsString(body));
  }  
  
  protected void setAuthentication(String accessToken) {
	    Map<String,Object> claims = jwtTokenProvider.validateAccessToken(accessToken);
	    T userDetails = buildUserDetails(claims);
	    UsernamePasswordAuthenticationToken authentication =
	        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	    SecurityContextHolder.getContext().setAuthentication(authentication);
	  }
  
}