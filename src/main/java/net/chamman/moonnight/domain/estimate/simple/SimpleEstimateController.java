package net.chamman.moonnight.domain.estimate.simple;

import java.nio.file.AccessDeniedException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.verification.RateLimiterStore;
import net.chamman.moonnight.global.annotation.ValidId;
import net.chamman.moonnight.global.annotation.ValidPhone;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseUtil;

@RestController
@RequestMapping("/api/spem")
@RequiredArgsConstructor
public class SimpleEstimateController {
  
  private final SimpleEstimateService spemService;
  private final RateLimiterStore rateLimiter;

  // SimpleEstimate = Spem
  //1.로그인한 유저 조회 OAUTH, LOCAL
  //2.휴대폰 인증 유저 조회 AUTH
  //3.휴대포번호, 견적서번호 조회 GUEST
  
  @PermitAll
  @PostMapping("/public/register")
  public ResponseEntity<ApiResponseUtil<SimpleEstimateResponseDto>> registerSimpleEstimate(
      @Valid @RequestBody SimpleEstimateRequestDto simpleEstimateRequestDto,
      HttpServletRequest request) {
    
    String clientIp = (String) request.getAttribute("clientIp");

    rateLimiter.isAllowedByIp(clientIp);
    
    SimpleEstimateResponseDto spemResponseDto = 
        spemService.registerSpem(simpleEstimateRequestDto, clientIp);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponseUtil.of(200, "간편 견적 신청 완료", spemResponseDto));
  }
  
//  1
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @GetMapping("/private/user")
  public ResponseEntity<ApiResponseUtil<List<SimpleEstimateResponseDto>>> getMyAllEstimateByUserId(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    
    List<SimpleEstimateResponseDto> list = spemService.getMyAllSpem(userDetails.getUserId());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "조회 요청 성공", list));
  }
  
//  1
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @GetMapping("/private/user/{estimateId}")
  public ResponseEntity<ApiResponseUtil<SimpleEstimateResponseDto>> getMyEstimateByEstimateId(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @ValidId @PathVariable int spemId
      ) throws AccessDeniedException {
    
    SimpleEstimateResponseDto spemResponseDto = spemService.getMySpemBySpemId(spemId, userDetails.getUserId());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "조회 요청 성공", spemResponseDto));
  }
  
//  2
  @PreAuthorize("hasRole('AUTH')")
  @GetMapping("/private/auth")
  public ResponseEntity<ApiResponseUtil<List<SimpleEstimateResponseDto>>> getAllEstimateByAuthPhone(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    
    List<SimpleEstimateResponseDto> list = 
        spemService.getAllSpemByAuthPhone(userDetails.getUsername());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "조회 요청 성공", list));
  }
  
//  2
  @PreAuthorize("hasRole('AUTH')")
  @GetMapping("/private/auth/{estimateId}")
  public ResponseEntity<ApiResponseUtil<SimpleEstimateResponseDto>> getEstimateByAuthPhone(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @ValidId @PathVariable int spemId) throws AccessDeniedException {
    
    SimpleEstimateResponseDto simpleEstimateResponseDto = 
        spemService.getSpemBySpemIdAndAuthPhone(spemId,userDetails.getUsername());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "조회 요청 성공", simpleEstimateResponseDto));
  }
  
//  3
  @PostMapping("/public/guest")
  public ResponseEntity<ApiResponseUtil<SimpleEstimateResponseDto>> getEstimateByEstimateIdAndPhone(
      @ValidPhone @RequestParam String phone,
      @ValidId @RequestParam int spemId) throws AccessDeniedException {
    
    SimpleEstimateResponseDto spemResponseDto = spemService.getSpemBySpemIdAndPhone(spemId,phone);
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "조회 요청 성공", spemResponseDto));
  }
  
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @DeleteMapping("/private/{spemId}")
  public ResponseEntity<ApiResponseUtil<Void>> deleteEstimateByUser(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable int spemId) throws AccessDeniedException{
    
    spemService.deleteMySpem(spemId, userDetails.getUserId());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "삭제 요청 성공", null));
  }
  
  @PreAuthorize("hasRole('AUTH')")
  @DeleteMapping("/private/auth/{spemId}")
  public ResponseEntity<ApiResponseUtil<Void>> deleteEstimateByAuthPhone(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable int spemId) throws AccessDeniedException{
    
    spemService.deleteSpemByAuth(spemId, userDetails.getUsername());
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "삭제 요청 성공", null));
  }
  
  
}
