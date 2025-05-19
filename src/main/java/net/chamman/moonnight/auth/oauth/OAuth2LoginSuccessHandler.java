package net.chamman.moonnight.auth.oauth;

import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthStatus;
import net.chamman.moonnight.auth.sign.log.SignLog;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogRepository;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuthRepository oauthRepository;
    private final JwtProvider jwtTokenProvider;
    private final SignLogRepository loginLogRepository;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy(); // 추가

    private final RedisTemplate<String, String> redisTemplate;


    @SuppressWarnings("incomplete-switch")
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
      try {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        
        String provider = extractProvider(authentication).toUpperCase(); // registrationId 추출
        String oauthId = oAuth2User.getAttribute("id").toString();
        String email = null;
        String name = null;
        if(Objects.equals(provider, "NAVER")){
          email = oAuth2User.getAttribute("email").toString();
          name = oAuth2User.getAttribute("name").toString();
        }else if(Objects.equals(provider, "KAKAO")) {
          Map<String,Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
          email = kakaoAccount.get("email").toString();
          Map<String,Object> properties = oAuth2User.getAttribute("properties");
          name = properties.get("nickname").toString();
        }

        String clientIp = getClientIp(request);

        // 기존 oauth id로 조회
        Optional<OAuth> existingOauth = oauthRepository.findByOauthProviderAndOauthProviderId(OAuthProvider.valueOf(provider), oauthId);

        User user;
        if (existingOauth.isPresent()) {
            user = existingOauth.get().getUser();
            switch (user.getUserStatus()) {
              case STAY -> {
                  saveSignLog(email, UserProvider.LOCAL, clientIp, SignResult.ACCOUNT_LOCKED);
                  throw new StatusStayException(USER_STATUS_STAY,"인증이 필요한 계정입니다.");
              }
              case STOP -> {
                  saveSignLog(email, UserProvider.LOCAL, clientIp, SignResult.ACCOUNT_SUSPENDED);
                  throw new StatusStopException(USER_STATUS_STOP,"정지된 계정입니다. 고객센터에 문의해주세요.");
              }
              case DELETE -> {
                  saveSignLog(email, UserProvider.LOCAL, clientIp, SignResult.ACCOUNT_DELETED);
                  throw new StatusDeleteException(USER_STATUS_DELETE,"탈퇴한 계정입니다.");
              }
            }
        } else {
            // user 저장
            user = User.builder()
                    .email(email)
                    .name(name)
                    .userProvider(UserProvider.valueOf(provider))
                    .userStatus(UserStatus.ACTIVE)
                    .marketingReceivedStatus(false)
                    .build();
            userRepository.save(user);

            // oauth 저장
            OAuth oauth = OAuth.builder()
                    .oauthProvider(OAuthProvider.valueOf(provider))
                    .oauthProviderId(oauthId)
                    .user(user)
                    .oauthStatus(OAuthStatus.ACTIVE)
                    .build();
            oauthRepository.save(oauth);
        }
        
        int userId = user.getUserId();
        // JWT accessToken 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId, List.of("ROLE_OAUTH"),
            Map.of(
                "provider", provider,
                "email", email,
                "name", name
                ));
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        
        redisTemplate.opsForValue().set("jwt:refresh:" + user.getUserId(), refreshToken, Duration.ofDays(14));

        saveSignLog(email, UserProvider.valueOf(provider), clientIp, SignResult.SUCCESS);
        
        // User-Agent 분기
        String userAgent = request.getHeader("X-Client-Type");
        boolean isMobileApp = userAgent != null && userAgent.contains("mobile");

        if (isMobileApp) {
            // 앱용 응답: JSON body로 토큰 전달
            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
            )));
        } else {
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
          response.addHeader("Set-Cookie", accessCookie.toString());
          response.addHeader("Set-Cookie", refreshCookie.toString());
          
          System.out.println("OAuth2LoginSuccessHandler에서 성공적으로 토큰 쿠키에 저장.");
          redirectStrategy.sendRedirect(request, response, "/home");
        }
      }catch(Exception e) {
        log.info("OAuth 로그인 처리 중 예외 발생: {}", e.getMessage(), e); 
      }
    }

    private String extractProvider(Authentication auth) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        return token.getAuthorizedClientRegistrationId(); // "naver" or "kakao"
    }
    
    private void saveSignLog(String email, UserProvider userProvider, String ip, SignResult signResult) {
      loginLogRepository.save(SignLog.builder()
          .userProvider(userProvider)
          .email(email)
          .requestIp(ip)
          .signResult(signResult)
          .build());
    }
    
    private String getClientIp(HttpServletRequest request) {
      String ip = request.getHeader("X-Forwarded-For");
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
          return ip.split(",")[0]; // 여러 개일 경우 첫 번째 IP
      }

      ip = request.getHeader("Proxy-Client-IP");
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
          return ip;
      }

      ip = request.getHeader("WL-Proxy-Client-IP");
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
          return ip;
      }

      return request.getRemoteAddr(); // fallback
  }
    
}