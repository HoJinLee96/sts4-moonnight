package net.chamman.moonnight.global.interceptor;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.support.RequestContextUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.AutoSetMessageResponse;
import net.chamman.moonnight.global.exception.HttpStatusCode;
import net.chamman.moonnight.global.util.ApiResponseDto;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CommonResponseAdvice implements ResponseBodyAdvice<ApiResponseDto<?>>{
	
	private final MessageSource messageSource;
	
	@Override
	public boolean supports(MethodParameter returnType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		
		boolean hasAutoWrapAnnotation = returnType.hasMethodAnnotation(AutoSetMessageResponse.class);
		
		// 최종 결정: @AutoWrapResponse 어노테이션이 붙은 메서드만 대상으로 하겠다!
		return hasAutoWrapAnnotation;
		
		// URI 체크 방식은 이제 필요 없어짐!
		// String requestURI = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRequestURI();
		// return requestURI.startsWith("/api/*/private/**"); // <-- 이제 이렇게 안 해도 됨!
	}
	
	@Override
	public ApiResponseDto<?> beforeBodyWrite(ApiResponseDto<?> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
					ServerHttpRequest request, ServerHttpResponse response) {
		
		if (body != null) {
			HttpStatusCode statusCode = body.getHttpStatusCode();
			
			if (statusCode != null) {
				String messageKey = statusCode.getMessageKey();
				
				if (messageKey != null && !messageKey.isEmpty()) {
					Locale locale = RequestContextUtils.getLocale(((org.springframework.http.server.ServletServerHttpRequest) request).getServletRequest());
					
					// MessageSource를 이용해서 메시지 키와 Locale로 실제 메시지를 조회
					try {
						// messageSource.getMessage(메시지키, 메시지 인자 배열, Locale)
						String resolvedMessage = messageSource.getMessage(messageKey, null, locale);
						
						body.setMessage(resolvedMessage);
						
						return body; // 수정된 body 객체 반환
						
					} catch (Exception e) {
						log.warn("메시지 번역 실패. [exception: {}]",e);
						body.setMessage(""); // 에러 정보 포함
						return body; // 수정된 body 객체 반환
					}
				} else {
					body.setMessage(""); // 에러 정보 포함
					return body; // 수정된 body 객체 반환
				}
			} else {
				return body;
			}
		}
		return body;
	}
	
}
