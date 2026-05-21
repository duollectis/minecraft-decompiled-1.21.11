package net.minecraft.network.codec;

@FunctionalInterface
/**
 * Интерфейс value first encoder.
 */
public interface ValueFirstEncoder<O, T> {

	void encode(T value, O buf);
}
