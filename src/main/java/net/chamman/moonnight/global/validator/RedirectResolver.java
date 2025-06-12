package net.chamman.moonnight.global.validator;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_REQUEST;

import java.util.Base64;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.Redirect;
import net.chamman.moonnight.global.exception.IllegalRequestException;

@Component
@Slf4j
public class RedirectResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Redirect.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		
		Redirect clientSpecificAnnotation = parameter.getParameterAnnotation(Redirect.class);
		boolean isRequired = clientSpecificAnnotation.required();

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new IllegalRequestException(ILLEGAL_REQUEST, "HttpServletRequest가 비어있음.");
		}

		String clientType = request.getHeader("X-Client-Type");
		boolean isMobileApp = clientType != null && clientType.contains("mobile");
		String clientTypeKr = isMobileApp ? "모바일 앱" : "웹";

		String redirect = "/";
		if (!isMobileApp) {
			String encodedRedirect = webRequest.getParameter("redirect");
			if(encodedRedirect!=null && !encodedRedirect.isBlank()) {
				redirect = new String(Base64.getUrlDecoder().decode(encodedRedirect));
			} else {
				if(isRequired) {
					throw new IllegalRequestException(ILLEGAL_INPUT_VALUE, "리다이렉트 파라미터가 필수이지만 값이 비어있음.");
				}
			}
		}
		log.debug("*[{} {}] 통해 접근. RedirectResolver 통한 결과 redirect: [{}]",clientTypeKr, clientType, redirect);
		return redirect;
		
	}

}
