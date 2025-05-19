package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_VALUE_MISMATCH;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.JwtProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.verification.Verification;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.address.Address;
import net.chamman.moonnight.domain.address.AddressRepository;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.User.UserStatus;
import net.chamman.moonnight.domain.user.UserCreateRequestDto;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.DuplicationException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.CustomJwtException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.sign.MismatchPasswordException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;
import net.chamman.moonnight.infra.naver.sms.GuidanceService;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignService {

	private final UserService userService;
	private final AddressRepository addressRepository;
	private final VerificationService verificationService;
	private final UserRepository userRepository;
	private final SignLogService signLogService ;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final TokenProvider tokenProvider;
	private final GuidanceService guidanceService;
	private final Map<UserProvider,String> roles = Map.of(UserProvider.LOCAL,"ROLE_LOCAL",UserProvider.KAKAO,"ROLE_OAUTH",UserProvider.NAVER,"ROLE_OAUTH");
	

	/** 회원가입 1차
	 * @param email
	 * @param password
	 * @param valificationEmailToken
	 * @throws IllegalTokenException {@link TokenProvider#getVerificationEmail} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getVerificationEmail} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getVerificationEmail} 복호화 실패 
	 * @throws IllegalTokenException {@link SignService#validateByReidsValue} Redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @throws DuplicationException {@link UserService#isEmailExists} 이메일 중복
     * @throws EncryptException {@link TokenProvider#createMapToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#removeToken} Redis 저장 실패
	 * @return
	 */
	@Transactional
	public String createJoinToken(String email, String password, String valificationEmailToken) {
		
		String emailRedis = tokenProvider.getVerificationEmail(valificationEmailToken);
		try {
			validateByReidsValue(email, emailRedis);
			
			userService.isEmailExists(UserProvider.LOCAL, email);
			
			return tokenProvider.createMapToken(TokenType.ACCESS_SIGNUP, Map.of("email",email,"password",password));
		} finally {
			tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, valificationEmailToken);
		}
	}
	
	/** 회원가입 2차
	 * @param userCreateRequestDto
	 * @param accessJoinToken 회원가입 1차 토큰
	 * @param verificationPhoneToken 휴대폰 인증 토큰
     * @throws IllegalTokenException {@link TokenProvider#getMapTokenData} 적합하지 않은 토큰
     * @throws NoSuchTokenException {@link TokenProvider#getMapTokenData} Redis에 일치한 Key 없음
     * @throws DecryptException {@link TokenProvider#getMapTokenData} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getMapTokenData}
	 * @throws IllegalTokenException {@link TokenProvider#getVerificationPhone} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getVerificationPhone} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getVerificationPhone} 복호화 실패 
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 key의 value와 request 값 불일치.
	 * @throws DuplicationException {@link UserService#isPhoneExists} 휴대폰 중복
	 * @return 회원 가입 유저 이름
	 */
	@Transactional
	public String signUpLocalUser(UserCreateRequestDto userCreateRequestDto, String accessJoinToken, String verificationPhoneToken) {
		
		Map<String,String> mapValue = tokenProvider.getMapTokenData(TokenType.ACCESS_SIGNUP, accessJoinToken);
		String phoneRedis = tokenProvider.getVerificationPhone(verificationPhoneToken);
		
		try {
			validateByReidsValue(userCreateRequestDto.phone(), phoneRedis); 
			
			userService.isPhoneExists(UserProvider.LOCAL, phoneRedis);
			
			// 비밀번호 인코딩 후 저장
			String encodePassoword = passwordEncoder.encode(mapValue.get("password"));
			
			User user = userCreateRequestDto.toEntity();
			user.setEmail(mapValue.get("email"));
			user.setPassword(encodePassoword);
			userRepository.save(user);
			
			Address address = userCreateRequestDto.toAddressEntity();
			address.setUser(user);
			addressRepository.save(address);
			
			return userCreateRequestDto.name();
		} finally {
			tokenProvider.removeToken(TokenType.ACCESS_SIGNUP, accessJoinToken);
			tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);
		}
	}
	
	/** LOCAL 유저 로그인
	 * @param loginRequestDto
	 * @param ip
	 * @throws NoSuchDataException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusStayException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusStopException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusDeleteExceptions {@link UserService#getUserByUserProviderAndEmail}
	 * @throws TooManySignFailException {@link #validatePassword} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
     * @throws EncryptException {@link #handleJwt} 암호화 실패
	 * @throws CreateJwtException {@link #handleJwt} 토큰 생성 실패
	 * @throws RedisSetException {@link #handleJwt} 리프레쉬 토큰 Redis 저장 실패
	 * @return
	 */
	@Transactional
	public Map<String,String> signInLocal(SignInRequestDto loginRequestDto, String ip) {
		
		String email = loginRequestDto.email();
		String password = loginRequestDto.password();
		User user = userService.getUserByUserProviderAndEmail(UserProvider.LOCAL, email);
		validatePassword(user, password, ip);
		return handleJwt(user);
	}
	
	@Transactional
	public String signInAuthSms(String token, String phone, String name, String ip) {
		
		String value = tokenProvider.getVerificationPhone(token);
		validateByReidsValue(phone, value);
		
		// ======= 수신자에 일치하는 DB 찾기 및 해당 데이터가 3분 이내인지 검사 =======
		Verification ver = verificationService.findVerification(phone, ip);
		
		// AccessToken만 발급 30분 
		return jwtProvider.createVerifyPhoneToken(ver.getVerificationId(),Map.of("phone",phone,"name",name));
	}
	
//	@Transactional
//	public UserResponseDto getUserByAccessToken(String accessToken, String clientIp){
//		
//		// ======= 엑세스 토큰 검증 =======
//		Map<String,Object> claims = jwtTokenProvider.validateAccessToken(accessToken);
//		Object subjectRaw = claims.get("subject");
//		if (subjectRaw == null) {
//			throw new CustomJwtException("유효하지 않은 JWT 입니다. - subject");
//		}
//		int userId = Integer.parseInt(subjectRaw.toString());
//		if(userId==0) {
//			throw new CustomJwtException("유효하지 않은 JWT 입니다. - subject");
//		}
//		
//		User user = userService.getUserByUserId(userId);
//		
//		return UserResponseDto.fromEntity(user);
//	}
//	
	@Transactional
	public Map<String,String> refresh(String accessToken, String refreshToken, String clientIp){
		
		// ======= 리프레쉬 토큰 검증 =======
		int userId = validateRefreshToken(accessToken, refreshToken, clientIp);
		
		User user = userService.getUserByUserId(userId);
		
		signLogService.registerSignLog(user.getUserProvider(),user.getEmail(),clientIp,SignResult.SUCCESS);
		
		return handleJwt(user);
	}
	
	@Transactional
	public void signOutLocal(String accessToken ,String refreshToken, String clientIp) {
		// 1. accessToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getLoginJwtRemainingTime(accessToken);
			tokenProvider.addJwtBlacklist(accessToken, ttl, "logout");
			log.info("로그아웃 - accessToken 블랙리스트 등록. accessToken: {}, ip: {}", accessToken, clientIp);
		} catch (Exception e) {
			log.warn("로그아웃 - accessToken 블랙리스트 등록 실패. accessToken: {}, ip: {}, {}", accessToken, clientIp, e.getCause());
			throw e;
		}
		
		// 2. refreshToken 삭제
		String userId = jwtProvider.validateRefreshToken(refreshToken);
		if(tokenProvider.removeToken(TokenType.JWT_REFRESH, userId)) {
			log.info("로그아웃 - refreshToken 삭제. refreshToken: {}, ip: {}", refreshToken, clientIp);
		}else {
			log.warn("로그아웃 - refreshToken 삭제 실패. 도용된 RefreshToken. refreshToken: {}, ip: {}", refreshToken, clientIp);
			throw new CustomJwtException("로그아웃 - refreshToken 삭제 실패. 도용된 RefreshToken.");
		}
	}
	
	// ======= email 통해 Local.User 찾고 Status익셉션별 로그 기록 및 반환 =======
	private User findValidUser(String email, String ip) {
		try {
			return userService.getUserByUserProviderAndEmail(UserProvider.LOCAL, email);
		} catch (NoSuchDataException e) {
			signLogService.registerSignLog(UserProvider.LOCAL, email, ip, SignResult.INVALID_EMAIL);
			throw new NoSuchElementException("아이디와 비밀번호를 확인해 주세요.");
		} catch (StatusStayException e) {
			signLogService.registerSignLog(UserProvider.LOCAL, email, ip, SignResult.ACCOUNT_LOCKED);
			throw e;
		} catch (StatusStopException e) {
			signLogService.registerSignLog(UserProvider.LOCAL, email, ip, SignResult.ACCOUNT_SUSPENDED);
			throw e;
		} catch (StatusDeleteException e) {
			signLogService.registerSignLog(UserProvider.LOCAL, email, ip, SignResult.ACCOUNT_DELETED);
			throw e;
		}
	}
	
	/** 비밀번호 검증 및 결과 `LoginLog` 기록
	 * @param user
	 * @param reqPassword
	 * @param ip
	 * @throws TooManySignFailException {@link SignLogService#validSignFailCount} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 */
	private void validatePassword(User user, String reqPassword, String ip) {
		if (!passwordEncoder.matches(reqPassword, user.getPassword())) {
			signLogService.registerSignLog(UserProvider.LOCAL, user.getEmail(), ip, SignResult.INVALID_PASSWORD);
			int signFailCount;
			try {
				signFailCount = signLogService.validSignFailCount(UserProvider.LOCAL, user.getEmail());
			} catch (TooManySignFailException e) {
				user.setUserStatus(UserStatus.STAY);
				userRepository.flush();
				log.info("로그인 실패 횟수 초과로 계정 일시정지: email={}, ip={}", user.getEmail(), ip);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED,"비밀번호 불일치. 실패 횟수: "+signFailCount);
		}
		signLogService.registerSignLog(UserProvider.LOCAL, user.getEmail(), ip, SignResult.SUCCESS);
	}
	
	/** User 객체 통해 토큰 발급 및 RefreshToken은 redis 저장
	 * @param user
     * @throws EncryptException {@link JwtProvider#createLoginToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createLoginToken} 토큰 생성 실패
	 * @throws RedisSetException {@link TokenProvider#addRefreshJwt} 리프레쉬 토큰 Redis 저장 실패
	 * @return
	 */
	private Map<String, String> handleJwt(User user) {
		Map<String, String> loginToken = jwtProvider.createLoginToken(
				user.getUserId(), List.of(roles.get(user.getUserProvider())),
				Map.of("provider", user.getUserProvider().toString(), "email", user.getEmail(), "name", user.getName()));
		
		tokenProvider.addRefreshJwt(user.getUserId(), loginToken.get("refreshToken"));
		return loginToken;
	}
	
	// ======= RefreshToken 검증 =======
	private int validateRefreshToken(String accessToken, String refreshToken, String clientIp) {
		
		String userId = jwtProvider.validateRefreshToken(refreshToken);
		String value = tokenProvider.getRefreshJwt(userId);
		
		// 토큰 탈취 검증
		validateByReidsValue(refreshToken, value);
		
		return Integer.valueOf(userId);
	}
	
	// ======= AccessToekn 블랙리스트 등록, RefreshToken 삭제 , 운영자에게 안내 =======
	private void handleRefreshTokenHijack(String accessToken, String userId, String clientIp){
		
		log.warn("[탈취 의심] 잘못된 refresh 요청 - IP: {}, userId: {}, accessToken: {}", clientIp, userId, accessToken);
		
		long ttl = jwtProvider.getLoginJwtRemainingTime(accessToken); // 남은 시간 (ms)
		tokenProvider.addJwtBlacklist(accessToken, ttl, "hijack");
		
		try {
			guidanceService.sendSecurityAlert("[탈취 의심] 잘못된 refresh 요청\n" +
					"userId: " + userId + "\nIP: " + clientIp + "\nAccessToken: " + accessToken);
		} catch (Exception e) {
			log.warn("탈취 의심 문자 알림 실패: {}", e.getMessage());
		}
		
		tokenProvider.removeToken(TokenType.JWT_REFRESH, userId);
	}
	
	/**
	 * @param requestValue
	 * @param redisValue
	 * @throws TokenValueMismatchException {@link #validateByReidsValue} Redis에 저장되어있는 key의 value와 request 값 불일치.
	 */
	private void validateByReidsValue(String requestValue, String redisValue) {
		if(!Objects.equals(requestValue,redisValue)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"redis에 저장되어있는 key의 value와 request 값 불일치. 입력값: {"+requestValue+"} != 저장값: {"+redisValue+"}.");
		}
	}
}
