package net.chamman.moonnight.global.exception;

import static net.chamman.moonnight.global.exception.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static net.chamman.moonnight.global.exception.HttpStatusCode.REQUEST_BODY_NOT_VALID;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.AutoSetMessageResponse;
import net.chamman.moonnight.global.util.ApiResponseDto;

@Slf4j
@Order(0)
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
	
	private final MessageSource messageSource;
	
	@AutoSetMessageResponse
	@ExceptionHandler(CriticalException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCriticalException(CriticalException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.error("*{} 발생. HttpStatusCode: [{}]",
				ex.getClass().getSimpleName(),
				httpStatusCode.toString(),
				ex
				);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(ApiResponseDto.of(httpStatusCode, null));
	}
	
	@AutoSetMessageResponse
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleCustomException(CustomException ex) {
		HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
		log.info("*{} 발생. HttpStatusCode: [{}]",
				ex.getClass().getSimpleName(),
				httpStatusCode.toString(),
				ex
				);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(ApiResponseDto.of(httpStatusCode, null));
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		log.info("*MethodArgumentNotValidException 발생.", ex);
		
		BindingResult bindingResult = ex.getBindingResult(); // 에러 정보가 다 여기 들어있음!
		
		String messageKey = bindingResult.getFieldError().getDefaultMessage();
		
		if (messageKey != null && !messageKey.isEmpty()) {
			Locale locale = RequestContextUtils.getLocale(((org.springframework.http.server.ServletServerHttpRequest) request).getServletRequest());
			
			try {
				
				String resolvedMessage = messageSource.getMessage(messageKey, null, locale);
				
				ApiResponseDto<Void> apiResponseDto = ApiResponseDto.of(REQUEST_BODY_NOT_VALID, null);
				apiResponseDto.setMessage(resolvedMessage);
				
				return ResponseEntity.status(400).body(apiResponseDto);
			} catch (Exception e) {
				log.warn("메시지 번역 실패. [exception: {}]",e);
				return ResponseEntity.status(400).body(ApiResponseDto.of(REQUEST_BODY_NOT_VALID, null));
			}
		} else {
			return  ResponseEntity.status(400).body(ApiResponseDto.of(REQUEST_BODY_NOT_VALID, null));
		}
	}
	
	@AutoSetMessageResponse
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Void>> handleAllExceptions(Exception e, HttpServletRequest request) {
		log.error("예상치 못한 익셉션 발생.", e);
		return ResponseEntity.status(500).body(ApiResponseDto.of(INTERNAL_SERVER_ERROR, null));
	}
	
}
