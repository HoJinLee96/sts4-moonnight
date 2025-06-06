package net.chamman.moonnight.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.oauth.OAuth2LoginSuccessHandler;
import net.chamman.moonnight.global.security.fillter.JwtAuthPhoneFilter;
import net.chamman.moonnight.global.security.fillter.JwtFilter;
import net.chamman.moonnight.global.security.fillter.SilentAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class SecurityConfig {
	
	private final JwtFilter jwtFilter;
	private final JwtAuthPhoneFilter jwtAuthPhoneFilter;
	private final SilentAuthenticationFilter silentAuthenticationFilter;
	private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
	
	@Value("${naver-login.clientId}")
	private String naverClientId;
	@Value("${naver-login.clientSecret}")
	private String naverClientSecret;
	@Value("${naver-login.redirectUri}")
	private String naverRedirectUri;
	
	@Value("${kakao-api.restApiKey}")
	private String kakaoClientId;
	@Value("${kakao-api.clientSecret}")
	private String kakaoClientSecret;
	@Value("${kakao-login.redirectUri}")
	private String kakaoRedirectUri;
	
	// 누구나 접근 가능
	public static final String[] PUBLIC_URIS = {
			"/api/*/public/**", 
			"/", "/home",
			"/estimate", "/review",
			"/verify/**"
	};
	
	// 로그인 하면 안 되는 접근
	public static final String[] NON_SIGNIN_ONLY_URIS = {
			"/signin", "/signinBlank", "/signup*", "/signup/**",
			"/find/**", "/update/password/blank" 
	};
	
//			다른 필터에서 적용중
//			"/api/*/private/**", "/api/spem/private/auth/**", "/api/estimate/private/auth/**"
	public static final String[] SIGNIN_ONLY_URIS = {
			"/my/**","/my"
	};
	
	// 모든 접근 허용
	@Bean
	@Order(0)
	public SecurityFilterChain staticFilterChain(HttpSecurity http) throws Exception {
		http
		.securityMatcher("/css/**", "/js/**", "/images/**", "/favicon.ico",
				"/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/swagger-resources/**","/webjars/**","/openapi.yaml")
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(authz -> authz
				.anyRequest().permitAll()
				);
		
		return http.build();
	}
	
	// ROLE_AUTH 전용
	@Bean
	@Order(1)
	public SecurityFilterChain phoneAuthSecurityFilterChain(HttpSecurity http) throws Exception {
		http
		.securityMatcher("/api/spem/private/auth/**", "/api/estimate/private/auth/**")
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth
				.anyRequest().authenticated()
				)
		.addFilterBefore(jwtAuthPhoneFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	// ROLE_LOCAL or ROLE_OAUTH
	@Bean
	@Order(2)
	public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) throws Exception {
		http
		.securityMatcher("/api/*/private/**")
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth
				.anyRequest().authenticated()
				)
		.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	// OAUTH 로그인
	@Bean
	@Order(3)
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher("/oauth2/**", "/login/oauth2/**")
				.csrf(AbstractHttpConfigurer::disable)
				.oauth2Login(oauth2 -> oauth2
						.clientRegistrationRepository(clientRegistrationRepository())
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService()))
						.successHandler(oauth2LoginSuccessHandler))
				.build();
	}
	
	// 그 외 로그인 여부 확인 및 점검(리프레쉬통한 발급)
	@Bean
	@Order(4)
	public SecurityFilterChain silentFilterChain(HttpSecurity http) throws Exception {
		http
		.securityMatcher("/**")
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.exceptionHandling(exceptions -> exceptions
				// 권한 없는 페이지 접근 시 처리 (예: 로그인 후 /signin 접근 시, 없는 요청 경로 /awghjw 접근시)
				.accessDeniedHandler(accessDeniedHandler())
				// 인증되지 않은 사용자가 접근 시 처리 (예: /my/** 접근 시)
				.authenticationEntryPoint(authenticationEntryPoint())
				)
		.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(PUBLIC_URIS).permitAll()
				.requestMatchers(SIGNIN_ONLY_URIS).authenticated()
				.requestMatchers(NON_SIGNIN_ONLY_URIS).access((authz, context) -> {
					Authentication auth = authz.get();
					boolean isAnonymous = auth == null || !auth.isAuthenticated() || (auth instanceof AnonymousAuthenticationToken);
					return new AuthorizationDecision(isAnonymous);
				})
				.anyRequest().denyAll()
				)
		.addFilterBefore(silentAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	
	
	// 사용자 정보를 커스터마이징 처리
	// OAuth2UserService는 OAuth 로그인 성공 후 AccessToken으로 유저 정보를 불러와 인증 객체를 만드는 커스터마이징 포인트.
	// 위 코드는 네이버처럼 구조가 다른 JSON 응답에도 대응하기 위한 커스텀 로직이 포함된 형태.
	@SuppressWarnings("unchecked")
	@Bean
	public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
		return userRequest -> {
			OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
			OAuth2User oAuth2User = delegate.loadUser(userRequest);
			String registrationId = userRequest.getClientRegistration().getRegistrationId();
			
			if (Objects.equals("naver",registrationId)){
				Map<String, Object> attributes = oAuth2User.getAttributes();
				Map<String, Object> response = (Map<String, Object>) attributes.get("response");
				return new DefaultOAuth2User(
						Collections.singleton(new SimpleGrantedAuthority("ROLE_OAUTH")),
						response,
						"id"
						);
			} 
			return oAuth2User;
		};
	}
	
	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(
				this.naverClientRegistration(),
				this.kakaoClientRegistration()
				);
	}
	
	private ClientRegistration naverClientRegistration() {
		return ClientRegistration.withRegistrationId("naver")
				.clientId(naverClientId)
				.clientSecret(naverClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(naverRedirectUri)
				.scope("name", "email")
				.authorizationUri("https://nid.naver.com/oauth2.0/authorize")
				.tokenUri("https://nid.naver.com/oauth2.0/token")
				.userInfoUri("https://openapi.naver.com/v1/nid/me")
				.userNameAttributeName("response") // 식별값
				.clientName("Naver")
				.build();
	}
	
	private ClientRegistration kakaoClientRegistration() {
		return ClientRegistration.withRegistrationId("kakao")
				.clientId(kakaoClientId)
				.clientSecret(kakaoClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(kakaoRedirectUri)
				.scope("profile_nickname", "account_email")
				.authorizationUri("https://kauth.kakao.com/oauth/authorize")
				.tokenUri("https://kauth.kakao.com/oauth/token")
				.userInfoUri("https://kapi.kakao.com/v2/user/me")
				.userNameAttributeName("id")
				.clientName("Kakao")
				.build();
	}
	
	// AccessDeniedHandler 빈 정의: 권한 없는 사용자 처리
	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return new CustomAccessDeniedHandler();
	}
	
	public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	    @Override
	    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
	    	log.debug("AccessDeniedHandler 실행됨",accessDeniedException);

			response.sendRedirect("/home");
	    }
	}
	
	// AuthenticationEntryPoint 빈 정의: 인증 안된 사용자 리다이렉트
	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return new CustomAuthenticationEntryPoint();
	}
	
	public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
	    	log.debug("AuthenticationEntryPoint 실행됨",authException);

			String requestUri = request.getRequestURI();
			String queryString = request.getQueryString();
			String fullUrl = requestUri + (queryString != null ? "?" + queryString : "");
			// Base64 인코딩
			String encodedUrl = Base64.getEncoder().encodeToString(fullUrl.getBytes(StandardCharsets.UTF_8));
			
			if (Arrays.stream(SIGNIN_ONLY_URIS).anyMatch(requestUri::startsWith)) {
				response.sendRedirect("/signin?redirect=" + encodedUrl);
	        } else {
				response.sendRedirect("/error");
	        }
			
		}
	}

}
