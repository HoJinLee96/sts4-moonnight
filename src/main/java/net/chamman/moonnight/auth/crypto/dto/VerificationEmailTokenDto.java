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
public class VerificationEmailTokenDto implements Encryptable<VerificationEmailTokenDto>{
	
	String verificationId;
	String email;
	public static final TokenType TOKENTYPE = TokenType.VERIFICATION_EMAIL;
	
	@Override
	public VerificationEmailTokenDto encrypt(AesProvider aesProvider) {
		return new VerificationEmailTokenDto(aesProvider.encrypt(this.verificationId),aesProvider.encrypt(this.email));
	}

	@Override
	public VerificationEmailTokenDto decrypt(AesProvider aesProvider) {
		return new VerificationEmailTokenDto(aesProvider.decrypt(this.verificationId),aesProvider.decrypt(this.email));
	}

	@JsonIgnore
	public int getIntVerificationId() {
		return Integer.parseInt(this.verificationId);
	}

}
