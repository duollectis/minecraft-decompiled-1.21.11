package net.minecraft.network.encryption;

import java.security.SignatureException;

@FunctionalInterface
/**
 * Интерфейс signature updatable.
 */
public interface SignatureUpdatable {

	void update(SignatureUpdatable.SignatureUpdater updater) throws SignatureException;

	@FunctionalInterface
	/**
	 * Интерфейс signature updater.
	 */
	public interface SignatureUpdater {

		void update(byte[] data) throws SignatureException;
	}
}
