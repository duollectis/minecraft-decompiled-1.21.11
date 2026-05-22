package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/**
 * Свойства мира: точка спавна, время, погода, сложность.
 * Реализуется классами уровня (LevelProperties) и предоставляет
 * базовый контракт для доступа к метаданным мира из любого контекста.
 */
public interface WorldProperties {

	WorldProperties.SpawnPoint getSpawnPoint();

	long getTime();

	long getTimeOfDay();

	boolean isThundering();

	boolean isRaining();

	void setRaining(boolean raining);

	boolean isHardcore();

	Difficulty getDifficulty();

	boolean isDifficultyLocked();

	default void populateCrashReport(CrashReportSection reportSection, HeightLimitView world) {
		reportSection.add(
				"Level spawn location",
				() -> CrashReportSection.createPositionString(world, this.getSpawnPoint().getPos())
		);
		reportSection.add(
				"Level time",
				() -> String.format(Locale.ROOT, "%d game time, %d day time", this.getTime(), this.getTimeOfDay())
		);
	}

	/**
	 * Точка спавна игрока: глобальная позиция (измерение + координаты),
	 * угол поворота по горизонтали и вертикали. Нормализует углы при создании.
	 */
	public record SpawnPoint(GlobalPos globalPos, float yaw, float pitch) {

		public static final WorldProperties.SpawnPoint
				DEFAULT =
				new WorldProperties.SpawnPoint(GlobalPos.create(World.OVERWORLD, BlockPos.ORIGIN), 0.0F, 0.0F);
		public static final MapCodec<WorldProperties.SpawnPoint> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    GlobalPos.MAP_CODEC.forGetter(WorldProperties.SpawnPoint::globalPos),
						                    Codec.floatRange(-180.0F, 180.0F).fieldOf("yaw").forGetter(WorldProperties.SpawnPoint::yaw),
						                    Codec.floatRange(-90.0F, 90.0F).fieldOf("pitch").forGetter(WorldProperties.SpawnPoint::pitch)
				                    )
				                    .apply(instance, WorldProperties.SpawnPoint::new)
		);
		public static final Codec<WorldProperties.SpawnPoint> CODEC = MAP_CODEC.codec();
		public static final PacketCodec<ByteBuf, WorldProperties.SpawnPoint> PACKET_CODEC = PacketCodec.tuple(
				GlobalPos.PACKET_CODEC,
				WorldProperties.SpawnPoint::globalPos,
				PacketCodecs.FLOAT,
				WorldProperties.SpawnPoint::yaw,
				PacketCodecs.FLOAT,
				WorldProperties.SpawnPoint::pitch,
				WorldProperties.SpawnPoint::new
		);

		public static WorldProperties.SpawnPoint create(
				RegistryKey<World> dimension,
				BlockPos pos,
				float yaw,
				float pitch
		) {
			return new WorldProperties.SpawnPoint(
					GlobalPos.create(dimension, pos.toImmutable()),
					MathHelper.wrapDegrees(yaw),
					MathHelper.clamp(pitch, -90.0F, 90.0F)
			);
		}

		public RegistryKey<World> getDimension() {
			return this.globalPos.dimension();
		}

		public BlockPos getPos() {
			return this.globalPos.pos();
		}
	}
}
