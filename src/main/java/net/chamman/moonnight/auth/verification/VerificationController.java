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
import net.chamman.moonnight.global.annotation.AutoSetMessageResponse;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.util.ApiResponseDto;

@Tag(name = "VerificationController", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verify")
public class VerificationController {
	
	private final VerificationService verificationService;
	private final RateLimiterStore rateLimiter;
	
	@Operation(summary = "휴대폰 문자 인증", description = "휴대폰 문자 인증")
	@AutoSetMessageResponse
	@PostMapping("/public/compare/sms/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareSmsVerification(
			@Valid @RequestBody VerificationPhoneRequestDto verificationPhoneRequestDto,
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByIp(clientIp);
		
		String token = verificationService.compareSms(
				verificationPhoneRequestDto.phone(), 
				verificationPhoneRequestDto.verificationCode(), 
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("X-Verification-Phone-Token",token)));
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
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "이메일 인증", description = "이메일 인증")
	@AutoSetMessageResponse
	@PostMapping("/public/compare/email/uuid")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> compareEmailVerification(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@Valid @RequestBody VerificationEmailRequestDto verificationEmailRequestDto,
			HttpServletRequest request) {
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByIp(clientIp);
		
		String token = verificationService.compareEmail(
				verificationEmailRequestDto.email(), 
				verificationEmailRequestDto.verificationCode(), 
				clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("X-Verification-Email-Token",token)));
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
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
		
	}
	
	@Operation(summary = "휴대폰 문자 인증번호 발송", description = "휴대폰 문자 인증번호 발송")
	@AutoSetMessageResponse
	@PostMapping("/public/sms")
	public ResponseEntity<ApiResponseDto<Void>> verifyToSms(
			@ValidPhone @RequestParam String phone,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByPhone(phone);
		rateLimiter.isAllowedByIp(clientIp);
		
		verificationService.sendSmsVerificationCode(phone, clientIp);
		
		return ResponseEntity.ok(ApiResponseDto.of(SUCCESS_NO_DATA,null));
	}
	
	@Operation(summary = "이메일 인증번호 발송", description = "이메일 인증번호 발송")
	@AutoSetMessageResponse
	@PostMapping("/public/email")
	public ResponseEntity<ApiResponseDto<Void>> verifyToEmail(
			@ValidEmail @RequestParam String email,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByEmail(email);
		rateLimiter.isAllowedByIp(clientIp);
		
		verificationService.sendEmailVerificationCode(email, clientIp);
		
		return ResponseEntity.ok(ApiResponseDto.of(SUCCESS_NO_DATA, null));
	}
	
}
