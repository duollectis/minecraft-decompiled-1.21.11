package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Непроверяемое исключение для ошибок OpenGL/GLFW на клиентской стороне.
 */
@Environment(EnvType.CLIENT)
public class GlException extends RuntimeException {

	public GlException(String message) {
		super(message);
	}

	public GlException(String message, Throwable cause) {
		super(message, cause);
	}
}
