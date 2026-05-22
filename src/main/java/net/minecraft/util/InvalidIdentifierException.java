package net.minecraft.util;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Исключение, выбрасываемое при попытке создать {@link Identifier} с недопустимыми символами
 * в пространстве имён или пути.
 */
public class InvalidIdentifierException extends RuntimeException {

	public InvalidIdentifierException(String message) {
		super(StringEscapeUtils.escapeJava(message));
	}

	public InvalidIdentifierException(String message, Throwable cause) {
		super(StringEscapeUtils.escapeJava(message), cause);
	}
}
