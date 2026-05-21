package net.minecraft.nbt;

/**
 * {@code NbtPrimitive}.
 */
public sealed interface NbtPrimitive extends NbtElement permits AbstractNbtNumber, NbtString {

	@Override
	default NbtElement copy() {
		return this;
	}
}
