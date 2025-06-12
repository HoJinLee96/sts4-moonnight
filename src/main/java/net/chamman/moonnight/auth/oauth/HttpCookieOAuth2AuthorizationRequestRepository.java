package net.chamman.moonnight.auth.oauth;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.util.CookieUtil;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    /**
     * 콜백 요청 시, 저장된 인증 요청 정보를 쿠키에서 다시 꺼내오는 역할.
     * 이 정보가 있어야 state 값의 유효성을 검증할 수 있음.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("*loadAuthorizationRequest: Trying to load authorization request from cookie.");
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    OAuth2AuthorizationRequest authRequest = CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
                    log.debug("*Successfully loaded authorization request from cookie: {}", authRequest);
                    return authRequest;
                })
                .orElse(null);
    }

    /**
     * 로그인 요청 시, 인증 요청 정보(state 포함)와 redirect 파라미터를 쿠키에 저장하는 역할.
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        log.debug("*saveAuthorizationRequest: Saving authorization request to cookie.");
        // OAuth2AuthorizationRequest 객체 전체를 쿠키에 저장
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        // redirect 파라미터도 별도의 쿠키에 저장
        String redirectUri = request.getParameter("redirect");
        if (StringUtils.isNotBlank(redirectUri)) {
            log.debug("*saveAuthorizationRequest: Saving redirect_uri to cookie: {}", redirectUri);
            CookieUtil.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUri, COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        // 이 메소드는 성공/실패 핸들러에서 명시적으로 호출될 때 쿠키를 지우는 용도로 사용할 수 있음
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        log.debug("*Removing authorization request cookies.");
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}