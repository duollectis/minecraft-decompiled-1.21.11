package net.minecraft.network.packet;

import net.minecraft.network.codec.PacketCodec;

@FunctionalInterface
/**
 * Интерфейс packet codec modifier.
 */
public interface PacketCodecModifier<B, V, C> {

	PacketCodec<? super B, V> apply(PacketCodec<? super B, V> packetCodec, C context);
}
