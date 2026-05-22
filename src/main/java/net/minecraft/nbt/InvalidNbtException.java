package net.minecraft.nbt;

/**
 * Бросается при обнаружении структурно некорректных NBT-данных
 * (например, отрицательная длина списка или отсутствующий тип элемента).
 */
public class InvalidNbtException extends NbtException {

	public InvalidNbtException(String message) {
		super(message);
	}
}
