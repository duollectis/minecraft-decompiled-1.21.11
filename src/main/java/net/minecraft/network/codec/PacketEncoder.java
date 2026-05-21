package net.minecraft.network.codec;

@FunctionalInterface
/**
 * Интерфейс packet encoder.
 */
public interface PacketEncoder<O, T> {

	void encode(O buf, T value);
}
