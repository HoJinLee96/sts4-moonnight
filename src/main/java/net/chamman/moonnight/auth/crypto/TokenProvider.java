package net.chamman.moonnight.auth.crypto;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_GET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_SET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_VALUE_MISMATCH;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.redis.RedisGetException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.token.IllegalTokenException;
import net.chamman.moonnight.global.exception.token.NoSuchTokenException;
import net.chamman.moonnight.global.exception.token.TokenValueMismatchException;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenProvider {
	
	private final AesProvider aesProvider;
	private final RedisTemplate<String, String> redisTemplate;
	private ObjectMapper objectMapper = new ObjectMapper();
	
	public enum TokenType {
		VERIFICATION_EMAIL("verification:email:", Duration.ofMinutes(5)),
		VERIFICATION_PHONE("verification:phone:", Duration.ofMinutes(5)),
		ACCESS_FINDPW("access:findpw:", Duration.ofMinutes(10)),
		ACCESS_PASSWORD("access:password:", Duration.ofMinutes(10)),
		ACCESS_SIGNUP("access:signup:", Duration.ofMinutes(20)),
		JWT_REFRESH("jwt:refresh:",Duration.ofDays(14));
		
		private final String prefix;
		private final Duration ttl;
		
		TokenType(String prefix, Duration ttl) {
			this.prefix = prefix;
			this.ttl = ttl;
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public Duration getTtl() {
			return ttl;
		}
	}
	
	/** value가 Map 형식 토큰 생성
	 * @param type
	 * @param map
	 * 
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
     * @throws RedisSetException {@link #createMapToken} value가 Map 형식 토큰 Redis에 저장 실패
     * 
	 * @return 토큰
	 */
	public String createMapToken(TokenType type, Map<String, String> map) {
		String token = UUID.randomUUID().toString();
		try {
			// 각 value에 AES 암호화 적용
			Map<String, String> encryptedMap = map.entrySet().stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							entry -> aesProvider.encrypt(entry.getValue())
							));
			
			String json = objectMapper.writeValueAsString(encryptedMap);
			redisTemplate.opsForValue().set(type.getPrefix() + token, json, type.getTtl());
			
		} catch(EncryptException e) {
			throw e;
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL,type.name()+" Token Redis 저장 중 오류",e);
		}
		
		return token;
	}
	
	/** 토큰 생성
	 * @param type
	 * @param value
	 * 
	 * @throws EncryptException 암호화 실패
	 * @throws RedisSetException
	 * 
	 * @return 토큰
	 */
	private String createToken(TokenType type, String value) {
		try {
			String token = UUID.randomUUID().toString();
			redisTemplate.opsForValue().set(type.getPrefix() + token, aesProvider.encrypt(value), type.getTtl());
			return token;
			
		} catch(EncryptException e) {
			throw e;
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL,type.name()+" Token Redis 저장 중 오류",e);
		}
	}

	public String createVerificationEmailToken(String email) {
		return createToken(TokenType.VERIFICATION_EMAIL, email);
	}
	
	public String createVerificationPhoneToken(String phone) {
		return createToken(TokenType.VERIFICATION_PHONE, phone);
	}
	
	public String createAccessFindPwToken(String email) {
		return createToken(TokenType.ACCESS_FINDPW, email);
	}
	
	public String createAccessPaaswordToken(String email) {
		return createToken(TokenType.ACCESS_PASSWORD, email);
	}
	
	/** 리프레쉬토큰 Redis set
	 * @param userId
	 * @param refreshToken
	 * @throws RedisSetException {@link #addRefreshJwt} 리프레쉬 토큰 Redis 저장 실패
	 */
	public void addRefreshJwt(int userId, String refreshToken) {
		try {
			redisTemplate.opsForValue().set(TokenType.JWT_REFRESH.getPrefix() + userId, refreshToken, TokenType.JWT_REFRESH.getTtl());
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL,"Refresh Token Redis 저장 중 오류",e);
		}
	}
	
	/** 액세스토큰 블랙리스트 Redis set
	 * @param accessToken
	 * @param ttl
	 * @param result
	 * @throws RedisSetException {@link #addAccessJwtBlacklist} Redis 저장 중 오류
	 */
	public void addAccessJwtBlacklist(String accessToken, long ttl, String result) {
		try {
			redisTemplate.opsForValue().set("jwt:blacklist:"+ accessToken, result, Duration.ofMillis(ttl));
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL,"Access Token BlackList Redis 저장 중 오류",e);
		}
	}
	
	/** value가 Map 형식 토큰 Get
	 * @param type
	 * @param token
     * @throws IllegalTokenException {@link TokenProvider#getMapTokenData}
     * @throws NoSuchTokenException {@link TokenProvider#getMapTokenData} Redis에 일치한 Key 없음
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getMapTokenData} Redis에서 토큰 조회 실패
	 * @return
	 */
	public Map<String, String> getMapTokenData(TokenType type, String token) {
		if(token == null || token.isBlank()) {
			throw new IllegalTokenException(TOKEN_ILLEGAL,type.name()+" Token null 또는 비어있음.");
		}
		String key = type.getPrefix() + token;
		String json = redisTemplate.opsForValue().get(key);
		
		if (json == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND,type.name()+" 일치하는 토큰이 없음.");
		}
		
		try {
			Map<String, String> encryptedMap = objectMapper.readValue(json, new TypeReference<>() {});
			
			// value 복호화 적용
			return encryptedMap.entrySet().stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							entry -> aesProvider.decrypt(entry.getValue())
							));
			
		} catch (DecryptException e) {
			throw e;
		} catch (Exception e) {
			throw new RedisGetException(TOKEN_GET_FIAL,"Redis에서 토큰값 가져오는 과정에서 오류",e);
		}
	}
	
	/** 토큰 Get
	 * @param type
	 * @param token
	 * @throws IllegalTokenException {@link TokenProvider#getTokenData} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getTokenData} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getMapTokenData} Redis에서 토큰 조회 실패
	 * @return
	 */
	private String getTokenData(TokenType type, String key) {
	
		if (key == null || key.isBlank()) {
			throw new IllegalTokenException(TOKEN_ILLEGAL, type.name() + " Key null 또는 비어있음.");
		}
	
		String path = type.getPrefix() + key;
		String value = redisTemplate.opsForValue().get(path);
	
		if (value == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND, type.name() + " Key: " + key + " 없음.");
		}
		try {
			return aesProvider.decrypt(value);
		} catch (DecryptException e) {
			throw e;
		} catch (Exception e) {
			throw new RedisGetException(TOKEN_GET_FIAL,"Redis에서 가져오는 과정에서 오류",e);
		}
	
	}

	public String getVerificationPhone(String token) {
		return getTokenData(TokenType.VERIFICATION_PHONE, token);
	}
	
	public String getVerificationEmail(String token) {
		return getTokenData(TokenType.VERIFICATION_EMAIL, token);
	}
	
	public String getAccessFindpwToken(String token) {
		return getTokenData(TokenType.ACCESS_FINDPW, token);
	}
	
	public String getAccessPasswordToken(String token) {
		return getTokenData(TokenType.ACCESS_PASSWORD, token);
	}
	
	public String getRefreshJwt(String token) {
		return getTokenData(TokenType.JWT_REFRESH, token);
	}
	
	public boolean isBlackList(String accessToken) {
		return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + accessToken));
	}
	
	public boolean isValid(TokenType type, String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(type.getPrefix() + key));
	}

	public boolean removeToken(TokenType type, String key) {
		return redisTemplate.delete(type.getPrefix() + key);
	}
	
	/** 휴대폰 인증 토큰 Get, value 비교
	 * @param reqPhone
	 * @param token
	 * @throws IllegalTokenException {@link #getTokenData} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link #getTokenData} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link #getTokenData} 복호화 실패
     * @throws RedisGetException {@link #getTokenData} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link #validVerificationPhone} 토큰 값과 phone 비교 불일치
	 * @return 일치 여부
	 */
	public boolean validVerificationPhone(String reqPhone, String token) {
		String value = getTokenData(TokenType.VERIFICATION_PHONE, token);
		if(!Objects.equals(reqPhone,value)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"Redis 값과 비교 불일치. reqPhone: "+reqPhone+", value: "+value);
		}
		return true;
	}
	
	/** 이메일 인증 토큰 Get, value 비교
	 * @param reqEmail
	 * @param token
	 * @throws IllegalTokenException {@link #getTokenData} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link #getTokenData} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link #getTokenData} 복호화 실패
     * @throws RedisGetException {@link #getTokenData} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link #validVerificationEmail} 토큰 값과 email 비교 불일치
	 * @return 일치 여부
	 */
	public boolean validVerificationEmail(String reqEmail, String token) {
		String value = getTokenData(TokenType.VERIFICATION_EMAIL, token);
		if(!Objects.equals(reqEmail,value)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"Redis 값과 비교 불일치. reqEmail: "+reqEmail+", value: "+value);
		}
		return true;
	}
	
	/** 비밀번호 찾기 토큰 Get, value 비교
	 * @param reqEmail
	 * @param token
	 * @throws IllegalTokenException {@link #getTokenData} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link #getTokenData} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link #getTokenData} 복호화 실패
     * @throws RedisGetException {@link #getTokenData} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link #validAccessFindpwToken} 토큰 값과 email 비교 불일치
	 * @return 일치 여부
	 */
	public boolean validAccessFindpwToken(String reqEmail, String token) {
		String value = getTokenData(TokenType.ACCESS_FINDPW, token);
		if(!Objects.equals(reqEmail,value)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"Redis 값과 비교 불일치. reqEmail: "+reqEmail+", value: "+value);
		}
		return true;
	}
	
	/** 비밀번호 검증 토큰 Get, value 비교
	 * @param reqEmail
	 * @param token
	 * @throws IllegalTokenException {@link #getTokenData} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link #getTokenData} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link #getTokenData} 복호화 실패
     * @throws RedisGetException {@link #getTokenData} Redis에서 토큰 조회 실패
	 * @throws TokenValueMismatchException {@link #validAccessPasswordToken} 토큰 값과 email 비교 불일치
	 * @return 일치 여부
	 */
	public boolean validAccessPasswordToken(String reqEmail, String token) {
		String value = getTokenData(TokenType.ACCESS_PASSWORD, token);
		if(!Objects.equals(reqEmail,value)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"Redis 값과 비교 불일치. reqEmail: "+reqEmail+", value: "+value);
		}
		return true;
	}
	
	/** 리프레쉬 토큰 Get, value 비교
	 * @param reqUserId
	 * @param token
	 * @throws NoSuchTokenException {@link #getTokenData} Redis에 없는 키
	 * @throws TokenValueMismatchException {@link #validRefreshToken} Redis 값과 비교 불일치
	 * @return 일치 여부
	 */
	public boolean validRefreshToken(String userId, String reqRefreshToken) {
		String path = TokenType.JWT_REFRESH.getPrefix()+userId;
		String refreshToken = redisTemplate.opsForValue().get(path);
		if (refreshToken == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND, userId+"에 일치하는 리프레쉬 토큰 없음.");
		}
		if(!Objects.equals(reqRefreshToken,refreshToken)) {
			throw new TokenValueMismatchException(TOKEN_VALUE_MISMATCH,"Redis 값과 비교 불일치. reqRefreshToken: "+reqRefreshToken+", refreshToken: "+refreshToken);
		}
		return true;
	}
	

}