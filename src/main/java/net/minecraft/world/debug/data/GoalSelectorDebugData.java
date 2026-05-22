package net.minecraft.world.debug.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;

/**
 * Отладочные данные селектора целей сущности: список активных и неактивных целей с приоритетами.
 */
public record GoalSelectorDebugData(List<GoalSelectorDebugData.Goal> goals) {

	public static final PacketCodec<ByteBuf, GoalSelectorDebugData> PACKET_CODEC = PacketCodec.tuple(
			Goal.PACKET_CODEC.collect(PacketCodecs.toList()),
			GoalSelectorDebugData::goals,
			GoalSelectorDebugData::new
	);

	/**
	 * Снимок одной цели селектора: приоритет, статус выполнения и имя.
	 */
	public record Goal(int priority, boolean isRunning, String name) {

		public static final PacketCodec<ByteBuf, Goal> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT,
				Goal::priority,
				PacketCodecs.BOOLEAN,
				Goal::isRunning,
				PacketCodecs.string(255),
				Goal::name,
				Goal::new
		);
	}
}
