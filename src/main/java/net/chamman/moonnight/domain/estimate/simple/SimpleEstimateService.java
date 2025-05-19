package net.chamman.moonnight.domain.estimate.simple;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.StatusDeleteException;

@Service
@RequiredArgsConstructor
public class SimpleEstimateService {
  
  private final SimpleEstimateRepository spemRepository;
  private final VerificationService verificationService;
  private final Obfuscator obfuscator;
  
  // SimpleEstimate = Spem
  //1.로그인한 유저 조회 OAUTH, USER
  //2.휴대폰 인증 유저 조회 AUTH
  //3.휴대포번호, 견적서번호 조회 GUEST

  @Transactional
  public SimpleEstimateResponseDto registerSpem(
      SimpleEstimateRequestDto pemRequestDto,
      String clientIp) {
    
    SimpleEstimate spem = pemRequestDto.toEntity();
    spem.setRequestIp(clientIp);
    spemRepository.save(spem);
    
    return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
  }
  
//  1
  public List<SimpleEstimateResponseDto> getMyAllSpem(int userId) {
    return spemRepository.findByUser_UserId(userId)
        .stream()
        .filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
        .map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
        .collect(Collectors.toList());
  }
  
//  1
  public SimpleEstimateResponseDto getMySpemBySpemId(int spemId, int userId) {
    SimpleEstimate spem = spemRepository.findById(obfuscator.decode(spemId))
        .orElseThrow(() -> new IllegalArgumentException("일치하는 견적서가 없습니다."));
    
    validateNotDeleted(spem);
    
    return Optional.of(spem)
        .filter(e -> e.getUser().getUserId() == userId)
        .map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
        .orElseThrow(() -> new ForbiddenException("해당 견적서를 조회할 권한이 없습니다."));
  }
  
//  2
  public List<SimpleEstimateResponseDto> getAllSpemByAuthPhone(String phone) {
    verificationService.validateVerify(phone);
    return spemRepository.findByPhone(phone)
        .stream()
        .filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
        .map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
        .collect(Collectors.toList());
  }
  
//  2
  public SimpleEstimateResponseDto getSpemBySpemIdAndAuthPhone(int spemId, String phone) {
    verificationService.validateVerify(phone);

    SimpleEstimate spem = spemRepository.findById(obfuscator.decode(spemId))
        .orElseThrow(()->new NoSuchElementException("일치하는 견적서가 없습니다."));
    
    validateNotDeleted(spem);

    return Optional.of(spem)
        .filter(e -> e.getPhone() == phone)
        .map(e->SimpleEstimateResponseDto.fromEntity(e, obfuscator))
        .orElseThrow(() -> new ForbiddenException("해당 견적서를 조회할 권한이 없습니다."));
  }
  
//  3
  public SimpleEstimateResponseDto getSpemBySpemIdAndPhone(int spemId, String phone) {
    SimpleEstimate spem = spemRepository.findById(obfuscator.decode(spemId))
      .filter(e->e.getPhone()==phone)
      .orElseThrow(()->new NoSuchElementException("일치하는 견적서가 없습니다."));
    
    validateNotDeleted(spem);
    
    return SimpleEstimateResponseDto.fromEntity(spem, obfuscator);
  }
  
//  1
  @Transactional
  public void deleteMySpem(int spemId, int userId) {
    
    SimpleEstimate spem = getAuthorizedSpem(spemId, userId);
    
    spem.setEstimateStatus(EstimateStatus.DELETE);
  }
  
//  2
  @Transactional
  public void deleteSpemByAuth(int spemId, String phone) {
    
    SimpleEstimate spem = getAuthorizedSpem(spemId, phone);
    
    spem.setEstimateStatus(EstimateStatus.DELETE);
  }
  
  private SimpleEstimate getSpemOrThrow(int spemId) {
    return spemRepository.findById(obfuscator.decode(spemId))
        .orElseThrow(() -> new NoSuchElementException("일치하는 견적서가 없습니다."));
  }
  
  private void validateNotDeleted(SimpleEstimate spem) {
    if(spem.getEstimateStatus()==EstimateStatus.DELETE) {
      throw new StatusDeleteException("삭제된 간편 견적서 입니다.");
    }
  }
  
  private SimpleEstimate getAuthorizedSpem(int spemId, int userId) {
    SimpleEstimate spem = getSpemOrThrow(spemId);
    
    if (spem.getUser().getUserId() != userId) {
        throw new ForbiddenException("견적서를 조회할 수 없습니다.");
    }
    
    validateNotDeleted(spem);
    
    return spem;
  }
  
  private SimpleEstimate getAuthorizedSpem(int spemId, String phone) {
    SimpleEstimate spem = getSpemOrThrow(spemId);
    
    if (spem.getPhone() != phone) {
      throw new ForbiddenException("견적서를 조회할 수 없습니다.");
    }
    
    validateNotDeleted(spem);
    
    return spem;
  }
  
  
  
}
