package net.minecraft.world.gen.chunk.placement;

import com.mojang.datafixers.Products.P5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

import java.util.Optional;

/**
 * Базовый класс стратегии размещения структуры в мире.
 * Определяет, должна ли структура генерироваться в данном чанке,
 * с учётом частоты появления, зоны исключения и конкретного алгоритма размещения.
 */
public abstract class StructurePlacement {

	public static final Codec<StructurePlacement> TYPE_CODEC = Registries.STRUCTURE_PLACEMENT
			.getCodec()
			.dispatch(StructurePlacement::getType, StructurePlacementType::codec);

	// Соль для устаревшего алгоритма LEGACY_TYPE_2 — фиксированное значение из оригинального кода.
	private static final int ARBITRARY_SALT = 10387320;

	private final Vec3i locateOffset;
	private final FrequencyReductionMethod frequencyReductionMethod;
	private final float frequency;
	private final int salt;
	private final Optional<ExclusionZone> exclusionZone;

	protected static <S extends StructurePlacement> P5<Mu<S>, Vec3i, FrequencyReductionMethod, Float, Integer, Optional<ExclusionZone>> buildCodec(
			Instance<S> instance
	) {
		return instance.group(
				Vec3i.createOffsetCodec(16)
				     .optionalFieldOf("locate_offset", Vec3i.ZERO)
				     .forGetter(StructurePlacement::getLocateOffset),
				FrequencyReductionMethod.CODEC
						.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT)
						.forGetter(StructurePlacement::getFrequencyReductionMethod),
				Codec.floatRange(0.0F, 1.0F)
				     .optionalFieldOf("frequency", 1.0F)
				     .forGetter(StructurePlacement::getFrequency),
				Codecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(StructurePlacement::getSalt),
				ExclusionZone.CODEC
						.optionalFieldOf("exclusion_zone")
						.forGetter(StructurePlacement::getExclusionZone)
		);
	}

	protected StructurePlacement(
			Vec3i locateOffset,
			FrequencyReductionMethod frequencyReductionMethod,
			float frequency,
			int salt,
			Optional<ExclusionZone> exclusionZone
	) {
		this.locateOffset = locateOffset;
		this.frequencyReductionMethod = frequencyReductionMethod;
		this.frequency = frequency;
		this.salt = salt;
		this.exclusionZone = exclusionZone;
	}

	protected Vec3i getLocateOffset() {
		return locateOffset;
	}

	protected FrequencyReductionMethod getFrequencyReductionMethod() {
		return frequencyReductionMethod;
	}

	protected float getFrequency() {
		return frequency;
	}

	protected int getSalt() {
		return salt;
	}

	protected Optional<ExclusionZone> getExclusionZone() {
		return exclusionZone;
	}

	/**
	 * Проверяет, должна ли структура генерироваться в чанке {@code (chunkX, chunkZ)}.
	 * Последовательно применяет: проверку стартового чанка, снижение частоты и зону исключения.
	 */
	public boolean shouldGenerate(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		return isStartChunk(calculator, chunkX, chunkZ)
				&& applyFrequencyReduction(chunkX, chunkZ, calculator.getStructureSeed())
				&& applyExclusionZone(calculator, chunkX, chunkZ);
	}

	/**
	 * Применяет снижение частоты появления структуры.
	 * Если частота равна 1.0, проверка пропускается — структура всегда проходит.
	 */
	public boolean applyFrequencyReduction(int chunkX, int chunkZ, long seed) {
		return frequency >= 1.0F || frequencyReductionMethod.shouldGenerate(seed, salt, chunkX, chunkZ, frequency);
	}

	/**
	 * Проверяет, не попадает ли чанк в зону исключения другого набора структур.
	 */
	public boolean applyExclusionZone(StructurePlacementCalculator calculator, int centerChunkX, int centerChunkZ) {
		return exclusionZone.isEmpty()
				|| !exclusionZone.get().shouldExclude(calculator, centerChunkX, centerChunkZ);
	}

	protected abstract boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ);

	public BlockPos getLocatePos(ChunkPos chunkPos) {
		return new BlockPos(chunkPos.getStartX(), 0, chunkPos.getStartZ()).add(getLocateOffset());
	}

	public abstract StructurePlacementType<?> getType();

	private static boolean defaultShouldGenerate(long seed, int salt, int chunkX, int chunkZ, float frequency) {
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setRegionSeed(seed, salt, chunkX, chunkZ);
		return random.nextFloat() < frequency;
	}

	private static boolean legacyType3ShouldGenerate(long seed, int salt, int chunkX, int chunkZ, float frequency) {
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setCarverSeed(seed, chunkX, chunkZ);
		return random.nextDouble() < frequency;
	}

	private static boolean legacyType2ShouldGenerate(long seed, int salt, int chunkX, int chunkZ, float frequency) {
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setRegionSeed(seed, chunkX, chunkZ, ARBITRARY_SALT);
		return random.nextFloat() < frequency;
	}

	private static boolean legacyType1ShouldGenerate(long seed, int salt, int chunkX, int chunkZ, float frequency) {
		int regionX = chunkX >> 4;
		int regionZ = chunkZ >> 4;
		ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
		random.setSeed(regionX ^ regionZ << 4 ^ seed);
		random.nextInt();
		return random.nextInt((int) (1.0F / frequency)) == 0;
	}

	/**
	 * Зона исключения: запрещает генерацию данной структуры, если в радиусе {@code chunkCount}
	 * чанков уже может генерироваться структура из {@code otherSet}.
	 * Помечена как {@code @Deprecated} — механизм устарел и может быть удалён в будущем.
	 */
	@Deprecated
	public record ExclusionZone(RegistryEntry<StructureSet> otherSet, int chunkCount) {

		public static final Codec<ExclusionZone> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						RegistryElementCodec.of(RegistryKeys.STRUCTURE_SET, StructureSet.CODEC, false)
						                    .fieldOf("other_set")
						                    .forGetter(ExclusionZone::otherSet),
						Codec.intRange(1, 16)
						     .fieldOf("chunk_count")
						     .forGetter(ExclusionZone::chunkCount)
				).apply(instance, ExclusionZone::new)
		);

		boolean shouldExclude(StructurePlacementCalculator calculator, int centerChunkX, int centerChunkZ) {
			return calculator.canGenerate(otherSet, centerChunkX, centerChunkZ, chunkCount);
		}
	}

	/**
	 * Метод снижения частоты появления структуры.
	 * Разные варианты используют разные алгоритмы генерации случайных чисел
	 * для обратной совместимости с историческими версиями мира.
	 */
	public enum FrequencyReductionMethod implements StringIdentifiable {
		DEFAULT("default", StructurePlacement::defaultShouldGenerate),
		LEGACY_TYPE_1("legacy_type_1", StructurePlacement::legacyType1ShouldGenerate),
		LEGACY_TYPE_2("legacy_type_2", StructurePlacement::legacyType2ShouldGenerate),
		LEGACY_TYPE_3("legacy_type_3", StructurePlacement::legacyType3ShouldGenerate);

		public static final Codec<FrequencyReductionMethod> CODEC = StringIdentifiable.createCodec(
				FrequencyReductionMethod::values
		);

		private final String name;
		private final GenerationPredicate generationPredicate;

		FrequencyReductionMethod(String name, GenerationPredicate generationPredicate) {
			this.name = name;
			this.generationPredicate = generationPredicate;
		}

		public boolean shouldGenerate(long seed, int salt, int chunkX, int chunkZ, float chance) {
			return generationPredicate.shouldGenerate(seed, salt, chunkX, chunkZ, chance);
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Предикат, определяющий, должна ли структура генерироваться с заданной вероятностью.
	 */
	@FunctionalInterface
	public interface GenerationPredicate {

		boolean shouldGenerate(long seed, int salt, int chunkX, int chunkZ, float chance);
	}
}
