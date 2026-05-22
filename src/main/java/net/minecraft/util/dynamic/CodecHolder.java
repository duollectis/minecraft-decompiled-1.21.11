package net.minecraft.util.dynamic;

import com.mojang.serialization.MapCodec;

/**
 * Обёртка над {@link MapCodec}, позволяющая передавать кодек как значение
 * без потери информации о типе. Используется в диспетчерных кодеках.
 *
 * @param <A> тип кодируемого объекта
 */
public record CodecHolder<A>(MapCodec<A> codec) {

	public static <A> CodecHolder<A> of(MapCodec<A> mapCodec) {
		return new CodecHolder<>(mapCodec);
	}
}
