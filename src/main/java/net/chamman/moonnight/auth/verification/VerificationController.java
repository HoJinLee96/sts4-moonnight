package net.chamman.moonnight.auth.verification;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.ApiResponseFactory;

@Tag(name = "VerificationController", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verify")
public class VerificationController {
	
	private final VerificationService verificationService;
	private final RateLimiterStore rateLimiter;
	private final ApiResponseFactory apiResponseFactory;
	
	@Operation(summary = "휴대폰 문자 인증번호 검사", description = "휴대폰 문자 인증번호 검사")
	@PostMapping("/public/compare/sms/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareSmsVerification(
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@ClientSpecific("X-Verification-Id") String verificationId,
			@Valid @RequestBody VerificationPhoneRequestDto verificationPhoneRequestDto,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByIp(clientIp);
		
		String token = verificationService.compareSms(
				verificationId,
				verificationPhoneRequestDto.phone(),
				verificationPhoneRequestDto.verificationCode(),
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Phone-Token",token)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Phone-Token", token)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(5))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "이메일 인증 인증번호 검사", description = "이메일 인증 인증번호 검사")
	@PostMapping("/public/compare/email/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareEmailVerification(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ClientSpecific("X-Verification-Id") String verificationId,
			@Valid @RequestBody VerificationEmailRequestDto verificationEmailRequestDto,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByIp(clientIp);
		
		String token = verificationService.compareEmail(
				verificationId,
				verificationEmailRequestDto.email(), 
				verificationEmailRequestDto.verificationCode(), 
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Email-Token",token)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Email-Token", token)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(5))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "휴대폰 문자 인증번호 발송", description = "휴대폰 문자 인증번호 발송")
	@PostMapping("/public/sms")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyToSms(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ValidPhone @RequestParam String phone,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByPhone(phone);
		rateLimiter.isAllowedByIp(clientIp);
		
		String encodingVerificationId = verificationService.sendSmsVerificationCode(phone, clientIp)+"";
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Id",encodingVerificationId)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Id", encodingVerificationId)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(3))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "이메일 인증번호 발송", description = "이메일 인증번호 발송")
	@PostMapping("/public/email")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyToEmail(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ValidEmail @RequestParam String email,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByEmail(email);
		rateLimiter.isAllowedByIp(clientIp);
		
		String encodingVerificationId = verificationService.sendEmailVerificationCode(email, clientIp)+"";
		
		if(isMobileApp) {
			return ResponseEntity.ok(apiResponseFactory.success(SUCCESS, Map.of("X-Verification-Id",encodingVerificationId)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Verification-Id", encodingVerificationId)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(3))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(apiResponseFactory.success(SUCCESS_NO_DATA, null));
		}
	}
	
}
