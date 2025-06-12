package net.chamman.moonnight.domain.user;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_VALUE_MISMATCH;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STAY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.USER_STATUS_STOP;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;
import net.chamman.moonnight.auth.crypto.dto.FindPwTokenDto;
import net.chamman.moonnight.auth.crypto.dto.PasswordTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.sign.SignService;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.global.exception.DuplicationException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;
import net.chamman.moonnight.global.exception.user.MismatchPasswordException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final SignLogService signLogService;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	/** 유저 엔티티 조회
	 * @param userId
	 * @throws NoSuchDataException {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return userId 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserId(int userId) {
		
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/** 유저 엔티티 조회
	 * @param userProvider
	 * @param email
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmail} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return userProvider, email 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmail(UserProvider userProvider, String email) {
		
		User user = userRepository.findByUserProviderAndEmail(userProvider, email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/** 유저 엔티티 조회
	 * @param userProvider
	 * @param phone
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndPhone} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return userProvider, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndPhone(UserProvider userProvider, String phone) {
		
		User user = userRepository.findByUserProviderAndPhone(userProvider, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/** 유저 엔티티 조회
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmailAndPhone} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 * @return userProvider, email, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone) {
		
		User user = userRepository.findByUserProviderAndEmailAndPhone(userProvider, email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/** 유저 엔티티 조회
	 * @param userProvider
	 * @param phone
	 * @param verificationPhoneToken
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link VerificationService#isVerify} DB verificationId 일치하는 인증 요청 없음
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB 미인증된 인증 요청(시관 초과된 인증)
	 * @throws NotVerifyException {@link VerificationService#isVerify} DB 미인증된 인증 요청
	 * 
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndPhone} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getUserByUserProviderAndPhone} 일시정지 유저
	 * @throws StatusStopException {@link #getUserByUserProviderAndPhone} 중지 유저
	 * @throws StatusDeleteException {@link #getUserByUserProviderAndPhone} 탈퇴 유저
	 * 
	 * @return 휴대폰 번호와 일치하는 User 엔티티
	 */
	public User getUserByVerifyPhone(UserProvider userProvider, String phone, String verificationPhoneToken) {
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		
		User user = getUserByUserProviderAndPhone(userProvider, phone);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		return user;
	}
	
	/** 비밀번호 찾기 자격 검증
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param verificationPhoneToken
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link VerificationService#isVerify} DB verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB 미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException {@link VerificationService#isVerify} DB 미인증된 인증 요청.
	 * 
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmailAndPhone} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getUserByUserProviderAndEmailAndPhone} 일시정지 유저
	 * @throws StatusStopException {@link #getUserByUserProviderAndEmailAndPhone} 중지 유저
	 * @throws StatusDeleteException {@link #getUserByUserProviderAndEmailAndPhone} 탈퇴 유저
	 * 
     * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
     * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	public String createFindPwTokenByVerifyPhone(UserProvider userProvider, String email, String phone, String verificationPhoneToken) {
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		
		User user = getUserByUserProviderAndEmailAndPhone(userProvider, email, phone);
		
		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId()+"", email), FindPwTokenDto.TOKENTYPE); 
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		
		return findPwToken ;
	}
	
	/** 비밀번호 찾기 자격 검증
	 * @param userProvider
	 * @param email
	 * @param verificationEmailToken
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link VerificationService#isVerify} DB verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB 미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException {@link VerificationService#isVerify} DB 미인증된 인증 요청.
	 * 
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmail} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getUserByUserProviderAndEmail} 일시정지 유저
	 * @throws StatusStopException {@link #getUserByUserProviderAndEmail} 중지 유저
	 * @throws StatusDeleteException {@link #getUserByUserProviderAndEmail} 탈퇴 유저
	 * 
     * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
	 * 
	 * @return 비밀번호 변경 자격 토큰
	 */
	public String createFindPwTokenByVerifyEmail(UserProvider userProvider, String email, String verificationEmailToken) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		User user = getUserByUserProviderAndEmail(userProvider, email);

		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId() + "", email),FindPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return findPwToken;
	}
	
	/** 비밀번호 변경
	 * @param token FindPwToken
	 * @param userProvider
	 * @param newPassword
	 * @param ip
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #validateStatus} 일시정지 유저
	 * @throws StatusStopException {@link #validateStatus} 중지 유저
	 * @throws StatusDeleteException {@link #validateStatus} 탈퇴 유저
	 */
	@Transactional
	public void updatePasswordByFindPwToken(String token, UserProvider userProvider, String newPassword, String ip) {
		
		FindPwTokenDto findPwTokenDto = tokenProvider.getDecryptedTokenDto(FindPwTokenDto.TOKENTYPE,token);
		
		User user = getUserByUserId(findPwTokenDto.getIntUserId());
		
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setUserStatus(UserStatus.ACTIVE);
		userRepository.save(user);
		
		// 로그인 실패기록에 비밀번호 변경으로 초기화 진행
		signLogService.signFailLogResolveByUpdatePassword(userProvider, user.getEmail(), ip);
		
		tokenProvider.removeToken(TokenType.ACCESS_FINDPW , token);
	}
	
	/** 비밀번호 검증 및 토큰 발행
	 * @param userId
	 * @param password
	 * 
	 * @throws TooManySignFailException {@link SignService#validatePassword} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link SignService#validatePassword} 비밀번호 불일치
	 * 
	 * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
	 * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
	 * 
	 * @return 비밀번호 검증 완료 토큰
	 */
	@Transactional
	public String confirmPasswordAndCreatePasswordToken(int userId, String password, String clientIp) {
		
		User user = getUserByUserId(userId);     
		validatePassword(user, password, clientIp);
	
		return tokenProvider.createToken(new PasswordTokenDto(userId+"", user.getEmail()), PasswordTokenDto.TOKENTYPE);
	}

	/** 휴대폰 번호 변경
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param token
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link VerificationService#isVerify} DB verificationId 일치하는 인증 요청 없음
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB 미인증된 인증 요청(시관 초과된 인증)
	 * @throws NotVerifyException {@link VerificationService#isVerify} DB 미인증된 인증 요청
	 * 
	 * @throws NoSuchDataException {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link #getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link #getUserByUserId} 탈퇴 유저
	 * @return 유저
	 */
	@Transactional
	public void updatePhoneByVerification(int userId, String phone, String token) {

		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		
		User user = getUserByUserId(userId);
		
		user.setPhone(phone);
		userRepository.save(user);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, token);
	}
	
	/** 회원탈퇴 (유저 상태 DELETE로 변경)
	 * @param userProvider
	 * @param email
	 * @param passwordToken
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws TokenValueMismatchException {@link #deleteUser} 로그인되어있는 userId와 PasswordToken.userId 불일치
	 * 
	 * @throws NoSuchDataException {@link #getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link #getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link #getUserByUserId} 탈퇴 유저
	 */
	@Transactional
	public void deleteUser(int userId, String passwordToken) {
		log.debug("*회원 탈퇴 서비스. UserId: [{}], PasswordToken: [{}]",
				LogMaskingUtil.maskId(userId, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(passwordToken, MaskLevel.MEDIUM)
				);
		
		PasswordTokenDto passwordTokenDto = tokenProvider.getDecryptedTokenDto(PasswordTokenDto.TOKENTYPE, passwordToken);
		if(!Objects.equals(userId, passwordTokenDto.getIntUserId())) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"로그인되어있는 userId와 PasswordToken.userId 불일치"); 
		}
		
		User user = getUserByUserId(userId);
		
		user.setUserStatus(UserStatus.DELETE);
		userRepository.save(user);
		
		tokenProvider.removeToken(TokenType.ACCESS_PASSWORD, passwordToken);
	}
	
	/** LOCAL 유저 이메일 중복 검사
	 * @param userProvider
	 * @param email
	 * @throws DuplicationException {@link #isEmailExists} 이메일 중복
	 */
	public void isEmailExists(UserProvider userProvider, String email) {
		Optional<User> user = userRepository.findByUserProviderAndEmail(userProvider, email)
				.filter(e->e.getUserStatus()!=UserStatus.DELETE);
		
		if (user.isPresent()) {
			throw new DuplicationException(EMAIL_ALREADY_EXISTS);
		}
	}
	
	/** LOCAL 유저 휴대폰 번호 중복 검사
	 * @param userProvider
	 * @param phone
	 * @throws DuplicationException {@link #isPhoneExists} 휴대폰 중복
	 */
	public void isPhoneExists(UserProvider userProvider, String phone) {
		Optional<User> user = userRepository.findByUserProviderAndPhone(userProvider, phone)
				.filter(e->e.getUserStatus()!=UserStatus.DELETE);
		if (user.isPresent()) {
			throw new DuplicationException(PHONE_ALREADY_EXISTS);
		}
	}
	
	/** 비밀번호 검증 및 결과 `LoginLog` 기록
	 * @param user
	 * @param reqPassword
	 * @param ip
	 * @throws TooManySignFailException {@link SignLogService#validSignFailCount} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 */
	public void validatePassword(User user, String reqPassword, String ip) {
		if (!passwordEncoder.matches(reqPassword, user.getPassword())) {
			signLogService.registerSignLog(UserProvider.LOCAL, user.getEmail(), ip, SignResult.INVALID_PASSWORD);
			int signFailCount;
			try {
				signFailCount = signLogService.validSignFailCount(UserProvider.LOCAL, user.getEmail());
			} catch (TooManySignFailException e) {
				user.setUserStatus(UserStatus.STAY);
				userRepository.save(user);
				log.warn("로그인 실패 횟수 초과로 계정 일시정지: email={}, ip={}", user.getEmail(), ip);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED,"비밀번호 불일치. 실패 횟수: "+signFailCount);
		}
		signLogService.registerSignLog(UserProvider.LOCAL, user.getEmail(), ip, SignResult.LOCAL_SUCCESS);
	}
	
	/** 유저 상태 검사
	 * @param user
	 * @throws StatusStayException {@link #validateStatus}
	 * @throws StatusStopException {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
	@SuppressWarnings("incomplete-switch")
	private void validateStatus(User user) {
		switch (user.getUserStatus()) {
		case STAY -> throw new StatusStayException(USER_STATUS_STAY,"일시 정지된 계정. user.id: "+user.getUserId());
		case STOP -> throw new StatusStopException(USER_STATUS_STOP,"정지된 계정. user.id: "+user.getUserId());
		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE,"탈퇴한 계정. user.id: "+user.getUserId());
		}
	}
	
	
	/**
	 * @param userStatus
	 * @throws StatusStayException {@link #validateStatus}
	 * @throws StatusStopException {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 */
//	@SuppressWarnings("incomplete-switch")
//	private void validateStatus(UserStatus userStatus) {
//		switch (userStatus) {
//		case STAY -> throw new StatusStayException(USER_STATUS_STAY,"일시 정지된 계정.");
//		case STOP -> throw new StatusStopException(USER_STATUS_STOP,"정지된 계정.");
//		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE,"탈퇴한 계정.");
//		}
//	}
	
	/** Redis에 저장되어있는 Key의 Value와 Request 값 비교
	 * @param requestValue
	 * @param redisValue
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 */
//	private void validateByReidsValue(String requestValue, String redisValue) {
//		if(!Objects.equals(requestValue,redisValue)) {
//			throw new TokenValueMismatchException(TOKEN_ILLEGAL,"redis에 저장되어있는 key의 value와 request 값 불일치. 입력값: {"+requestValue+"} != 저장값: {"+redisValue+"}.");
//		}
//	}
	
}