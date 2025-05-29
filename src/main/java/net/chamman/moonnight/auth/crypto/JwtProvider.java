package net.chamman.moonnight.auth.crypto;

import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_CREATE_FIAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_EXPIRED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_ILLEGAL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.JWT_VALIDATE_FIAL;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.crypto.DecryptException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.jwt.CreateJwtException;
import net.chamman.moonnight.global.exception.jwt.TimeOutJwtException;
import net.chamman.moonnight.global.exception.jwt.ValidateJwtException;

@Component
@Slf4j
@PropertySource("classpath:application.properties")
public class JwtProvider {
	
	private final AesProvider aesProvider;
	private final Key signHmacShaKey;
	
	private final long expiration14Days = 1000 * 60 * 60 * 24 * 14; // 14
	private final long expiration1Hour = 1000 * 60 * 60; // 1시간
	private final long expiration30Minute = 1000 * 60 * 30; // 30분
	
	public JwtProvider(
			@Autowired AesProvider aesProvider,
			@Value("${jwt.sign.secretKey}") String signSecretKey
			) {
		this.aesProvider = aesProvider;
		this.signHmacShaKey = Keys.hmacShaKeyFor(signSecretKey.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * @param userId
	 * @param roles
	 * @param claims
     * @throws EncryptException {@link #createAccessToken}, {@link #createRefreshToken} 암호화 실패
	 * @throws CreateJwtException {@link #createAccessToken}, {@link #createRefreshToken} 토큰 생성 실패
	 * @return 액세스토큰, 리프레쉬토큰
	 */
	public Map<String,String> createSignToken(int userId, List<String> roles, Map<String, Object> claims) {
		String accessToken = createAccessToken(userId, roles, claims);
		String refreshToken = createRefreshToken(userId);
		return Map.of("accessToken",accessToken,"refreshToken",refreshToken);
	}
	
	/** 액세스 토큰 생성
	 * @param userId
	 * @param roles
	 * @param claims
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
	 * @throws CreateJwtException {@link #createAccessToken} 토큰 생성 실패
	 * @return 액세스 토큰
	 */
	public String createAccessToken(int userId, List<String> roles, Map<String, Object> claims) {
		try {
			JwtBuilder builder = Jwts.builder()
					.setSubject(aesProvider.encrypt(userId + ""))
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + expiration1Hour))
					.signWith(signHmacShaKey, SignatureAlgorithm.HS256);
			
			if (claims != null) {
				for (Map.Entry<String, Object> entry : claims.entrySet()) {
					Object value = entry.getValue();
					if (value instanceof String strVal) {
						builder.claim(entry.getKey(), aesProvider.encrypt(strVal));
					} else {
						builder.claim(entry.getKey(), value); // 예외 방지
					}
				}
			}
			builder.claim("roles",roles);
			return builder.compact();
		} catch (EncryptException e) {
			throw e;
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"AccessToken 생성 실패.",e);
		}
	}
	
	/** 리프레쉬 토큰 생성
	 * @param userId
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
	 * @throws CreateJwtException {@link #createRefreshToken} 토큰 생성 실패
	 * @return 리프레쉬 토큰
	 */
	public String createRefreshToken(int userId) {
		try {
			return Jwts.builder()
					.setSubject(aesProvider.encrypt(userId + ""))
					.setExpiration(new Date(System.currentTimeMillis() + expiration14Days))
					.signWith(signHmacShaKey, SignatureAlgorithm.HS256)
					.compact();
		} catch (EncryptException e) {
			throw e;
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"RefreshToken 생성 실패.",e);
		}
	}
	
	/** 휴대폰 인증 로그인 토큰 생성
	 * @param verificationId
	 * @param claims
     * @throws EncryptException {@link AesProvider#encrypt} 암호화 실패
	 * @throws CreateJwtException {@link #createVerifyPhoneToken} 토큰 생성 실패
	 * @return 휴대폰 인증 로그인 토큰
	 */
	public String createVerifyPhoneToken(String verificationId, String phone) {
		try {
			
			return Jwts.builder()
					.setSubject(aesProvider.encrypt(verificationId)) 
					.setIssuedAt(new Date())
					.setExpiration(new Date(System.currentTimeMillis() + expiration30Minute))
					.signWith(signHmacShaKey, SignatureAlgorithm.HS256)
					.claim("phone", phone)
					.claim("roles",List.of("RULE_AUTH"))
					.compact();
			
		} catch (EncryptException e) {
			throw e;
		} catch (Exception e) {
			throw new CreateJwtException(JWT_CREATE_FIAL,"VerifyPhoneToken 생성 실패.",e);
		}
	}
	
	
	/** 액세스 토큰 검증
	 * @param token
	 * @throws TimeOutJwtException {@link #validateAccessToken} 시간 초과
     * @throws DecryptException {@link #getDecryptedClaims} 복호화 실패
	 * @throws ValidateJwtException {@link #validateAccessToken} JWT 파싱 실패
	 * @return 복호화된 유저 정보
	 */
	public Map<String, Object> validateAccessToken(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			return getDecryptedClaims(claims);
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED, "Access Token 시간 만료.", e);
		} catch (DecryptException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL, "Access Token 검증 중 익셉션 발생.", e);
		}
	}
	
	/** 리프레쉬 토큰 검증
	 * @param token
	 * @throws TimeOutJwtException {@link #validateRefreshToken} 시간 초과
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
	 * @throws ValidateJwtException {@link #validateRefreshToken} JWT 파싱 실패
	 * @return userId
	 */
	public String validateRefreshToken(String token) {
		
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			String encryptedUserId = claims.getSubject(); 
			return aesProvider.decrypt(encryptedUserId);
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED, "Refresh Token 시간 만료.", e);
		} catch (DecryptException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL, "Refresh Token 검증 중 익셉션 발생.", e);
		}
	}
	
	/** 토큰 남은 시간 조회
	 * @param token
	 * @throws TimeOutJwtException {@link #getSignJwtRemainingTime} 시간 초과
	 * @throws ValidateJwtException {@link #getSignJwtRemainingTime} JWT 파싱 실패
	 * @return 토큰 남은시간 
	 */
	public long getSignJwtRemainingTime(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(signHmacShaKey)
					.build()
					.parseClaimsJws(token)
					.getBody();
			
			Date expiration = claims.getExpiration();
			return expiration.getTime() - System.currentTimeMillis();
		} catch (ExpiredJwtException e) {
			throw new TimeOutJwtException(JWT_EXPIRED,"토큰 만료",e);
		} catch (Exception e) {
			throw new ValidateJwtException(JWT_VALIDATE_FIAL,"토큰 검증 중 익셉션 발생.",e);
		}
	}
	
	
	/** Claims 복호화
	 * @param claims
     * @throws DecryptException {@link AesProvider#decrypt} 복호화 실패
	 * @return Claims
	 */
	@SuppressWarnings("unused")
	private Map<String,Object> getDecryptedClaims(Claims claims) {
		Map<String, Object> result = new HashMap<>();
		result.put("subject",aesProvider.decrypt(claims.getSubject()));
		claims.forEach((k, v) -> {
			if (v instanceof String strVal) {
				result.put(k, aesProvider.decrypt((String)v));
			} else {
				result.put(k, v);
			}
		});
		return result;
	}
	
}
