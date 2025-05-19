package net.chamman.moonnight.domain.user;

import static net.chamman.moonnight.global.exception.HttpStatusCode.EMAIL_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.PHONE_ALREADY_EXISTS;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_ILLEGAL;
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
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.global.exception.DuplicationException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.token.CustomTokenException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
	
	private final UserRepository userRepository;
	private final SignLogService signLogService;
	private final TokenProvider tokenStore;
	private final PasswordEncoder passwordEncoder;
	
	/** 
	 * @param userId
	 * @throws NoSuchDataException
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
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
	 * @throws NoSuchDataException
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
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
	 * @throws NoSuchDataException
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 * @return userProvider, email, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByUserProviderAndEmailAndPhone(UserProvider userProvider, String email, String phone) {
		
		User user = userRepository.findByUserProviderAndEmailAndPhone(userProvider, email, phone)
				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
		validateStatus(user);
		
		return user;
	}
	
	/**
	 * @param userProvider
	 * @param phone
	 * @param token 휴대폰 인증 토큰
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 * @return 휴대폰 인증 토큰과 userProvider, phone 일치하는 User 조회 및 status 검사
	 */
	public User getUserByVerifyPhone(UserProvider userProvider, String phone, String token) {
		
		String phoneRedis = tokenStore.getVerificationPhone(token);
		
		try {
			validateByReidsValue(phone, phoneRedis);
			
			User user = getUserByUserProviderAndPhone(userProvider, phone);
			
			return user;
		} finally {
			tokenStore.removeToken(TokenType.VERIFICATION_PHONE, token);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 * @return userProvider, email 일치하는 유저 
	 */
//	public boolean isActiveByUserProviderAndEmail(UserProvider userProvider, String email) {
//		UserStatus userStatus = userRepository.findUserStatusByUserProviderAndEmail(userProvider, email)
//				.orElseThrow(() -> new NoSuchDataException(USER_NOT_FOUND,"찾을 수 없는 유저."));
//		validateStatus(userStatus);
//		
//		return true;
//	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param token 휴대폰 인증 토큰
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @throws NoSuchDataException
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 * @throws CustomTokenException UUID 토큰 생성 및 Reids 저장 실패
	 * @return 비밀번호 찾기 자격 부여 토큰
	 */
	public String createFindPwTokenByVerifyPhone(UserProvider userProvider, String email, String phone, String token) {
		
		String phoneRedis = tokenStore.getVerificationPhone(token);
		
		try {
			validateByReidsValue(phone, phoneRedis);
			
			getUserByUserProviderAndEmailAndPhone(userProvider, email, phone);
			
			return tokenStore.createAccessFindPwToken(email);
		} finally {
			tokenStore.removeToken(TokenType.VERIFICATION_PHONE, token);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param token
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @return
	 */
	public String createFindPwTokenByVerifyEmail(UserProvider userProvider, String email, String token) {
		
		String emailRedis = tokenStore.getVerificationEmail(token);
		try {
			validateByReidsValue(email, emailRedis);
			
			getUserByUserProviderAndEmail(userProvider, email);
			
			return tokenStore.createAccessFindPwToken(email);
			
		} finally {
			tokenStore.removeToken(TokenType.VERIFICATION_EMAIL, token);
		}
	}
	
	/**
	 * @param userId
	 * @param password
	 * @return
	 */
	@Transactional
	public String verifyPasswordAndCreatePasswordToken(int userId, String password) {
		
		User user = getUserByUserId(userId);     
		
		if(!passwordEncoder.matches(password, user.getPassword())) {
			throw new IllegalArgumentException("비밀번호가 틀립니다.");
		}
		
		return tokenStore.createAccessPaaswordToken(user.getEmail());
	}
	
	/**
	 * @param token
	 * @param userProvider
	 * @param newPassword
	 * @param ip
	 * @return
	 */
	@Transactional
	public UserResponseDto updatePasswordByFindPwToken(String token, UserProvider userProvider, String newPassword, String ip) {
		
		String email = tokenStore.getAccessFindpwToken(token);
		
		try {
			
			User user = getUserByUserProviderAndEmail(userProvider, email);
			
			user.setPassword(passwordEncoder.encode(newPassword));
			user.setUserStatus(UserStatus.ACTIVE);
			userRepository.flush();
			
			// 로그인 실패기록에 비밀번호 변경으로 초기화 진행
			loginLogService.loginFailLogResolveByUpdatePassword(userProvider, email, ip);
			
			return UserResponseDto.fromEntity(userRepository.getReferenceById(user.getUserId())); 
		} finally {
			tokenStore.removeToken(TokenType.ACCESS_FINDPW , token);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param phone
	 * @param token
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @return
	 */
	@Transactional
	public UserResponseDto updatePhoneByVerification(UserProvider userProvider, String email, String phone, String token) {
		
		String phoneRedis = tokenStore.getVerificationPhone(token);
		try {
			validateByReidsValue(phone, phoneRedis);
			
			User user = getUserByUserProviderAndEmail(userProvider, email);
			
			user.setPhone(phone);
			userRepository.flush();
			
			return UserResponseDto.fromEntity(userRepository.getReferenceById(user.getUserId())); 
		} finally {
			tokenStore.removeToken(TokenType.VERIFICATION_PHONE, token);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param token
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 */
	@Transactional
	public void deleteUser(UserProvider userProvider, String email, String token) {
		String emailRedis = tokenStore.getAccessPasswordToken(token);
		try {
			validateByReidsValue(email, emailRedis);
			
			User user = getUserByUserProviderAndEmail(userProvider, email);
			
			user.setUserStatus(UserStatus.DELETE);
			
		} finally {
			tokenStore.removeToken(TokenType.ACCESS_PASSWORD, token);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @throws DuplicationException 이메일 중복
	 */
	public void isEmailExists(UserProvider userProvider, String email) {
		Optional<User> user = userRepository.findByUserProviderAndEmail(userProvider, email)
				.filter(e->e.getUserStatus()!=UserStatus.DELETE);
		
		if (user.isPresent()) {
			throw new DuplicationException(EMAIL_ALREADY_EXISTS);
		}
	}
	
	/**
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
	
	/**
	 * @param user
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
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
	 * @throws StatusStayException
	 * @throws StatusStopException
	 * @throws StatusDeleteExceptions
	 */
//	@SuppressWarnings("incomplete-switch")
//	private void validateStatus(UserStatus userStatus) {
//		switch (userStatus) {
//		case STAY -> throw new StatusStayException(USER_STATUS_STAY,"일시 정지된 계정.");
//		case STOP -> throw new StatusStopException(USER_STATUS_STOP,"정지된 계정.");
//		case DELETE -> throw new StatusDeleteException(USER_STATUS_DELETE,"탈퇴한 계정.");
//		}
//	}
	
	/**
	 * @param requestValue
	 * @param redisValue
	 * @throws IllegalTokenException redis에 저장되어있는 key의 value와 request 값 불일치.
	 */
	private void validateByReidsValue(String requestValue, String redisValue) {
		if(!Objects.equals(requestValue,redisValue)) {
			throw new IllegalTokenException(TOKEN_ILLEGAL,"redis에 저장되어있는 key의 value와 request 값 불일치. 입력값: {"+requestValue+"} != 저장값: {"+redisValue+"}.");
		}
	}
	
}