package net.chamman.moonnight.auth.sign;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED;

import java.util.List;
import java.util.Map;

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
import net.chamman.moonnight.global.exception.ExpiredException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.ParsingJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.sign.MismatchPasswordException;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;

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
	private final Map<UserProvider,String> roles = Map.of(UserProvider.LOCAL,"ROLE_LOCAL",UserProvider.KAKAO,"ROLE_OAUTH",UserProvider.NAVER,"ROLE_OAUTH");
	

	/** 회원가입 1차
	 * @param email
	 * @param password
	 * @param valificationEmailToken
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#validVerificationEmail} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#validVerificationEmail} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#validVerificationEmail} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#validVerificationEmail} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link TokenProvider#validVerificationEmail} 토큰 값과 email 비교 불일치
	 * 
	 * @throws DuplicationException {@link UserService#isEmailExists} 이메일 중복
	 * 
     * @throws EncryptException {@link TokenProvider#createMapToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#removeToken} Redis 저장 실패
     * 
	 * @return
	 */
	@Transactional
	public String createJoinToken(String email, String password, String valificationEmailToken) {
		
		try {
			tokenProvider.validVerificationEmail(email, valificationEmailToken);
			
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
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#validVerificationPhone} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#validVerificationPhone} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#validVerificationPhone} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#validVerificationPhone} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link TokenProvider#validVerificationPhone} 토큰 값과 phone 비교 불일치
	 * 
	 * @throws DuplicationException {@link UserService#isPhoneExists} 휴대폰 중복
	 * 
	 * @return 회원 가입 유저 이름
	 */
	@Transactional
	public String signUpLocalUser(UserCreateRequestDto userCreateRequestDto, String accessJoinToken, String verificationPhoneToken) {
		
		Map<String,String> mapValue = tokenProvider.getMapTokenData(TokenType.ACCESS_SIGNUP, accessJoinToken);
		String reqPhone = userCreateRequestDto.phone();
		
		try {
			tokenProvider.validVerificationPhone(reqPhone, verificationPhoneToken);
			
			userService.isPhoneExists(UserProvider.LOCAL, reqPhone);
			
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
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusStayException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusStopException {@link UserService#getUserByUserProviderAndEmail}
	 * @throws StatusDeleteExceptions {@link UserService#getUserByUserProviderAndEmail}
	 * 
	 * @throws TooManySignFailException {@link #validatePassword} 비밀번호 실패 횟수 초과
	 * @throws MismatchPasswordException {@link #validatePassword} 비밀번호 불일치
	 * 
     * @throws EncryptException {@link #handleJwt} 암호화 실패
	 * @throws CreateJwtException {@link #handleJwt} 토큰 생성 실패
	 * @throws RedisSetException {@link #handleJwt} 리프레쉬 토큰 Redis 저장 실패
	 * 
	 * @return 로그인 토큰들
	 */
	@Transactional
	public Map<String,String> signInLocal(SignInRequestDto loginRequestDto, String ip) {
		
		String email = loginRequestDto.email();
		String password = loginRequestDto.password();
		
		User user = userService.getUserByUserProviderAndEmail(UserProvider.LOCAL, email);
		
		validatePassword(user, password, ip);
		
		return handleJwt(user);
	}
	
	/** 휴대폰 인증 로그인
	 * @param token
	 * @param phone
	 * @param name
	 * @param ip
	 * 
	 * @throws IllegalTokenException {@link TokenProvider#validVerificationPhone} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#validVerificationPhone} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#validVerificationPhone} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#validVerificationPhone} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link TokenProvider#validVerificationPhone} 토큰 값과 phone 비교 불일치
	 * 
	 * @throws NoSuchDataException {@link VerificationService#findVerification}  최근 인증 요청 없음
	 * @throws ExpiredException {@link VerificationService#findVerification}  인증 시간 초과
	 * 
     * @throws EncryptException {@link JwtProvider#createVerifyPhoneToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createVerifyPhoneToken} 토큰 생성 실패
	 * 
	 * @return 휴대폰 인증 로그인 토큰
	 */
	@Transactional
	public String signInAuthSms(String token, String phone, String name, String ip) {
		
		tokenProvider.validVerificationPhone(phone, token);
		
		// ======= 수신자에 일치하는 DB 찾기 및 해당 데이터가 3분 이내인지 검사 =======
		Verification ver = verificationService.findVerification(phone, ip);
		
		String verifyPhoneToken = jwtProvider.createVerifyPhoneToken(ver.getVerificationId(),Map.of("phone",phone,"name",name));
		
		signLogService.registerSignLog(null, verifyPhoneToken, ip, SignResult.AUTH_SUCCESS);
		
		return verifyPhoneToken;
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
	
	/** accessToken의 기한 만료로 토큰 재발급
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws IllegalJwtException {@link #refresh} 액세스 토큰 블랙리스트
	 * 
	 * @throws TimeOutJwtException {@link JwtProvider#validateRefreshToken} 시간 초과
     * @throws DecryptException {@link JwtProvider#validateRefreshToken} 복호화 실패
	 * @throws ParsingJwtException {@link JwtProvider#validateRefreshToken} JWT 파싱 실패
	 * 
	 * @throws NoSuchTokenException {@link TokenProvider#validRefreshToken} Redis에 없는 키
	 * @throws TokenValueMismatchException {@link TokenProvider#validRefreshToken} Redis 값과 비교 불일치
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteExceptions {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
     * @throws EncryptException {@link #handleJwt} 암호화 실패
	 * @throws CreateJwtException {@link #handleJwt} 토큰 생성 실패
	 * @throws RedisSetException {@link #handleJwt} 리프레쉬 토큰 Redis 저장 실패
	 * 
	 * @return 로그인 토큰들
	 */
	@Transactional
	public Map<String,String> refresh(String accessToken, String refreshToken, String clientIp){
		
		boolean isBlackList = tokenProvider.isBlackList(accessToken);
		if(isBlackList) {
			throw new IllegalJwtException(JWT_ILLEGAL,"액세스 토큰 블랙리스트.");
		}
		try {
//			리프레쉬 토큰 검증
			String userIdStr = jwtProvider.validateRefreshToken(refreshToken);
			tokenProvider.validRefreshToken(userIdStr, refreshToken);
			
			int userId = Integer.parseInt(userIdStr);
			User user = userService.getUserByUserId(userId);
			
			signLogService.registerSignLog(user.getUserProvider(), user.getEmail(), clientIp, SignResult.REFRESH);
			
//			새로운 토큰 발급
			Map<String, String> signTokens = handleJwt(user);
			
//			기존 리프레쉬 토큰 Redis에서 삭제
			tokenProvider.removeToken(TokenType.JWT_REFRESH, userIdStr);
			
			return signTokens;
		} catch (Exception e) {
			signLogService.registerSignLog(clientIp, SignResult.REFRESH_FAIL);
			throw e;
		}
	}
	
	/** 로그아웃
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} 시간 초과
	 * @throws ParsingJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} JWT 파싱 실패
	 * 
	 * @throws RedisSetException {@link TokenProvider#addAccessJwtBlacklist} Redis 저장 중 오류
	 * 
     * @throws DecryptException {@link JwtProvider#validateRefreshToken} 복호화 실패
     * 
     * @throws IllegalJwtException(@link #signOutLocal)
	 */
	@Transactional
	public void signOut(String accessToken ,String refreshToken, String clientIp) {
		
//		1. accessToken 블랙리스트 등록
		long ttl = jwtProvider.getSignJwtRemainingTime(accessToken);
		tokenProvider.addAccessJwtBlacklist(accessToken, ttl, "logout");
		log.info("로그아웃 - accessToken 블랙리스트 등록. accessToken: {}, ip: {}", accessToken, clientIp);
			
//		2. refreshToken 삭제
		String userId = jwtProvider.validateRefreshToken(refreshToken);
		if(!(tokenProvider.removeToken(TokenType.JWT_REFRESH, userId))) {
			log.warn("로그아웃 - refreshToken 삭제 실패. 도용된 RefreshToken. userId: {}, refreshToken: {}, ip: {}", userId, refreshToken, clientIp);
			throw new IllegalJwtException(JWT_ILLEGAL,"로그아웃 - refreshToken 삭제 실패. 도용된 RefreshToken.");
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
				userRepository.save(user);
				log.info("로그인 실패 횟수 초과로 계정 일시정지: email={}, ip={}", user.getEmail(), ip);
				throw e;
			}
			throw new MismatchPasswordException(SIGNIN_FAILED,"비밀번호 불일치. 실패 횟수: "+signFailCount);
		}
		signLogService.registerSignLog(UserProvider.LOCAL, user.getEmail(), ip, SignResult.LOCAL_SUCCESS);
	}
	
	/** User 정보 통해 로그인 토큰들 발급 및 RefreshToken은 Redis 저장
	 * @param user
	 * 
     * @throws EncryptException {@link JwtProvider#createSignToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createSignToken} 토큰 생성 실패
	 * 
	 * @throws RedisSetException {@link TokenProvider#addRefreshJwt} 리프레쉬 토큰 Redis 저장 실패
	 * @return 로그인 토큰들
	 */
	private Map<String, String> handleJwt(User user) {
		Map<String, String> signToken = jwtProvider.createSignToken(
				user.getUserId(), List.of(roles.get(user.getUserProvider())),
				Map.of("provider", user.getUserProvider().toString(), "email", user.getEmail(), "name", user.getName()));
		
		tokenProvider.addRefreshJwt(user.getUserId(), signToken.get("refreshToken"));
		return signToken;
	}
	
}
