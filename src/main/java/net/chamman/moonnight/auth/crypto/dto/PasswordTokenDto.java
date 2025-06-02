package net.chamman.moonnight.auth.crypto.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordTokenDto implements Encryptable<PasswordTokenDto>{

	private String userId; 
	private String email;
	public static final TokenType TOKENTYPE = TokenType.ACCESS_PASSWORD;

	
	@Override
	public PasswordTokenDto encrypt(AesProvider aesProvider) {
		return new PasswordTokenDto(aesProvider.encrypt(this.userId), aesProvider.encrypt(this.email));
	}

	@Override
	public PasswordTokenDto decrypt(AesProvider aesProvider) {
		return new PasswordTokenDto(aesProvider.decrypt(this.userId), aesProvider.decrypt(this.email));
	}

	@JsonIgnore
	public int getIntUserId() {
		return Integer.parseInt(userId);
	}

	
}
