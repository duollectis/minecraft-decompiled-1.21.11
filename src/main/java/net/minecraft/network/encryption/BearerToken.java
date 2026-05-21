package net.minecraft.network.encryption;

import java.security.SecureRandom;

/**
 * Запись bearer token.
 */
public record BearerToken(String secretKey) {

	private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	public static boolean isValid(String token) {
		return token.isEmpty() ? false : token.matches("^[a-zA-Z0-9]{40}$");
	}

	/**
	 * Generate.
	 *
	 * @return String — результат операции
	 */
	public static String generate() {
		SecureRandom secureRandom = new SecureRandom();
		StringBuilder stringBuilder = new StringBuilder(40);

		for (int i = 0; i < 40; i++) {
			stringBuilder.append(
					"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
							.charAt(secureRandom.nextInt(
									"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length()))
			);
		}

		return stringBuilder.toString();
	}
}
