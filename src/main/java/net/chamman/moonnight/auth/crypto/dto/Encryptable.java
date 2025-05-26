package net.chamman.moonnight.auth.crypto.dto;

import net.chamman.moonnight.auth.crypto.AesProvider;

public interface Encryptable<T extends Encryptable<T>> {
	T encrypt(AesProvider aesProvider);
	T decrypt(AesProvider aesProvider);
}
