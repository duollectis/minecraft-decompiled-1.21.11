package net.minecraft.nbt;

/**
 * Базовое непроверяемое исключение для ошибок, связанных с обработкой NBT-данных.
 */
public class NbtException extends RuntimeException {

	public NbtException(String message) {
		super(message);
	}
}
