package net.chamman.moonnight.auth.crypto;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_GET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_SET_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.TOKEN_VALUE_MISMATCH;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.dto.Encryptable;
import net.chamman.moonnight.auth.crypto.dto.FindPwTokenDto;
import net.chamman.moonnight.auth.crypto.dto.PasswordTokenDto;
import net.chamman.moonnight.auth.crypto.dto.SignUpTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationPhoneTokenDto;
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
		VERIFICATION_PHONE("verification:phone:", Duration.ofMinutes(5), VerificationPhoneTokenDto.class),
		VERIFICATION_EMAIL("verification:email:", Duration.ofMinutes(5), VerificationEmailTokenDto.class),
		ACCESS_FINDPW("access:findpw:", Duration.ofMinutes(5), FindPwTokenDto.class),
		ACCESS_PASSWORD("access:password:", Duration.ofMinutes(5), PasswordTokenDto.class),
		ACCESS_SIGNUP("access:signup:", Duration.ofMinutes(10), SignUpTokenDto.class),
		JWT_REFRESH("jwt:refresh:",Duration.ofDays(14), null);
		
		private final String prefix;
		private final Duration ttl;
		private final Class<? extends Encryptable<?>> dtoType;
		
		TokenType(String prefix, Duration ttl, Class<? extends Encryptable<?>> dtoType) {
			this.prefix = prefix;
			this.ttl = ttl;
			this.dtoType = dtoType;
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public Duration getTtl() {
			return ttl;
		}
		
	    public Class<? extends Encryptable<?>> getDtoType() {
	        return dtoType;
	    }
	}
	
	/**
	 * DTO를 페이로드로 사용하는 토큰 생성
	 * @param <T> Encryptable를 구현한 DTO 타입
	 * @param dto 토큰에 담을 데이터 DTO 객체
	 * @param tokenType 토큰 타입
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
     * @throws RedisSetException {@link #createToken} Redis 저장 실패
	 * @return
	 */
	public <T extends Encryptable<T>> String createToken(T dto, TokenType tokenType) {
		
	    if (dto.getClass() != tokenType.getDtoType()) {
	        throw new RedisSetException(TOKEN_SET_FIAL,
	            "TokenType과 Dto 타입이 일치하지 않습니다. " +
	            "입력 받은 타입: " + tokenType.getDtoType().getSimpleName() +
	            ", 입력 받은 Dto 타입: " + dto.getClass().getSimpleName()
	        );
	    }
	    
	    String token = UUID.randomUUID().toString();
	    
	    try {
	        // 1. DTO에 정의된 방식대로 암호화 수행
	    	T encryptedDto = dto.encrypt(aesProvider);
	        
	        // 2. 암호화된 DTO 객체를 JSON 문자열로 직렬화
	        String json = objectMapper.writeValueAsString(encryptedDto);
	        
	        // 3. Redis에 저장
	        redisTemplate.opsForValue().set(tokenType.getPrefix() + token, json, tokenType.getTtl());
	        
	    } catch (EncryptException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new RedisSetException(TOKEN_SET_FIAL, tokenType.name() + " Token Redis 저장 중 오류", e);
	    }
	    
	    return token;
	}
	
	
	/**
	 * @param <T> Encryptable를 구현한 DTO 타입
	 * @param tokenType 토큰 타입
	 * @param token 토큰
	 * @throws IllegalTokenException {@link #getToken} 토큰 null 또는 비어있음
	 * @throws NoSuchTokenException {@link #getToken} 일치하는 토큰 없음
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
     * @throws RedisGetException {@link #getDecryptedTokenDto} Redis 조회 실패
	 * @return
	 */
	public <T extends Encryptable<T>> T getDecryptedTokenDto(TokenType tokenType, String token) {
		
	    String json = getToken(tokenType, token);
	    
	    try {
	    	
	        @SuppressWarnings("unchecked")
			T encryptedDto = (T) objectMapper.readValue(json, tokenType.getDtoType());
	        
	        return encryptedDto.decrypt(aesProvider);
	        
	    } catch (DecryptException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new RedisGetException(TOKEN_GET_FIAL, "Redis에서 토큰 조회 중 오류", e);
	    }
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
	
	public boolean isBlackList(String accessToken) {
		return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + accessToken));
	}
	
	public boolean isValid(TokenType type, String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(type.getPrefix() + key));
	}

	public boolean removeToken(TokenType type, String key) {
		return redisTemplate.delete(type.getPrefix() + key);
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

	/**
	 * Redis에서 토큰 조회
	 * @param tokenType
	 * @param token
	 * @throws IllegalTokenException {@link #getToken} 토큰 null 또는 비어있음
	 * @throws NoSuchTokenException {@link #getToken} 일치하는 토큰 없음
	 * @return json형식 String
	 */
	private String getToken(TokenType tokenType, String token) {
		if(token == null || token.isBlank()) {
			throw new IllegalTokenException(TOKEN_ILLEGAL,tokenType.name()+" Token null 또는 비어있음.");
		}
		String key = tokenType.getPrefix() + token;
		String json = redisTemplate.opsForValue().get(key);
		
		if (json == null) {
			throw new NoSuchTokenException(TOKEN_NOT_FOUND,tokenType.name()+" 일치하는 토큰이 없음.");
		}
		return json;
	}
	

}