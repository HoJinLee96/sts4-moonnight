package net.chamman.moonnight.auth.oauth;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthStatus;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.global.interceptor.ClientIpInterceptor;
import net.chamman.moonnight.global.util.CookieUtil;
import net.chamman.moonnight.global.util.HttpServletUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.global.validator.RedirectResolver;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final UserRepository userRepository;
	private final OAuthRepository oauthRepository;
	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;
	private final SignLogService signLogService;
    private final ClientIpInterceptor clientIpInterceptor;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final RedirectResolver redirectResolver;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException {
	
		// 1. 정보 추출
		String clientIp = clientIpInterceptor.extractClientIp(req);
		DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
		
		String provider = extractProvider(authentication).toUpperCase(); // registrationId 추출
		String email = null;
		String name = null;
		String oauthId = oAuth2User.getName();
		if(Objects.equals(provider, "NAVER")){
			email = oAuth2User.getAttribute("email").toString();
			name = oAuth2User.getAttribute("name").toString();
		}else if(Objects.equals(provider, "KAKAO")) {
			Map<String,Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
			email = kakaoAccount.get("email").toString();
			Map<String,Object> properties = oAuth2User.getAttribute("properties");
			name = properties.get("nickname").toString();
		}
		log.debug("*OAuth 인증 성공 핸들러 작동. Provider: [{}], OAuthId: [{}], Email: [{}], Name: [{}], ClientIp: [{}]",
				provider,
				LogMaskingUtil.maskId(oauthId, MaskLevel.MEDIUM),
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskName(name, MaskLevel.MEDIUM),
				clientIp
				);
		UserProvider userProvider = UserProvider.valueOf(provider);
		OAuthProvider oauthProvider = OAuthProvider.valueOf(provider);
		
		try {
			// 2. 유저 조회
			// oauth 테이블 확인
			Optional<OAuth> existingOauth = oauthRepository.findByOauthProviderAndOauthProviderId(oauthProvider, oauthId);
			
			User user;
			// 기존 유저
			if (existingOauth.isPresent()) {
				OAuth oauth = existingOauth.get();
				user = existingOauth.get().getUser();
				log.debug("* oauth.getOauthStatus(): {}",oauth.getOauthStatus());
				log.debug("* user.getUserStatus(): {}",user.getUserStatus());
				switch (user.getUserStatus()) {
					case STAY -> {
						log.debug("* 일시정지된 계정.");
						signLogService.registerSignLog (userProvider, email, clientIp, SignResult.ACCOUNT_STAY);
						redirectStrategy.sendRedirect(req, res, "/sign/stay");
						return;
					}
					case STOP -> {
						log.debug("* 중지된 계정.");
						signLogService.registerSignLog(userProvider, email, clientIp, SignResult.ACCOUNT_STOP);
						redirectStrategy.sendRedirect(req, res, "/sign/stop");
						return;
					}
					case DELETE -> {
						log.debug("* 탈퇴한 계정.");
						signLogService.registerSignLog(userProvider, email, clientIp, SignResult.ACCOUNT_DELETE);
						redirectStrategy.sendRedirect(req, res, "/sign/delete");
						return;
					}
				}
			} else {
				//신규 유저
				// user 저장
				user = User.builder()
						.email(email)
						.name(name)
						.userProvider(userProvider)
						.userStatus(UserStatus.ACTIVE)
						.marketingReceivedStatus(false)
						.build();
				userRepository.save(user);
				
				// oauth 저장
				OAuth oauth = OAuth.builder()
						.oauthProvider(oauthProvider)
						.oauthProviderId(oauthId)
						.user(user)
						.oauthStatus(OAuthStatus.ACTIVE)
						.build();
				oauthRepository.save(oauth);
			}
			
			int userId = user.getUserId();
			// JWT accessToken 생성
			String accessToken = jwtProvider.createAccessToken(userId, List.of("ROLE_OAUTH"),
					Map.of(
							"provider", provider,
							"email", email,
							"name", name
							));
			
			String refreshToken = jwtProvider.createRefreshToken(userId);
			
			tokenProvider.addRefreshJwt(user.getUserId(), refreshToken);
			
			signLogService.registerSignLog(userProvider, email, clientIp, SignResult.OAUTH_SUCCESS);
			
			// 3. 클라이언트별 반환
			String userAgent = req.getHeader("X-Client-Type");
			boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
			
			if (isMobileApp) {
				// 앱용 응답: JSON body로 토큰 전달
				res.setContentType("application/json");
				res.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
						"accessToken", accessToken,
						"refreshToken", refreshToken
						)));
			} else {
				HttpServletUtil.resSetCookie(res, "X-Access-Token", accessToken, Duration.ofMinutes(120));
				HttpServletUtil.resSetCookie(res, "X-Refresh-Token", refreshToken, Duration.ofDays(14));
				
				String redirect = getRedirect(req, res);

				redirectStrategy.sendRedirect(req, res, redirect);
			}
		}catch(Exception e) {
			log.error("OAuth 로그인 중 예외 발생.", e); 
			signLogService.registerSignLog(userProvider, email, clientIp, SignResult.OAUTH_FAIL, e.getClass().getSimpleName() + e.getMessage());
			redirectStrategy.sendRedirect(req, res, "/error");
		}
	}
	
	private String extractProvider(Authentication auth) {
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
		return token.getAuthorizedClientRegistrationId(); // "naver" or "kakao"
	}
	
	private String getRedirect(HttpServletRequest req, HttpServletResponse res) {
	       // 1. 쿠키에서 리다이렉트 URL 꺼내기
	    Optional<String> redirectUri = CookieUtil.getCookie(req, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
	            .map(Cookie::getValue);

        // 2. 사용한 쿠키 삭제
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(req, res);
        
        if (redirectUri.isPresent() && !redirectUri.get().isBlank()) {
        	String decodedUrl = new String(Base64.getUrlDecoder().decode(redirectUri.get()));
        	// 3. 리다이렉트 URL가 내 도메인인지 검증
            if (redirectResolver.isAuthorizedRedirectUri(decodedUrl)) {
                log.debug("* OAUTH로그인 성공 이후 Redirect 값: [{}]", decodedUrl);
                return decodedUrl;
            } else {
                log.info("* 허용되지 않은 Redirect URI 감지: [{}]", decodedUrl);
                return "/"; 
            }
        }
        return "/";
	}
	


}
