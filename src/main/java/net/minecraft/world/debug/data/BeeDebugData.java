package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * {@code BeeDebugData}.
 */
public record BeeDebugData(
		Optional<BlockPos> hivePos,
		Optional<BlockPos> flowerPos,
		int travelTicks,
		List<BlockPos> blacklistedHives
) {

	public static final PacketCodec<ByteBuf, BeeDebugData> PACKET_CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC.collect(PacketCodecs::optional),
			BeeDebugData::hivePos,
			BlockPos.PACKET_CODEC.collect(PacketCodecs::optional),
			BeeDebugData::flowerPos,
			PacketCodecs.VAR_INT,
			BeeDebugData::travelTicks,
			BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()),
			BeeDebugData::blacklistedHives,
			BeeDebugData::new
	);

	/**
	 * Hive pos equals.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean hivePosEquals(BlockPos pos) {
		return this.hivePos.isPresent() && pos.equals(this.hivePos.get());
	}
}
