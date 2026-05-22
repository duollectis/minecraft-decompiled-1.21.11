package net.minecraft.world.gen.chunk.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.Optional;

/**
 * Размещение структур с равномерным случайным распределением по регионам.
 * Мир делится на квадратные регионы размером {@code spacing × spacing} чанков;
 * в каждом регионе структура размещается в случайной позиции со смещением
 * не менее {@code separation} чанков от края региона.
 */
public class RandomSpreadStructurePlacement extends StructurePlacement {

	public static final MapCodec<RandomSpreadStructurePlacement> CODEC =
			RecordCodecBuilder.<RandomSpreadStructurePlacement>mapCodec(
					instance -> buildCodec(instance)
							.and(instance.group(
									Codec.intRange(0, 4096)
									     .fieldOf("spacing")
									     .forGetter(RandomSpreadStructurePlacement::getSpacing),
									Codec.intRange(0, 4096)
									     .fieldOf("separation")
									     .forGetter(RandomSpreadStructurePlacement::getSeparation),
									SpreadType.CODEC
											.optionalFieldOf("spread_type", SpreadType.LINEAR)
											.forGetter(RandomSpreadStructurePlacement::getSpreadType)
							))
							.apply(instance, RandomSpreadStructurePlacement::new)
			).validate(RandomSpreadStructurePlacement::validate);

	private final int spacing;
	private final int separation;
	private final SpreadType spreadType;

	private static DataResult<RandomSpreadStructurePlacement> validate(RandomSpreadStructurePlacement placement) {
		return placement.spacing <= placement.separation
				? DataResult.error(() -> "Spacing has to be larger than separation")
				: DataResult.success(placement);
	}

	public RandomSpreadStructurePlacement(
			Vec3i locateOffset,
			FrequencyReductionMethod frequencyReductionMethod,
			float frequency,
			int salt,
			Optional<ExclusionZone> exclusionZone,
			int spacing,
			int separation,
			SpreadType spreadType
	) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
		this.spacing = spacing;
		this.separation = separation;
		this.spreadType = spreadType;
	}

	public RandomSpreadStructurePlacement(int spacing, int separation, SpreadType spreadType, int salt) {
		this(
				Vec3i.ZERO,
				FrequencyReductionMethod.DEFAULT,
				1.0F,
				salt,
				Optional.empty(),
				spacing,
				separation,
				spreadType
		);
	}

	public int getSpacing() {
		return spacing;
	}

	public int getSeparation() {
		return separation;
	}

	public SpreadType getSpreadType() {
		return spreadType;
	}

	/**
	 * Вычисляет стартовый чанк структуры для региона, содержащего чанк {@code (chunkX, chunkZ)}.
	 * Регион определяется делением координат на {@code spacing}; внутри региона
	 * позиция смещается случайно в диапазоне {@code [0, spacing - separation)}.
	 */
	public ChunkPos getStartChunk(long seed, int chunkX, int chunkZ) {
		int regionX = Math.floorDiv(chunkX, spacing);
		int regionZ = Math.floorDiv(chunkZ, spacing);
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setRegionSeed(seed, regionX, regionZ, getSalt());
		int spread = spacing - separation;
		int offsetX = spreadType.get(random, spread);
		int offsetZ = spreadType.get(random, spread);
		return new ChunkPos(regionX * spacing + offsetX, regionZ * spacing + offsetZ);
	}

	@Override
	protected boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		ChunkPos startChunk = getStartChunk(calculator.getStructureSeed(), chunkX, chunkZ);
		return startChunk.x == chunkX && startChunk.z == chunkZ;
	}

	@Override
	public StructurePlacementType<?> getType() {
		return StructurePlacementType.RANDOM_SPREAD;
	}
}
