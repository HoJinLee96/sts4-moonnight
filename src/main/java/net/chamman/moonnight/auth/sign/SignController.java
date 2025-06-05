package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.user.UserCreateRequestDto;
import net.chamman.moonnight.global.annotation.AutoSetMessageResponse;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.exception.IllegalValueException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Tag(name = "SignController", description = "로그인, 회원가입, 로그아웃 API")
@RestController
@RequestMapping("/api/sign")
@Slf4j
@RequiredArgsConstructor
public class SignController {
	
	private final SignService signService;
	
	@Operation(summary = "LOCAL 유저 로그인", description = "LOCAL 유저 로그인")
	@AutoSetMessageResponse
	@PostMapping("/public/in/local")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> signInLocal(
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@Valid @RequestBody SignInRequestDto signInRequestDto,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		log.debug("LOCAL 로그인 요청. Email: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskEmail(signInRequestDto.email(), MaskLevel.MEDIUM),
				clientIp,
				isMobileApp?"mobile":"web");
		
		Map<String,String> signJwt = signService.signInLocal(signInRequestDto, clientIp);
		
		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.of(SUCCESS, signJwt));
		} else {
			String referer = request.getHeader("Referer");
			if (referer == null || !referer.startsWith(request.getScheme() + "://" + request.getServerName())) {
				referer = "/home";
			}
			
			ResponseCookie accessCookie = ResponseCookie.from("X-Access-Token", signJwt.get("accessToken"))
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(120))
					.sameSite("Lax")
					.build();
			
			ResponseCookie refreshCookie = ResponseCookie.from("X-Refresh-Token", signJwt.get("refreshToken"))
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofDays(14))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK)
					.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
					.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
					.body(ApiResponseDto.of(SUCCESS, Map.of("redirect",referer)));
		}
	}
	
	@Operation(summary = "AUTH 로그인", description = "휴대폰 인증 통해 로그인 토큰 발급")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@AutoSetMessageResponse
	@PostMapping("/public/in/auth/sms")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> signInAuthSms(
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@ValidPhone @RequestParam String phone,
			@RequestParam String name,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		log.debug("AUTH SMS 로그인 요청. Phone: [{}], Name: [{}], VerificationToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskPhone(phone, MaskLevel.MEDIUM),
				LogMaskingUtil.maskName(name, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM),
				clientIp,
				isMobileApp?"mobile":"web");

		String authPhoneToken = signService.signInAuthSms(verificationPhoneToken, phone, name, clientIp);
		
		if (isMobileApp) {
			return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.of(SUCCESS, Map.of("X-Auth-Phone-Token",authPhoneToken)));
		} else {
			ResponseCookie authPhoneTokenCookie = ResponseCookie.from("X-Auth-Phone-Token", authPhoneToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(30))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK)
					.header(HttpHeaders.SET_COOKIE, authPhoneTokenCookie.toString())
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "로그아웃", description = "로그아웃")
	@SecurityRequirement(name = "X-Access-Token")
	@SecurityRequirement(name = "X-Refresh-Token")
	@AutoSetMessageResponse
	@PreAuthorize("hasRole('LOCAL') or hasRole('OAUTH')")
	@PostMapping("/public/out/local")
	public ResponseEntity<ApiResponseDto<Void>> signOut(
			@AuthenticationPrincipal CustomUserDetails user,
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@ClientSpecific(value = "X-Access-Token") String accessToken,
			@ClientSpecific(value = "X-Refresh-Token") String refreshToken,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("MyMobileApp");
		log.debug("LOCAL 로그아웃 요청. User ID: [{}], AccessToken: [{}], Client IP: [{}], User-Agent: [{}]",
				user != null ? user.getUserId() : "anonymous",
				LogMaskingUtil.maskToken(accessToken, MaskLevel.MEDIUM),
				clientIp,
				isMobileApp?"mobile":"web"
				);
		
		
		signService.signOut(accessToken, refreshToken, clientIp);
		
		if(!isMobileApp) {
			ResponseCookie accessCookie = ResponseCookie.from("X-Access-Token", "")
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(0)
					.sameSite("Lax")
					.build();
			ResponseCookie refreshCookie = ResponseCookie.from("X-Refresh-Token", "")
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(0)
					.sameSite("Lax")
					.build();
			
			response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
			response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
		}
		return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, null));
	}
	
	@Operation(summary = "회원가입 1차", description = "이메일 인증, 비밀번호 입력")
	@SecurityRequirement(name = "X-Verification-Email-Token")

	@AutoSetMessageResponse
	@PostMapping("/public/up/first")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> signup1(
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@ClientSpecific("X-Verification-Email-Token") String verificationEmailToken,
			@ValidEmail @RequestParam String email,
			@ValidPassword @RequestParam String password,
			@ValidPassword @RequestParam String confirmPassword,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("MyMobileApp");
		log.debug("LOCAL 회원가입 1차 요청. Email: [{}], VerificationEmailToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationEmailToken, MaskLevel.MEDIUM),
				clientIp,
				isMobileApp?"mobile":"web"
				);
		
		if(!Objects.equals(password, confirmPassword)) {
			throw new IllegalValueException(ILLEGAL_INPUT_VALUE,"두 비밀번호들가 일치하지 않음. password: "+password+", confirmPassword: "+confirmPassword);
		}
		
		String accessSignUpToken = signService.createSignUpToken(email, password, verificationEmailToken);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("X-Access-SignUp-Token",accessSignUpToken)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Access-SignUp-Token", accessSignUpToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(20))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "회원가입 2차", description = "휴대폰 문자 인증, 개인정보 입력")
	@SecurityRequirement(name = "X-Access-SignUp-Token")
	@SecurityRequirement(name = "X-Verification-Phone-Token")
	@AutoSetMessageResponse
	@PostMapping("/public/up/second")
	public ResponseEntity<ApiResponseDto<String>> signup2(
			@RequestHeader(required = false, value = "X-Client-Type") String userAgent,
			@ClientSpecific("X-Access-SignUp-Token") String accessSignUpToken,
			@ClientSpecific("X-Verification-Phone-Token") String verificationPhoneToken,
			@Valid @RequestBody UserCreateRequestDto userCreateRequestDto,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("MyMobileApp");
		log.debug("LOCAL 회원가입 2차 요청. AccessSignUpToken: [{}], VerificationPhoneToken: [{}], Client IP: [{}], User-Agent: [{}]",
				LogMaskingUtil.maskToken(accessSignUpToken, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(verificationPhoneToken, MaskLevel.MEDIUM),
				clientIp,
				isMobileApp?"mobile":"web"
				);
		
		String name = signService.signUpLocalUser(userCreateRequestDto, accessSignUpToken, verificationPhoneToken);
		
		
		return ResponseEntity.ok(ApiResponseDto.of(CREATE_SUCCESS, name));
	}

}
