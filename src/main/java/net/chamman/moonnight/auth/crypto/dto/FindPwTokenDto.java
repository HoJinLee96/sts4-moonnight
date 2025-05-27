package net.chamman.moonnight.auth.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.chamman.moonnight.auth.crypto.AesProvider;
import net.chamman.moonnight.auth.crypto.TokenProvider.TokenType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FindPwTokenDto implements Encryptable<FindPwTokenDto>{
	
	private String userId;
	private String email;
	
	public static final TokenType TOKENTYPE = TokenType.ACCESS_FINDPW;

	@Override
	public FindPwTokenDto encrypt(AesProvider aesProvider) {
		return new FindPwTokenDto(aesProvider.encrypt(userId),aesProvider.encrypt(email));
	}

	@Override
	public FindPwTokenDto decrypt(AesProvider aesProvider) {
		return new FindPwTokenDto(aesProvider.decrypt(userId),aesProvider.decrypt(email));
	}

	public int getIntUserId() {
		return Integer.parseInt(userId);
	}
}
