package net.chamman.moonnight.auth.crypto;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_GET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_SET_FIAL;

import java.time.Duration;
import java.util.Map;
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
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
     * @throws RedisSetException {@link #createMapToken} value가 Map 형식 토큰 Redis에 저장 실패
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
	
	/**
	 * @param type
	 * @param value
	 * @throws EncryptException 암호화 실패
	 * @throws RedisSetException
	 * @return
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
	
	/**
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
	
	/**
	 * @param accessToken
	 * @param ttl
	 * @param result
	 * @throws RedisSetException
	 */
	public void addJwtBlacklist(String accessToken, long ttl, String result) {
		try {
			redisTemplate.opsForValue().set("jwt:blacklist:"+ accessToken, result, Duration.ofMillis(ttl));
		} catch (Exception e) {
			throw new RedisSetException(TOKEN_SET_FIAL,"Access Token Black List Redis 저장 중 오류",e);
		}
	}
	
	/**
	 * @param type
	 * @param token
     * @throws IllegalTokenException {@link TokenProvider#getMapTokenData}
     * @throws NoSuchTokenException {@link TokenProvider#getMapTokenData} Redis에 일치한 Key 없음
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
     * @throws RedisGetException {@link TokenProvider#getMapTokenData}
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
			
		} catch (Exception e) {
			throw new RedisGetException(TOKEN_GET_FIAL,"Redis에서 토큰값 가져오는 과정에서 오류",e);
		}
	}
	
	/**
	 * @param type
	 * @param token
	 * @throws IllegalTokenException {@link TokenProvider#getTokenData(String)} 적합하지 않은 토큰
	 * @throws NoSuchTokenException {@link TokenProvider#getTokenData(String)} 토큰을 찾지 못한 경우
     * @throws DecryptException {@link TokenProvider#getTokenData(String)} 복호화 실패 
	 * @return
	 */
	private String getTokenData(TokenType type, String token) {
	
		if (token == null || token.isBlank()) {
			throw new IllegalTokenException(TOKEN_ILLEGAL, type.name() + " Token null 또는 비어있음.");
		}
	
		String key = type.getPrefix() + token;
		String value = redisTemplate.opsForValue().get(key);
	
		if (value == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND, type.name() + " Token: " + token + " 없음.");
		}
	
		return aesProvider.decrypt(value);
	
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

}