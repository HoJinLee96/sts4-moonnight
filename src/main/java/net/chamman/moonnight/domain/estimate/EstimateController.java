package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.CREATE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.DELETE_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.READ_SUCCESS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.UPDATE_SUCCESS;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.verification.RateLimiterStore;
import net.chamman.moonnight.global.annotation.ImageConstraint;
import net.chamman.moonnight.global.annotation.ValidId;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.security.principal.SilentUserDetails;
import net.chamman.moonnight.global.util.ApiResponseDto;

@RestController
@RequestMapping("/api/estimate")
@MultipartConfig
@RequiredArgsConstructor
public class EstimateController {
	
	private final EstimateService estimateService;
	private final RateLimiterStore rateLimiter;
	
//  견적서 등록
	@Operation(summary = "견적서 등록", description = "견적서 등록")
	@PostMapping("/public/register")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> registerEstimate(
			@AuthenticationPrincipal SilentUserDetails userDetails,
			@Valid @RequestPart EstimateRequestDto estimateRequestDto,
			@Valid @ImageConstraint @RequestPart List<MultipartFile> images,
			HttpServletRequest request) throws IOException {
		
		String clientIp = (String) request.getAttribute("clientIp");
		rateLimiter.isAllowedByIp(clientIp);
		
		EstimateResponseDto estimateResponseDto = 
				estimateService.registerEstimate(estimateRequestDto, images, userDetails.getUserId());
		
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDto.of(CREATE_SUCCESS, estimateResponseDto));
	}
	
//  유저 견적서 전체 조회
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/user")
	public ResponseEntity<ApiResponseDto<List<EstimateResponseDto>>> getMyAllEstimateByUserId(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		
		List<EstimateResponseDto> list = estimateService.getMyAllEstimate(userDetails.getUserId());
		
		return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS, list));
	}
	
//  유저 견적서 단건 조회
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@GetMapping("/private/user/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getMyEstimateByEstimateId(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId) {
		
		EstimateResponseDto estimateResponseDto = estimateService.getMyEstimateByEstimateId(encodedEstimateId,userDetails.getUserId());
		
		return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS, estimateResponseDto));
	}
	
//  인증된 전화번호로 견적서 전체 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/private/auth")
	public ResponseEntity<ApiResponseDto<List<EstimateResponseDto>>> getAllEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		
		List<EstimateResponseDto> list = 
				estimateService.getAllEstimateByAuthPhone(userDetails.getUsername());
		
		return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS, list));
	}
	
//  인증된 전화번호로 견적서 단건 조회
	@PreAuthorize("hasRole('AUTH')")
	@GetMapping("/auth/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> getEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId)  {
		
		EstimateResponseDto estimateResponseDto = 
				estimateService.getEstimateByEstimateIdAndPhone(encodedEstimateId,userDetails.getUsername());
		
		return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS, estimateResponseDto));
	}
	
//  비회원 견적서 조회
	@PostMapping("/public/guest")
	public ResponseEntity<?> getEstimateByEstimateIdAndPhone(
			@ValidId @RequestParam int encodedEstimateId,
			@ValidPhone @RequestParam String phone)  {
		
		EstimateResponseDto estimateResponseDto = estimateService.getEstimateByEstimateIdAndPhone(encodedEstimateId,phone);
		
		return ResponseEntity.ok(ApiResponseDto.of(READ_SUCCESS, estimateResponseDto));
	}
	
//  유저 견적서 수정
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PostMapping("/private/update/{estimateId}")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByUser(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId, 
			@Valid @RequestPart("estimate") EstimateRequestDto estimateRequestDto,
			@ImageConstraint @RequestPart("images") List<MultipartFile> images,
			HttpServletRequest request) throws IOException{
		
		EstimateResponseDto estimateResponseDto = 
				estimateService.updateMyEstimate(encodedEstimateId, estimateRequestDto, images, userDetails.getUserId());
		
		return ResponseEntity.ok(ApiResponseDto.of(UPDATE_SUCCESS, estimateResponseDto));
	}
	
//  인증된 전화번호로 견적서 수정
	@PreAuthorize("hasRole('AUTH')")
	@PostMapping("/private/auth/update")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> updateEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId, 
			@Valid @RequestPart EstimateRequestDto estimateRequestDto,
			@ImageConstraint @RequestPart("images") List<MultipartFile> images,
			HttpServletRequest request) throws IOException{
		
		EstimateResponseDto estimateResponseDto = 
				estimateService.updateEstimateByAuthPhone(encodedEstimateId, estimateRequestDto, images, userDetails.getUsername());
		
		return ResponseEntity.ok(ApiResponseDto.of(UPDATE_SUCCESS, estimateResponseDto));
	}
	
//  사용자 견적서 삭제
	@PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
	@PostMapping("/private/delete")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> deleteEstimateByUser(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId) {
		
		estimateService.deleteMyEstimate(encodedEstimateId, userDetails.getUserId());
		
		return ResponseEntity.ok(ApiResponseDto.of(DELETE_SUCCESS, null));
	}
	
//  인증된 전화번호로 견적서 삭제
	@PreAuthorize("hasRole('AUTH') ")
	@PostMapping("/private/auth/delete")
	public ResponseEntity<ApiResponseDto<EstimateResponseDto>> deleteEstimateByAuthPhone(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("estimateId") int encodedEstimateId) {
		
		estimateService.deleteEstimateByAuth(encodedEstimateId, userDetails.getUsername());
		
		return ResponseEntity.ok(ApiResponseDto.of(DELETE_SUCCESS, null));
	}
	
}
