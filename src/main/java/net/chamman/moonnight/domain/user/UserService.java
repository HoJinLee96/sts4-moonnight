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
import net.chamman.moonnight.global.exception.sign.MismatchPasswordException;
import net.chamman.moonnight.global.exception.token.CustomTokenException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final VerificationService verificationService;
	private final UserRepository userRepository;
	private final SignLogService signLogService;
	private final TokenProvider tokenProvider;
	private final PasswordEncoder passwordEncoder;

	/** 
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
	
	/**
	 * @param userProvider
	 * @param email
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusStayException {@link #validateStatus}
	 * @throws StatusStopException {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 * @return userProvider, email 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmail(UserProvider userProvider, String email) {
		
		User user = userRepository.findByUserProviderAndEmail(userProvider, email)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/**
	 * @param userProvider
	 * @param phone
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndPhone}
	 * @throws StatusStayException {@link #validateStatus}
	 * @throws StatusStopException {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 * @return userProvider, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndPhone(UserProvider userProvider, String phone) {
		
		User user = userRepository.findByUserProviderAndPhone(userProvider, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmailAndPhone}
	 * @throws StatusStayException {@link #validateStatus}
	 * @throws StatusStopException {@link #validateStatus}
	 * @throws StatusDeleteExceptions {@link #validateStatus}
	 * @return userProvider, email, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone) {
		
		User user = userRepository.findByUserProviderAndEmailAndPhone(userProvider, email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/** 휴대폰 인증 토큰 검증 및 User 반환
	 * @param userProvider
	 * @param phone
	 * @param token 휴대폰 인증 토큰
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 * @throws StatusStayException {@link #getUserByUserProviderAndPhone}
	 * @throws StatusStopException {@link #getUserByUserProviderAndPhone}
	 * @throws StatusDeleteExceptions {@link #getUserByUserProviderAndPhone}
	 * @return 휴대폰 번호와 일치하는 User
	 */
	public User getUserByVerifyPhone(UserProvider userProvider, String phone, String token) {
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		
		User user = getUserByUserProviderAndPhone(userProvider, phone);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, token);
		return user;
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @throws StatusStayException {@link #getUserByUserProviderAndPhone}
	 * @throws StatusStopException {@link #getUserByUserProviderAndPhone}
	 * @throws StatusDeleteExceptions {@link #getUserByUserProviderAndPhone}
	 * @return userProvider, email 일치하는 유저 
	 */
//	public boolean isActiveByUserProviderAndEmail(UserProvider userProvider, String email) {
//		UserStatus userStatus = userRepository.findUserStatusByUserProviderAndEmail(userProvider, email)
//				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
//		validateStatus(userStatus);
//		
//		return true;
//	}
	
	/** 비밀번호 찾기 자격 검증
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param token 휴대폰 인증 토큰
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 * @throws NoSuchDataException
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 * @throws CustomTokenException UUID 토큰 생성 및 Reids 저장 실패
	 * @return 토큰
	 */
	public String createFindPwTokenByVerifyPhone(UserProvider userProvider, String email, String phone, String token) {
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());
		
		User user = getUserByUserProviderAndEmailAndPhone(userProvider, email, phone);
		
		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId()+"", email), FindPwTokenDto.TOKENTYPE); 
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, token);
		
		return findPwToken ;
	}
	
	/** 비밀번호 찾기 자격 검증
	 * @param userProvider
	 * @param email
	 * @param token
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 * @return 토큰
	 */
	public String createFindPwTokenByVerifyEmail(UserProvider userProvider, String email, String token) {

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, token);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		User user = getUserByUserProviderAndEmail(userProvider, email);

		String findPwToken = tokenProvider.createToken(new FindPwTokenDto(user.getUserId() + "", email),FindPwTokenDto.TOKENTYPE);

		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, token);

		return findPwToken;
	}
	
	/** 비밀번호 재검증 및 토큰 발행
	 * @param userId
	 * @param password
	 * @throws MismatchPasswordException {@link #verifyPasswordAndCreatePasswordToken}
	 * @return 토큰
	 */
	@Transactional
	public String verifyPasswordAndCreatePasswordToken(int userId, String password) {
		
		User user = getUserByUserId(userId);     
		
		if(!passwordEncoder.matches(password, user.getPassword())) {
			throw new MismatchPasswordException(SIGNIN_FAILED,"비밀번호 불일치.");
		}

		return tokenProvider.createToken(new PasswordTokenDto(userId+"", user.getEmail()), PasswordTokenDto.TOKENTYPE);
	}
	
	/** 비밀번호 변경
	 * @param token FindPwToken
	 * @param userProvider
	 * @param newPassword
	 * @param ip
	 * @throws IllegalTokenException {@link TokenProvider#getAccessFindpwToken} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getAccessFindpwToken} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getAccessFindpwToken} 복호화 실패 
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusStayException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusStopException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusDeleteExceptions {@link #getUserByUserProviderAndEmail}
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
	
	/** 휴대폰 번호 변경
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param token
	 * @throws IllegalTokenException {@link TokenProvider#getVerificationPhone} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getVerificationPhone} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getVerificationPhone} 복호화 실패 
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 * @throws NoSuchDataException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusStayException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusStopException {@link #getUserByUserProviderAndEmail}
	 * @throws StatusDeleteExceptions {@link #getUserByUserProviderAndEmail}
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
	 * @param token
	 * @throws IllegalTokenException {@link TokenProvider#getAccessPasswordToken} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getAccessPasswordToken} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getAccessPasswordToken} 복호화 실패 
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 Key의 Value와 Request 값 불일치.
	 */
	@Transactional
	public void deleteUser(int userId, String token) {
		
		PasswordTokenDto passwordTokenDto = tokenProvider.getDecryptedTokenDto(PasswordTokenDto.TOKENTYPE, token);
		if(!Objects.equals(userId, passwordTokenDto.getIntUserId())) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"로그인되어있는 userId와 PasswordToken.userId 불일치"); 
		}
		
		User user = getUserByUserId(userId);
		
		user.setUserStatus(UserStatus.DELETE);
		userRepository.save(user);
		
		tokenProvider.removeToken(TokenType.ACCESS_PASSWORD, token);
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