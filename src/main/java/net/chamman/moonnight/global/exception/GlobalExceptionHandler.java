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
		log.error("{} 발생.\n[httpStatusCode: {}]\n[exception: {}]",
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
		log.info("{} 발생.\n[httpStatusCode: {}]\n[exception: {}]",
				ex.getClass().getSimpleName(),
				httpStatusCode.toString(),
				ex
				);
		return ResponseEntity.status(httpStatusCode.getStatus()).body(ApiResponseDto.of(httpStatusCode, null));
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Void>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		
		log.info("MethodArgumentNotValidException 발생.\n [exception: {}]", ex);
		
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
		log.error("예상치 못한 익셉션 발생. [exception: {}]", e);
		return ResponseEntity.status(500).body(ApiResponseDto.of(INTERNAL_SERVER_ERROR, null));
	}
	
//  @ExceptionHandler(DuplicationException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleDuplicateKeyException(DuplicationException ex) {
//    HttpStatusCode httpStatusCode = ex.getHttpStatusCode();
//    log.info("{} 발생. [httpStatusCode: {}]",
//        this.getClass().getSimpleName(),
//        httpStatusCode.toString());
//    return ResponseEntity.status(httpStatusCode.getStatus()).body(ApiResponseUtil.of(httpStatusCode, null));
//  }
	
	// (입력받은 값) 기본적인 데이터 유효성 부적합, 비즈니스 로직 기반 검증 부적합
//  @ExceptionHandler(IllegalArgumentException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
//    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseUtil.of(HttpStatus.BAD_REQUEST.value(), "다시 확인해 주세요.",null));
//  }
//
//  // 비정상적인 상태 (정상이여야 하는 상황)
//  @ExceptionHandler(IllegalStateException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
//    log.error("서버 내부 상태 오류 발생: {}", ex.getMessage(), ex); 
//    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseUtil.of(
//        HttpStatus.INTERNAL_SERVER_ERROR.value(),
//        "죄송합니다 현재 서버에서 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요.",
//        null));
//  }
//
//  // 권한 이상
//  @ExceptionHandler(AccessDeniedException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
//    log.info("권한 부족: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseUtil.of(HttpStatus.FORBIDDEN.value(), "적합하지 않은 권한입니다.",null));
//  }
//
//  // 로그인 실패, 인증번호 불일치 또는 지난 인증요청 확인 결과 불, 정지된 SNS 로그인 계정
//  @ExceptionHandler(BadCredentialsException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
//    log.info("인증 실패: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponseUtil.of(HttpStatus.UNAUTHORIZED.value(), "유효하지 않습니다.", null));
//  }
//
//  @ExceptionHandler(NoSuchElementException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleNoSuchElementException(RuntimeException ex, WebRequest request) {
//    log.info("데이터 조회 실패: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseUtil.of(HttpStatus.NOT_FOUND.value(), "다시 확인해 주세요.", null));
//  }
//  
//  @ExceptionHandler(NotFoundException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleNotFoundException(RuntimeException ex, WebRequest request) {
//    log.info("데이터 조회 실패: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseUtil.of(HttpStatus.NOT_FOUND.value(), "다시 확인해 주세요.", null));
//  }
//
//  // 요청 횟수 초과 시
//  @ExceptionHandler(TooManyRequestsException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handle(TooManyRequestsException ex, WebRequest request) {
//    log.warn("요청 횟수 초과: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//        .body(ApiResponseUtil.of(HttpStatus.TOO_MANY_REQUESTS.value(), "요청 횟수 초과 입니다.\n잠시후 시도해 주세요.",null));
//  }
//
//  // 인증 시간 초과 시
//  @ExceptionHandler(VerificationTimeoutException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleTimeoutException(VerificationTimeoutException ex, WebRequest request) {
//    log.info("시간 초과: {}", ex.getMessage()); 
//    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(ApiResponseUtil.of(HttpStatus.REQUEST_TIMEOUT.value(), "시간 초과 입니다.\n다시 시도해 주세요.", null));
//  }
//
//  // 입출력 예외 발생 시
//  @ExceptionHandler(IOException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleIOException(IOException ex, WebRequest request) {
//    log.error("입출력 오류 발생: {}", ex.getMessage(), ex); 
//    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//        .body(ApiResponseUtil.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "죄송합니다 현재 서버에서 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요.", null));
//  }
//  
//  @ExceptionHandler(MaxUploadSizeExceededException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
//    log.info("입출력 오류 발생: {}", ex.getMessage(), ex); 
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//              .body(ApiResponseUtil.of(HttpStatus.BAD_REQUEST.value(),"업로드 가능한 최대 파일 크기를 초과했습니다.", null));
//  }
//  
//  @ExceptionHandler(ExpiredJwtException.class)
//  public ResponseEntity<ApiResponseUtil<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
//    log.info("입출력 오류 발생: {}", ex.getMessage(), ex); 
//    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//        .body(ApiResponseUtil.of(HttpStatus.UNAUTHORIZED.value(),"로그인 시간 만료.", null));
//  }
	
}
