package net.chamman.moonnight.auth.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpTokenDto implements Encryptable<SignUpTokenDto>{
	
	private String email;
	private String rawPassword;
	public static final TokenType TOKENTYPE = TokenType.ACCESS_SIGNUP;

	@Override
	public SignUpTokenDto encrypt(AesProvider aesProvider) {
        return new SignUpTokenDto(
        		aesProvider.encrypt(this.email), 
                aesProvider.encrypt(this.rawPassword)
        );
	}

	@Override
	public SignUpTokenDto decrypt(AesProvider aesProvider) {
        return new SignUpTokenDto(
        		aesProvider.decrypt(this.email), 
                aesProvider.decrypt(this.rawPassword)
        );
	}
	
}
