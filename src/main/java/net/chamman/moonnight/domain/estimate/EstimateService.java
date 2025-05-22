package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;
import net.chamman.moonnight.infra.s3.AwsS3Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EstimateService {
	
	private final EstimateRepository estimateRepository;
	private final UserService userService;
	private final AwsS3Service awsS3Service;
	private final VerificationService verificationService;
	private final GuidanceService guidanceService;
	private final Obfuscator obfuscator;
	
	//1.로그인한 유저 조회 OAUTH, LOCAL
	//2.휴대폰 인증 유저 조회 AUTH
	//3.휴대포번호, 견적서번호 조회 GUEST
	
	/** OAUTH, LOCAL 견적서 등록
	 * @param estimateRequestDto
	 * @param images
	 * @param userId
	 * 
	 * 
	 * 
	 * @return
	 */
	@Transactional
	public EstimateResponseDto registerEstimate(EstimateRequestDto estimateRequestDto, List<MultipartFile> images, int userId)  {
		
		List<String> imagesPath = null;
		try {
			if (images != null && !images.isEmpty()) {
				imagesPath = awsS3Service.uploadEstimateImages(images, estimateRequestDto.phone());
			}
		}catch(IOException e) {
			log.warn("견적서 이미지 업로드 실패. phone: {}", estimateRequestDto.phone());
			e.printStackTrace();
		}
		
		User user;
		try {
			user = userService.getUserByUserId(userId);
		}catch(NoSuchElementException e) {
			user = null;
		}
		
		Estimate estimate = estimateRequestDto.toEntity(user, imagesPath);
		estimateRepository.save(estimate);
		
		try {
			guidanceService.sendEstimateInfoSms(estimate.getPhone(), obfuscator.encode(estimate.getEstimateId())+"");
		} catch (InvalidKeyException | JsonProcessingException | NoSuchAlgorithmException
				| UnsupportedEncodingException | URISyntaxException e) {
			log.warn("견적서 등록 이후 문자 발송 실패. phone: {}, estimateId: {}",estimate.getPhone(), estimate.getEstimateId());
			e.printStackTrace();
		}
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** 견적서 조회
	 * @param estimateId
	 * @throws NoSuchDataException {@link #getEstimateById} 찾을 수 없는 견적서 
	 * @return
	 */
	public Estimate getEstimateById(int encodedEstimateId) {
		return estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND,"찾을 수 없는 견적서."));
	}
	
//  1
	public List<EstimateResponseDto> getMyAllEstimate(int userId) {
		return estimateRepository.findByUser_UserId(userId)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
//  1
	public EstimateResponseDto getMyEstimateByEstimateId(int estimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
//  2
	public List<EstimateResponseDto> getAllEstimateByAuthPhone(String phone) {
		verificationService.validateVerify(phone);
		return estimateRepository.findByPhone(phone)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
//  2
	public EstimateResponseDto getEstimateByEstimateIdAndAuthPhone(int estimateId, String phone) {
		verificationService.validateVerify(phone);  // 본인 인증 로직
		Estimate estimate = getAuthorizedEstimate(estimateId, phone);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
//  3
	public EstimateResponseDto getEstimateByEstimateIdAndPhone(int estimateId, String phone) {
		Estimate estimate = getAuthorizedEstimate(estimateId, phone);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
//  1
	@Transactional
	public EstimateResponseDto updateMyEstimate(
			int estimateId,
			EstimateRequestDto estimateRequestDto,
			List<MultipartFile> images, 
			int userId) throws IOException {
		
		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		
		Estimate updatedEstimate = updateEstimate(estimate, estimateRequestDto, images);
		
		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
//  2
	@Transactional
	public EstimateResponseDto updateEstimateByAuthPhone(
			int estimateId,
			EstimateRequestDto estimateRequestDto, 
			List<MultipartFile> images, 
			String phone) throws IOException {
		
		verificationService.validateVerify(phone);
		
		Estimate estimate = getAuthorizedEstimate(estimateId, phone);
		
		Estimate updatedEstimate = updateEstimate(estimate, estimateRequestDto, images);
		
		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
	//회원 전용
	@Transactional
	public void deleteMyEstimate(int estimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	//비회원 전용
	@Transactional
	public void deleteEstimateByAuth(int estimateId, String phone) {
		verificationService.validateVerify(phone);
		Estimate estimate = getAuthorizedEstimate(estimateId, phone);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	private Estimate updateEstimate(Estimate estimate, EstimateRequestDto estimateRequestDto, List<MultipartFile> images) throws IOException {
		if (estimate.getImagesPath() != null && !estimate.getImagesPath().isEmpty()) {
			awsS3Service.deleteEstimateImages(estimate.getImagesPath());
		}
		List<String> imagesPath = null;
		if (images != null && !images.isEmpty()) {
			imagesPath = awsS3Service.uploadEstimateImages(images, estimateRequestDto.phone());
		}
		
		estimate.setName(estimateRequestDto.name());
		estimate.setEmail(estimateRequestDto.email());
		estimate.setEmailAgree(estimateRequestDto.emailAgree());
		estimate.setSmsAgree(estimateRequestDto.smsAgree());
		estimate.setCallAgree(estimateRequestDto.callAgree());
		estimate.setPostcode(estimateRequestDto.postcode());
		estimate.setMainAddress(estimateRequestDto.mainAddress());
		estimate.setDetailAddress(estimateRequestDto.detailAddress());
		estimate.setContent(estimateRequestDto.content());
		estimate.setImagesPath(imagesPath);
		estimateRepository.flush();
		
		return estimateRepository.getReferenceById(estimate.getEstimateId());
	}
	
	
	protected Estimate getEstimateOrThrow(int estimateId) {
		return estimateRepository.findById(obfuscator.decode(estimateId))
				.orElseThrow(() -> new NoSuchElementException("일치하는 견적서가 없습니다."));
	}
	
	protected void validateNotDeleted(Estimate estimate) {
		if (estimate.getEstimateStatus() == EstimateStatus.DELETE) {
			throw new StatusDeleteException("이미 삭제된 견적서 입니다.");
		}
	}
	
	private Estimate getAuthorizedEstimate(int estimateId, int userId) {
		Estimate estimate = getEstimateOrThrow(estimateId);
		
		if (estimate.getUser().getUserId() != userId) {
			throw new ForbiddenException("견적서를 조회할 수 없습니다.");
		}
		
		validateNotDeleted(estimate);
		
		return estimate;
	}
	
	private Estimate getAuthorizedEstimate(int estimateId, String phone) {
		Estimate estimate = getEstimateOrThrow(estimateId);
		
		if (!Objects.equals(estimate.getPhone(), phone)) {
			throw new ForbiddenException("견적서를 조회할 수 없습니다.");
		}
		
		validateNotDeleted(estimate);
		
		return estimate;
	}
	
}
