package net.minecraft.network.codec;

@FunctionalInterface
/**
 * Интерфейс packet decoder.
 */
public interface PacketDecoder<I, T> {

	T decode(I buf);
}
