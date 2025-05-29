package net.chamman.moonnight.domain.estimate;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ESTIMATE_STATUS_DELETE;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.infra.s3.S3UploadException;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;
import net.chamman.moonnight.infra.s3.AwsS3Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EstimateService {
	
	private final EstimateRepository estimateRepository;
	private final UserService userService;
	private final AwsS3Service awsS3Service;
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
	 * @throws S3UploadException {@link AwsS3Service#uploadEstimateImages}
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return 등록된 견적서
	 */
	@Transactional
	public EstimateResponseDto registerEstimate(EstimateRequestDto estimateRequestDto, List<MultipartFile> images, int userId)  {
		
		List<String> imagesPath = null;
		
//		1. 이미지 S3 등록
		if (images != null && !images.isEmpty()) {
			imagesPath = awsS3Service.uploadEstimateImages(images, estimateRequestDto.phone());
		}
		
//		2. 견적서 DB 등록
		try {
			User user = userService.getUserByUserId(userId);
			
			Estimate estimate = estimateRequestDto.toEntity(user, imagesPath);
			estimateRepository.save(estimate);
			
			guidanceService.sendEstimateInfoSms(estimate.getPhone(), obfuscator.encode(estimate.getEstimateId())+"");
			
			return EstimateResponseDto.fromEntity(estimate, obfuscator);
		} catch (Exception e) {
//			3. 견적서 DB 등록 실패시 등록했던 이미지 S3 삭제 
	        if (imagesPath != null && !imagesPath.isEmpty()) {
	            try {
	                log.info("견적서 등록 중. DB 작업 실패로 인한 S3 이미지 롤백 시작. 삭제 대상 경로: {}", imagesPath);
	                awsS3Service.deleteEstimateImages(imagesPath);
	                log.info("S3 이미지 롤백 완료.");
	            } catch (Exception s3DeleteEx) {
	                log.error("S3 이미지 롤백 중 심각한 오류 발생! 삭제 대상 경로: {}. 에러: {}", imagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
	                guidanceService.sendAdminAlert("S3 이미지 롤백 중 심각한 오류 발생!");
	            }
	        }
	        throw e;
		}
	}
	
	/** 검증 없이 견적서 조회
	 * @param encodedEstimateId
	 * @throws NoSuchDataException {@link #getEstimateOrThrow} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #isDelete} 이미 삭제된 견적서
	 * @return 견적서 엔티티
	 */
	public Estimate getEstimateById(int encodedEstimateId) {
		Estimate estimate = estimateRepository.findById(obfuscator.decode(encodedEstimateId))
				.orElseThrow(() -> new NoSuchDataException(ESTIMATE_NOT_FOUND, "찾을 수 없는 견적서."));
		isDelete(estimate);
		
		return estimate;
	}

	/** OAUTH, LOCAL 견적서 리스트 조회
	 * @param userId
	 * @return 견적서 리스트
	 */
	public List<EstimateResponseDto> getMyAllEstimate(int userId) {
		return estimateRepository.findByUser_UserId(userId)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** OAUTH, LOCAL 견적서 조회
	 * @param encodedEstimateId
	 * @param userId
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 * @return 견적서
	 */
	public EstimateResponseDto getMyEstimateByEstimateId(int encodedEstimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, userId);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** AUTH 견적서 리스트 조회
	 * @param phone
	 * @return 견적서 리스트
	 */
	public List<EstimateResponseDto> getAllEstimateByAuthPhone(String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);
		return estimateRepository.findByPhone(phone)
				.stream()
				.filter(e->e.getEstimateStatus()!=EstimateStatus.DELETE)
				.map(e->EstimateResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** AUTH 견적서 조회
	 * @param encodedEstimateId
	 * @param phone
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 * 
	 * @return 견적서
	 */
	public EstimateResponseDto getEstimateByEstimateIdAndAuthPhone(int encodedEstimateId, String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);
		
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, phone);
		
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** jwt없이 휴대폰번호 + 난수화된 견적서 ID 통해 조회
	 * @param encodedEstimateId
	 * @param phone
	 * @return 견적서
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 */
	public EstimateResponseDto getEstimateByEstimateIdAndPhone(int encodedEstimateId, String phone) {
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, phone);
		return EstimateResponseDto.fromEntity(estimate, obfuscator);
	}
	
	/** OAUTH, LOCAL 견적서 수정
	 * @param estimateId
	 * @param estimateRequestDto
	 * @param images
	 * @param userId
	 * @return
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 * 
	 * @throws S3UploadException {@link #setNewEstimateAndSave} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	public EstimateResponseDto updateMyEstimate(int estimateId, EstimateRequestDto estimateRequestDto, List<MultipartFile> images, int userId) {

		Estimate estimate = getAuthorizedEstimate(estimateId, userId);

		Estimate updatedEstimate = setNewEstimateAndSave(estimate, estimateRequestDto, images);

		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
	/** AUTH 견적서 수정
	 * @param encodedEstimateId
	 * @param estimateRequestDto
	 * @param images
	 * @param phone
	 * @return
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 * 
	 * @throws S3UploadException {@link #setNewEstimateAndSave} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	public EstimateResponseDto updateEstimateByAuthPhone(int encodedEstimateId, EstimateRequestDto estimateRequestDto, List<MultipartFile> images, String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);

		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, phone);

		Estimate updatedEstimate = setNewEstimateAndSave(estimate, estimateRequestDto, images);

		return EstimateResponseDto.fromEntity(updatedEstimate, obfuscator);
	}
	
	/** OAUTH, LOCAL 견적서 삭제
	 * @param estimateId
	 * @param userId
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate} 이미 삭제된 견적서
	 */
	@Transactional
	public void deleteMyEstimate(int estimateId, int userId) {
		Estimate estimate = getAuthorizedEstimate(estimateId, userId);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** AUTH 견적서 삭제
	 * @param encodedEstimateId
	 * @param phone
	 * @throws NoSuchDataException {@link #getAuthorizedEstimate(int, String)} 찾을 수 없는 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate(int, String)} 견적서 조회 권한 이상
	 * @throws StatusDeleteException {@link #getAuthorizedEstimate(int, String)} 이미 삭제된 견적서
	 */
	@Transactional
	public void deleteEstimateByAuth(int encodedEstimateId, String phone) {
//		휴대폰 인증 jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		verificationService.validateVerify(phone);
		Estimate estimate = getAuthorizedEstimate(encodedEstimateId, phone);
		estimate.setEstimateStatus(EstimateStatus.DELETE);
	}
	
	/** 기존 견적서 엔티티에 새로운 값 SET 및 SAVE
	 * @param estimate 기존 엔티티
	 * @param estimateRequestDto 수정 될 객체
	 * @param images 수정 될 이미지
	 * @return 업데이트 된 엔티티
	 * @throws IOException
	 * @throws S3UploadException {@link AwsS3Service#uploadEstimateImages} AWS S3에 파일 업로드 중 오류 발생 시.
	 */
	@Transactional
	private Estimate setNewEstimateAndSave(Estimate estimate, EstimateRequestDto estimateRequestDto, List<MultipartFile> images) {
		
		List<String> oldImagesPath = estimate.getImagesPath();
		List<String> newImagesPath = null;
		
//		1. 새로운 이미지 S3 등록
		if (images != null && !images.isEmpty()) {
			newImagesPath = awsS3Service.uploadEstimateImages(images, estimateRequestDto.phone());
		}
		
//		2. 견적서 DB 업데이트
		try {
			estimate.setName(estimateRequestDto.name());
			estimate.setEmail(estimateRequestDto.email());
			estimate.setEmailAgree(estimateRequestDto.emailAgree());
			estimate.setSmsAgree(estimateRequestDto.smsAgree());
			estimate.setCallAgree(estimateRequestDto.callAgree());
			estimate.setPostcode(estimateRequestDto.postcode());
			estimate.setMainAddress(estimateRequestDto.mainAddress());
			estimate.setDetailAddress(estimateRequestDto.detailAddress());
			estimate.setContent(estimateRequestDto.content());
			estimate.setImagesPath(newImagesPath);
			estimateRepository.save(estimate);
		} catch (Exception e) {
//			3. 견적서 DB 업데이트 실패시 S3 등록했던 새로운 이미지 삭제
	        if (newImagesPath != null && !newImagesPath.isEmpty()) {
	            try {
	                log.info("견적서 업데이트 중. DB 작업 실패로 인한 S3 이미지 롤백 시작. 삭제 대상 경로: {}", newImagesPath);
	                awsS3Service.deleteEstimateImages(newImagesPath);
	                log.info("S3 이미지 롤백 완료.");
	            } catch (Exception s3DeleteEx) {
	                log.error("견적서 업데이트 중. S3 이미지 롤백 중 심각한 오류 발생! 삭제 대상 경로: {}. 에러: {}", newImagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
	                guidanceService.sendAdminAlert("S3 이미지 롤백 중 심각한 오류 발생!");
	            }
	        }
	        throw e;
		}
//		4. 기존 이미지 S3 삭제
		if (oldImagesPath!= null && !oldImagesPath.isEmpty()) {
			try {
				awsS3Service.deleteEstimateImages(oldImagesPath);
			} catch (Exception s3DeleteEx) {
                log.error("견적서 업데이트 중. 기존 S3 이미지 삭제 중 오류 발생! 삭제 대상 경로: {}. 에러: {}", oldImagesPath, s3DeleteEx.getMessage(), s3DeleteEx);
                guidanceService.sendAdminAlert("견적서 업데이트 중. 기존 S3 이미지 삭제 중 오류 발생");
			}
		}
		
		return estimateRepository.getReferenceById(estimate.getEstimateId());
	}
	
	/** 견적서 조회 및 검증
	 * @param encodedEstimateId
	 * @param userId
	 * @return 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #getEstimateById} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 */
	private Estimate getAuthorizedEstimate(int encodedEstimateId, int userId) {
//		jwt자체를 신뢰하는 방식통해 아래 코드 주석 처리 진행
//		userService.getUserByUserId(userId);
		Estimate estimate = getEstimateById(encodedEstimateId);
		
		if (estimate.getUser().getUserId() != userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"견적서 조회 권한 이상. address.getUser().getUserId(): "+estimate.getUser().getUserId()+"!= userId: "+userId);
		}
		
		return estimate;
	}
	
	/** 견적서 조회 및 검증
	 * @param encodedEstimateId
	 * @param phone
	 * @return 견적서 엔티티
	 * 
	 * @throws NoSuchDataException {@link #getEstimateById} 찾을 수 없는 견적서
	 * @throws StatusDeleteException {@link #getEstimateById} 삭제된 견적서
	 * @throws ForbiddenException {@link #getAuthorizedEstimate} 견적서 조회 권한 이상
	 */
	private Estimate getAuthorizedEstimate(int encodedEstimateId, String phone) {
		Estimate estimate = getEstimateById(encodedEstimateId);
		
		if (!Objects.equals(estimate.getPhone(), phone)) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"견적서 조회 권한 이상. estimate.getUser().getPhone(): "+estimate.getUser().getPhone()+"!= phone: "+phone);
		}
		
		return estimate;
	}

	/** 견적서 EstimateStatus.DELETE 검사
	 * @param estimate
	 * 
	 * @throws StatusDeleteException {@link #isDelete} 삭제된 견적서
	 */
	private void isDelete(Estimate estimate) {
		if (estimate.getEstimateStatus() == EstimateStatus.DELETE) {
			throw new StatusDeleteException(ESTIMATE_STATUS_DELETE,"삭제된 견적서.");
		}
	}
	
}
