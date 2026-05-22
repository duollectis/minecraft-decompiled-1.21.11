package net.minecraft.server.dedicated.management;

/**
 * Класс Rpc Method Not Found Exception.
 */
public class RpcMethodNotFoundException extends RuntimeException {

	public RpcMethodNotFoundException(String message) {
		super(message);
	}
}
