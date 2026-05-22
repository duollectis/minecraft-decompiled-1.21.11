package net.minecraft.nbt;

/**
 * Бросается при превышении лимитов размера или глубины вложенности NBT-данных.
 *
 * @see NbtSizeTracker
 */
public class NbtSizeValidationException extends NbtException {

	public NbtSizeValidationException(String message) {
		super(message);
	}
}
