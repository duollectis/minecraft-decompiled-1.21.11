package net.minecraft.network.encryption;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collection;

/**
 * Интерфейс для верификации криптографических подписей сообщений чата и ключей игроков.
 */
public interface SignatureVerifier {

	SignatureVerifier NOOP = (updatable, signatureData) -> true;

	Logger LOGGER = LogUtils.getLogger();

	boolean validate(SignatureUpdatable updatable, byte[] signatureData);

	default boolean validate(byte[] signedData, byte[] signatureData) {
		return validate(updater -> updater.update(signedData), signatureData);
	}

	private static boolean verify(SignatureUpdatable updatable, byte[] signatureData, Signature signature)
	throws SignatureException {
		updatable.update(signature::update);
		return signature.verify(signatureData);
	}

	static SignatureVerifier create(PublicKey publicKey, String algorithm) {
		return (updatable, signatureData) -> {
			try {
				Signature signature = Signature.getInstance(algorithm);
				signature.initVerify(publicKey);
				return verify(updatable, signatureData, signature);
			} catch (Exception e) {
				LOGGER.error("Failed to verify signature", e);
				return false;
			}
		};
	}

	static @Nullable SignatureVerifier create(ServicesKeySet servicesKeySet, ServicesKeyType servicesKeyType) {
		Collection<ServicesKeyInfo> keys = servicesKeySet.keys(servicesKeyType);
		return keys.isEmpty()
				? null
				: (updatable, signatureData) -> keys.stream().anyMatch(keyInfo -> {
					Signature signature = keyInfo.signature();

					try {
						return verify(updatable, signatureData, signature);
					} catch (SignatureException e) {
						LOGGER.error("Failed to verify Services signature", e);
						return false;
					}
				});
	}
}
