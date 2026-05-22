package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Optional;

/**
	 * Компонент отслеживания лодестона. Хранит глобальную позицию лодестона и флаг
	 * отслеживания. Если лодестон разрушен или находится в другом измерении —
	 * цель сбрасывается.
	 */
public record LodestoneTrackerComponent(Optional<GlobalPos> target, boolean tracked) {

	public static final Codec<LodestoneTrackerComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										GlobalPos.CODEC.optionalFieldOf("target").forGetter(LodestoneTrackerComponent::target),
										Codec.BOOL.optionalFieldOf("tracked", true).forGetter(LodestoneTrackerComponent::tracked)
								)
								.apply(instance, LodestoneTrackerComponent::new)
	);
	public static final PacketCodec<ByteBuf, LodestoneTrackerComponent> PACKET_CODEC = PacketCodec.tuple(
			GlobalPos.PACKET_CODEC.collect(PacketCodecs::optional),
			LodestoneTrackerComponent::target,
			PacketCodecs.BOOLEAN,
			LodestoneTrackerComponent::tracked,
			LodestoneTrackerComponent::new
	);

	/**
		 * Проверяет актуальность цели в указанном мире: если лодестон разрушен или
		 * вышел за пределы мира — возвращает компонент с пустой целью.
		 *
		 * @param world серверный мир, в котором проверяется позиция лодестона
		 * @return актуальный компонент (возможно с обнулённой целью)
		 */
	public LodestoneTrackerComponent forWorld(ServerWorld world) {
		if (!tracked || target.isEmpty()) {
			return this;
		}

		if (target.get().dimension() != world.getRegistryKey()) {
			return this;
		}

		BlockPos blockPos = target.get().pos();
		return world.isInBuildLimit(blockPos)
				&& world.getPointOfInterestStorage().hasTypeAt(PointOfInterestTypes.LODESTONE, blockPos)
				? this
				: new LodestoneTrackerComponent(Optional.empty(), true);
	}
}
