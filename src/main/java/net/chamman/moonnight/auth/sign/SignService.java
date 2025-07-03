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
import net.chamman.moonnight.auth.crypto.dto.SignUpTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.auth.sign.log.SignLogService;
import net.chamman.moonnight.auth.verification.VerificationService;
import net.chamman.moonnight.domain.address.Address;
import net.chamman.moonnight.domain.address.AddressRepository;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.UserRepository;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.DuplicationException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.IllegalJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.jwt.ValidateJwtException;
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
public class SignService {

	private final UserService userService;
	private final VerificationService verificationService;
	private final AddressRepository addressRepository;
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
	 * @throws IllegalTokenException {@link TokenProvider#getDecryptedTokenDto} 토큰 문자열 null 또는 비어있음
	 * @throws NoSuchTokenException {@link TokenProvider#getDecryptedTokenDto} Redis 일치하는 토큰 없음
     * @throws DecryptException {@link TokenProvider#getDecryptedTokenDto} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getDecryptedTokenDto} Redis 조회 실패
     * 
	 * @throws NoSuchDataException {@link VerificationService#isVerify} DB verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link VerificationService#isVerify} DB 미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException {@link VerificationService#isVerify} DB 미인증된 인증 요청.
	 * 
	 * @throws DuplicationException {@link UserService#isEmailExists} 이메일 중복
	 * 
     * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
     * 
	 * @return
	 */
	@Transactional
	public String createSignUpToken(String email, String password, String valificationEmailToken) {
		log.debug("*회원 가입 1차 요청. email: [{}] with verificationEmailToken: [{}]",
				LogMaskingUtil.maskEmail(email, MaskLevel.MEDIUM),
				LogMaskingUtil.maskToken(valificationEmailToken, MaskLevel.MEDIUM));

		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, valificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());
		
		userService.isEmailExists(UserProvider.LOCAL, email);
		
		String token = tokenProvider.createToken(new SignUpTokenDto(email, password), SignUpTokenDto.TOKENTYPE);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, valificationEmailToken);
		
		return token; 
	}
	
	/** 회원가입 2차
	 * @param userCreateRequestDto
	 * @param accessJoinToken 회원가입 1차 토큰
	 * @param verificationPhoneToken 휴대폰 인증 토큰
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
	 * @throws DuplicationException {@link UserService#isPhoneExists} 휴대폰 중복
	 * 
	 * @return 회원 가입 유저 이름
	 */
	@Transactional
	public String signUpLocalUser(SignUpRequestDto signUpRequestDto, String accessSignUpToken, String verificationPhoneToken) {
		
		SignUpTokenDto signUpTokenDto = tokenProvider.getDecryptedTokenDto(SignUpTokenDto.TOKENTYPE, accessSignUpToken);
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		userService.isPhoneExists(UserProvider.LOCAL, verificationPhoneTokenDto.getPhone());
		
		// 비밀번호 인코딩 후 저장
		String encodedPassoword = passwordEncoder.encode(signUpTokenDto.getRawPassword());
		
		User user = signUpRequestDto.toEntity();
		user.setEmail(signUpTokenDto.getEmail());
		user.setPassword(encodedPassoword);
		user.setPhone(verificationPhoneTokenDto.getPhone());
		userRepository.save(user);
		
		Address address = signUpRequestDto.toAddressEntity();
		address.setUser(user);
		addressRepository.save(address);
		
		tokenProvider.removeToken(SignUpTokenDto.TOKENTYPE, accessSignUpToken);
		tokenProvider.removeToken(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		
		return signUpRequestDto.name();
	}
	
	/** LOCAL 유저 로그인
	 * @param loginRequestDto
	 * @param ip
	 * @return 로그인 토큰들
	 * 
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
	 * @throws NoSuchDataException {@link #signInLocal} 일치하는 유저 없음
	 */
	@Transactional
	public Map<String,String> signInLocal(SignInRequestDto loginRequestDto, String ip) {
		
		String email = loginRequestDto.email();
		String password = loginRequestDto.password();
		
		try {
			User user = userService.getUserByUserProviderAndEmail(UserProvider.LOCAL, email);
			
			userService.validatePassword(user, password, ip);
			
			return handleJwt(user);
		} catch (NoSuchDataException e) {
			throw new NoSuchDataException(SIGNIN_FAILED,"일치하는 계정 없음",e);
		}
	}
	
	/** 휴대폰 인증 토큰 통해 auth jwt 발행
	 * @param verificationPhoneToken
	 * @param ip
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
     * @throws EncryptException {@link JwtProvider#createAuthToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createAuthToken} 토큰 생성 실패
	 * 
	 * @return auth 로그인 토큰
	 */
	public String signInAuthBySms(String verificationPhoneToken, String ip) {
		
		VerificationPhoneTokenDto verificationPhoneTokenDto = tokenProvider.getDecryptedTokenDto(VerificationPhoneTokenDto.TOKENTYPE, verificationPhoneToken);
		verificationService.isVerify(verificationPhoneTokenDto.getIntVerificationId());

		String authTokenDto = jwtProvider.createAuthToken(
				verificationPhoneTokenDto.getVerificationId(),
				verificationPhoneTokenDto.getPhone());
		
		signLogService.registerSignLog(null, verificationPhoneTokenDto.getPhone(), ip, SignResult.AUTH_SUCCESS);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_PHONE, verificationPhoneToken);

		return authTokenDto;
	}
	
	/** 이메일 인증 토큰 통해 auth jwt 발행
	 * @param verificationEmailToken
	 * @param ip
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
     * @throws EncryptException {@link JwtProvider#createAuthToken} 암호화 실패
	 * @throws CreateJwtException {@link JwtProvider#createAuthToken} 토큰 생성 실패
	 * 
	 * @return auth jwt
	 */
	public String signInAuthByEmail(String verificationEmailToken, String ip) {
		
		VerificationEmailTokenDto verificationEmailTokenDto = tokenProvider.getDecryptedTokenDto(VerificationEmailTokenDto.TOKENTYPE, verificationEmailToken);
		verificationService.isVerify(verificationEmailTokenDto.getIntVerificationId());

		String authTokenDto = jwtProvider.createAuthToken(
				verificationEmailTokenDto.getVerificationId(),
				verificationEmailTokenDto.getEmail());
		
		signLogService.registerSignLog(null, verificationEmailTokenDto.getEmail(), ip, SignResult.AUTH_SUCCESS);
		
		tokenProvider.removeToken(TokenType.VERIFICATION_EMAIL, verificationEmailToken);

		return authTokenDto;
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
	 * @throws TimeOutJwtException {@link JwtProvider#validateRefreshToken} 시간 초과
     * @throws DecryptException {@link JwtProvider#validateRefreshToken} 복호화 실패
	 * @throws ValidateJwtException {@link JwtProvider#validateRefreshToken} JWT 파싱 실패
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
	public Map<String,String> refresh(String refreshToken, String clientIp){
		try {
//			리프레쉬 토큰 검증
			String userIdStr = jwtProvider.validateRefreshToken(refreshToken);
			tokenProvider.validRefreshToken(userIdStr, refreshToken);
			
			int userId = Integer.parseInt(userIdStr);
			User user = userService.getUserByUserId(userId);
			
			signLogService.registerSignLog(user.getUserProvider(), user.getEmail(), clientIp, SignResult.REFRESH);
			
//			새로운 토큰 발급
			Map<String, String> signTokens = handleJwt(user);
			
			return signTokens;
		} catch (Exception e) {
			signLogService.registerSignLog(clientIp, SignResult.REFRESH_FAIL);
			throw e;
		}
	}
	
	/** LOCAL 로그아웃
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} 시간 초과
	 * @throws ValidateJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} JWT 파싱 실패
	 * 
	 * @throws RedisSetException {@link TokenProvider#addAccessJwtBlacklist} Redis 저장 중 오류
	 * 
     * @throws DecryptException {@link JwtProvider#validateRefreshToken} 복호화 실패
     * 
     * @throws IllegalJwtException(@link #signOutLocal)
	 */
	@Transactional
	public void signOut(String accessToken ,String refreshToken, String clientIp) {
		log.debug("* LOCAL 로그아웃 요청.");
		
//		1. accessToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getAccessTokenRemainingTime(accessToken);
			tokenProvider.addTokenBlacklist(accessToken, ttl, "SIGNOUT");
		} catch (TimeOutJwtException e) {
			log.debug("* 이미 만료된 AT.");
		}
			
//		2. refreshToken 삭제
		String userId = jwtProvider.validateRefreshToken(refreshToken);
		if(!(tokenProvider.removeToken(TokenType.JWT_REFRESH, userId))) {
			log.warn("* 로그아웃 중 RefreshToken 삭제 실패. 도용된 RefreshToken.userId: [{}], RefreshToken: [{}], RequestIp: [{}]", userId, refreshToken, clientIp);
			throw new IllegalJwtException(JWT_ILLEGAL,"* 로그아웃 - refreshToken 삭제 실패. 도용된 RefreshToken.");
		}
	}
	
	/** AUTH 로그아웃
	 * @param accessToken
	 * @param refreshToken
	 * @param clientIp
	 * 
	 * @throws TimeOutJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} 시간 초과
	 * @throws ValidateJwtException {@link JwtProvider#getSignJwtRemainingTime}, {@link JwtProvider#validateRefreshToken} JWT 파싱 실패
	 * 
	 * @throws RedisSetException {@link TokenProvider#addAccessJwtBlacklist} Redis 저장 중 오류
	 * 
     * @throws DecryptException {@link JwtProvider#validateRefreshToken} 복호화 실패
     * 
     * @throws IllegalJwtException(@link #signOutLocal)
	 */
	@Transactional
	public void signOutAuth(String authToken, String clientIp) {
		log.debug("* AUTH 로그아웃 요청.");
		
//		authToken 블랙리스트 등록
		try {
			long ttl = jwtProvider.getAuthTokenRemainingTime(authToken);
			tokenProvider.addTokenBlacklist(authToken, ttl, "SIGNOUT");
		} catch (TimeOutJwtException e) {
			log.debug("* 이미 만료된 AuthToken.");
		}
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
