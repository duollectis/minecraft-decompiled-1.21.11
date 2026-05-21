package net.minecraft.network.packet;

import net.minecraft.network.codec.PacketCodec;

@FunctionalInterface
public interface PacketCodecModifier<B, V, C> {

	PacketCodec<? super B, V> apply(PacketCodec<? super B, V> packetCodec, C context);
}
