package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Отладочные данные моба Breeze: цель атаки и целевая позиция прыжка.
 */
public record BreezeDebugData(Optional<Integer> attackTarget, Optional<BlockPos> jumpTarget) {

	public static final PacketCodec<ByteBuf, BreezeDebugData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT.collect(PacketCodecs::optional),
			BreezeDebugData::attackTarget,
			BlockPos.PACKET_CODEC.collect(PacketCodecs::optional),
			BreezeDebugData::jumpTarget,
			BreezeDebugData::new
	);
}
