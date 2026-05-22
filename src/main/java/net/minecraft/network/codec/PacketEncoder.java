package net.minecraft.network.codec;

/**
 * Функциональный интерфейс для кодирования значения типа {@code T} в буфер типа {@code O}.
 *
 * @param <O> тип выходного буфера (например, {@link io.netty.buffer.ByteBuf})
 * @param <T> тип кодируемого значения
 */
@FunctionalInterface
public interface PacketEncoder<O, T> {

	void encode(O buf, T value);
}
