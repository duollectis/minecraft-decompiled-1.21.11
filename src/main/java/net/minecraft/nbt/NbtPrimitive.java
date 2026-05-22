package net.minecraft.nbt;

/**
 * Маркерный интерфейс для примитивных (неизменяемых) NBT-элементов.
 *
 * <p>Все реализации являются иммутабельными value-объектами, поэтому {@link #copy()}
 * возвращает {@code this} без создания новых объектов.</p>
 */
public sealed interface NbtPrimitive extends NbtElement permits AbstractNbtNumber, NbtString {

	@Override
	default NbtElement copy() {
		return this;
	}
}
