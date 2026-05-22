package net.minecraft.network.encryption;

import java.security.SignatureException;

/**
 * Функциональный интерфейс для объектов, которые могут обновлять состояние подписи,
 * передавая свои байтовые данные в {@link SignatureUpdater}.
 */
@FunctionalInterface
public interface SignatureUpdatable {

	void update(SignatureUpdatable.SignatureUpdater updater) throws SignatureException;

	/**
	 * Функциональный интерфейс для записи байтов в объект {@link java.security.Signature}.
	 */
	@FunctionalInterface
	interface SignatureUpdater {

		void update(byte[] data) throws SignatureException;
	}
}
