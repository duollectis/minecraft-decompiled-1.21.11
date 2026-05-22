package net.minecraft.network.codec;

/**
 * Функциональный интерфейс для декодирования значения типа {@code T} из буфера типа {@code I}.
 *
 * @param <I> тип входного буфера (например, {@link io.netty.buffer.ByteBuf})
 * @param <T> тип декодируемого значения
 */
@FunctionalInterface
public interface PacketDecoder<I, T> {

	T decode(I buf);
}
