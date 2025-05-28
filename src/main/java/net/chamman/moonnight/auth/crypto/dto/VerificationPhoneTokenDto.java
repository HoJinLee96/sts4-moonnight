package net.chamman.moonnight.auth.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationPhoneTokenDto implements Encryptable<VerificationPhoneTokenDto>{
	
	String verificationId;
	String phone;
	public static final TokenType TOKENTYPE = TokenType.VERIFICATION_PHONE;
	
	
	@Override
	public VerificationPhoneTokenDto encrypt(AesProvider aesProvider) {
		return new VerificationPhoneTokenDto(aesProvider.encrypt(this.verificationId),aesProvider.encrypt(this.phone));
	}
	
	@Override
	public VerificationPhoneTokenDto decrypt(AesProvider aesProvider) {
		return new VerificationPhoneTokenDto(aesProvider.decrypt(this.verificationId),aesProvider.decrypt(this.phone));
	}
	
	public int getIntVerificationId() {
		return Integer.parseInt(verificationId);
	}
	
}
