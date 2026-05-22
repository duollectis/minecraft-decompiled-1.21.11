package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Отладочные данные пчелы: улей, цветок, время в пути и список заблокированных ульев.
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
	 * Проверяет, совпадает ли позиция улья пчелы с заданной позицией.
	 *
	 * @param pos позиция для сравнения
	 * @return {@code true}, если улей присутствует и его позиция совпадает с {@code pos}
	 */
	public boolean hivePosEquals(BlockPos pos) {
		return hivePos.isPresent() && pos.equals(hivePos.get());
	}
}
