package net.minecraft.network.encryption;

/**
 * Исключение, возникающее при ошибках шифрования или дешифрования сетевых пакетов.
 */
public class NetworkEncryptionException extends Exception {

	public NetworkEncryptionException(Throwable cause) {
		super(cause);
	}
}
