package net.minecraft.network.encryption;

import java.security.SecureRandom;

/**
 * Неизменяемый токен-носитель (Bearer Token) для аутентификации.
 * Токен представляет собой строку из 40 случайных буквенно-цифровых символов.
 */
public record BearerToken(String secretKey) {

	private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int TOKEN_LENGTH = 40;

	public static boolean isValid(String token) {
		return !token.isEmpty() && token.matches("^[a-zA-Z0-9]{40}$");
	}

	public static String generate() {
		SecureRandom random = new SecureRandom();
		StringBuilder builder = new StringBuilder(TOKEN_LENGTH);

		for (int index = 0; index < TOKEN_LENGTH; index++) {
			builder.append(ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length())));
		}

		return builder.toString();
	}
}
