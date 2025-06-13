package net.chamman.moonnight.domain.user;

import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ILLEGAL_INPUT_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_NOT_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SUCCESS_NO_DATA;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.annotation.AutoSetMessageResponse;
import net.chamman.moonnight.global.annotation.ClientSpecific;
import net.chamman.moonnight.global.annotation.ValidEmail;
import net.chamman.moonnight.global.annotation.ValidPassword;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.exception.IllegalRequestException;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;


@Tag(name = "UserInfoController", description = "유저 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/local/user")
public class UserInfoController {
	
	private final UserService userService;
	
	@Operation(summary = "LOCAL 유저 이메일 찾기", description = "휴대폰 인증 토큰 통해 이메일을 찾는다.")
	@AutoSetMessageResponse
	@SecurityRequirement(name = "VerificationPhoneToken")
	@Parameters(@Parameter(ref = "Phone"))
	@ApiResponses({ 
		@ApiResponse(responseCode = "200", ref = "SuccessEmailResponse"),
		@ApiResponse(responseCode = "404", ref = "BadRequestTokenMissing")
	})
	@PostMapping("/public/find/email/by/phone")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyPhoneAndGetEmail(
			@Parameter(hidden = true) @ClientSpecific("X-Verification-Phone-Token") String token,
			@Parameter(hidden = true) @ValidPhone @RequestParam String phone){
		
		User user = userService.getUserByVerifyPhone(UserProvider.LOCAL,phone,token);
		
		return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("email",user.getEmail())));
	}
	
	@Operation(summary = "LOCAL 유저 비밀번호 찾기 1단계", description = "이메일 입력 통해 해당 이메일이 존재하는지 확인한다.")
	@AutoSetMessageResponse
	@PostMapping("/public/find/pw")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> findPassword(
			@ValidEmail @RequestParam String email){
		
		User user = userService.getUserByUserProviderAndEmail(UserProvider.LOCAL,email);
		
		return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("email",user.getEmail())));
	}
	
	@Operation(summary = "LOCAL 유저 비밀번호 찾기 2단계", description = "휴대폰 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@AutoSetMessageResponse
	@PostMapping("/public/find/pw/by/phone")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> verifyPhoneAndCreateFindPwToken(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ClientSpecific("X-Verification-Phone-Token") String token,
			@ValidEmail @RequestParam String email,
			@ValidPhone @RequestParam String phone){
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String findPwToken = userService.createFindPwTokenByVerifyPhone(UserProvider.LOCAL,email,phone,token);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("X-Access-FindPw-Token",findPwToken)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Access-FindPw-Token", findPwToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(10))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}    
	}
	
	@Operation(summary = "LOCAL 유저 비밀번호 찾기 2단계", description = "이메일 인증 토큰 통해 비밀번호 변경할 자격이 있는지 검증 이 후 Access-FindPw-Token 토큰 발급.")
	@AutoSetMessageResponse
	@PostMapping("/public/find/pw/by/email")
	public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyEmailAndCreateFindPwToken(
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ClientSpecific("X-Verification-Email-Token") String token,
			@ValidEmail @RequestParam String email){
		
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String findPwToken = userService.createFindPwTokenByVerifyEmail(UserProvider.LOCAL,email,token);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(SUCCESS, Map.of("X-Access-FindPw-Token",findPwToken)));
		}else {
			ResponseCookie cookie = ResponseCookie.from("X-Access-FindPw-Token", findPwToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(10))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "LOCAL 유저 비밀번호 찾기 3단계", description = "2단계에서 발급 받은 토큰과 새로운 비밀번호 비밀번호를 검증하여 업데이트.")
	@AutoSetMessageResponse
	@PatchMapping("/public/update/pw")
	public ResponseEntity<ApiResponseDto<Void>> updatePasswordByFindPwToken(
			@ClientSpecific("X-Access-FindPw-Token") String accessFindPwToken,
			@ValidPassword @RequestParam String password,
			@ValidPassword @RequestParam String confirmPassword,
			HttpServletRequest request){
		
		if(!Objects.equals(password, confirmPassword)) {
			throw new IllegalRequestException(ILLEGAL_INPUT_VALUE,"새로운 두 비밀번호가 일치하지 않음. password: "+password+", confirmPassword: "+confirmPassword);
		}
		
		String clientIp = (String) request.getAttribute("clientIp");
		
		userService.updatePasswordByFindPwToken(accessFindPwToken, UserProvider.LOCAL, password, clientIp);
		
		return ResponseEntity.ok(ApiResponseDto.of(UPDATE_SUCCESS, null));
	}
	
	@Operation(summary = "LOCAL 회원 탈퇴 1단계", description = "회원탈퇴를 위해 비밀번호를 입력받고 Access-Password-Token 발급.")
	@AutoSetMessageResponse
	@PreAuthorize("hasRole('LOCAL')")
	@PostMapping("/private/password")
	public ResponseEntity<ApiResponseDto<Map<String,String>>> validPassword(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestHeader(required = false, value = "X-Client-Type")String userAgent,
			@ValidPassword @RequestParam String password,
			HttpServletRequest request) {
		
		String clientIp = (String) request.getAttribute("clientIp");
		boolean isMobileApp = userAgent != null && userAgent.contains("mobile");
		
		String accessPasswordToken = userService.confirmPasswordAndCreatePasswordToken(userDetails.getUserId(), password, clientIp);
		
		if(isMobileApp) {
			return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS,Map.of("X-Access-Password-Token",accessPasswordToken)));
		} else {
			ResponseCookie cookie = ResponseCookie.from("X-Access-Password-Token",accessPasswordToken)
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(10))
					.sameSite("Lax")
					.build();
			
			return ResponseEntity
					.status(HttpStatus.OK) 
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(ApiResponseDto.of(SUCCESS_NO_DATA, null));
		}
	}
	
	@Operation(summary = "LOCAL 회원 탈퇴 2단계", description = "회원탈퇴 1단계에서 발급 받은 Access-Password-Token과 Access-Token 검증하여 회원탈퇴 처리.")
	@AutoSetMessageResponse
	@PreAuthorize("hasRole('LOCAL')")
	@PostMapping("/private/delete")
	public ResponseEntity<ApiResponseDto<Void>> stopUser(
			@ClientSpecific("X-Access-Password-Token") String accessPasswordToken,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		
		userService.deleteUser(userDetails.getUserId(), accessPasswordToken);
		
		return ResponseEntity.ok(ApiResponseDto.of(DELETE_SUCCESS, null));
	}
	
	// LOCAL 이메일 중복 검사
	@Operation(summary = "LOCAL 이메일 중복 검사", description = "2070: 중복 없음, 2072: 이메일 중복.")
	@AutoSetMessageResponse
	@PostMapping("/public/exist/email")
	public ResponseEntity<ApiResponseDto<Void>> isEmailExists(
			@ValidEmail @RequestParam String email) {
		userService.isEmailExists(UserProvider.LOCAL, email);
		return ResponseEntity.ok(ApiResponseDto.of(EMAIL_NOT_EXISTS, null));
	}
	
	@Operation(summary = "LOCAL 휴대폰 중복 검사", description = "2071: 중복 없음, 2073: 휴대폰 번호 중복.")
	@AutoSetMessageResponse
	@PostMapping("/public/exist/phone")
	public ResponseEntity<ApiResponseDto<Void>> isPhoneExists(
			@ValidPhone @RequestParam String phone) {
		userService.isPhoneExists(UserProvider.LOCAL, phone);
		return ResponseEntity.ok(ApiResponseDto.of(PHONE_NOT_EXISTS, null));
	}
	
}
