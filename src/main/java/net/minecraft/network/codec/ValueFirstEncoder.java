package net.minecraft.network.codec;

/**
 * Вариант {@link PacketEncoder}, где аргументы переставлены: сначала значение, затем буфер.
 * Используется в {@link PacketCodec#of} для совместимости с лямбдами вида {@code (value, buf) -> ...}.
 *
 * @param <O> тип выходного буфера
 * @param <T> тип кодируемого значения
 */
@FunctionalInterface
public interface ValueFirstEncoder<O, T> {

	void encode(T value, O buf);
}
