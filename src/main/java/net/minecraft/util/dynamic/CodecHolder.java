package net.minecraft.util.dynamic;

import com.mojang.serialization.MapCodec;

/**
 * {@code CodecHolder}.
 */
public record CodecHolder<A>(MapCodec<A> codec) {

	/**
	 * Of.
	 *
	 * @param mapCodec map codec
	 *
	 * @return CodecHolder — результат операции
	 */
	public static <A> CodecHolder<A> of(MapCodec<A> mapCodec) {
		return new CodecHolder<>(mapCodec);
	}
}
