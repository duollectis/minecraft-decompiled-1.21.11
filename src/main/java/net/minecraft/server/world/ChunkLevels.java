package net.minecraft.server.world;

import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Класс Chunk Levels.
 */
public class ChunkLevels {

	private static final int FULL = 33;
	private static final int BLOCK_TICKING = 32;
	private static final int ENTITY_TICKING = 31;
	private static final ChunkGenerationStep
			FULL_GENERATION_STEP =
			ChunkGenerationSteps.GENERATION.get(ChunkStatus.FULL);
	public static final int
			FULL_GENERATION_REQUIRED_LEVEL =
			FULL_GENERATION_STEP.accumulatedDependencies().getMaxLevel();
	public static final int INACCESSIBLE = 33 + FULL_GENERATION_REQUIRED_LEVEL;

	public static @Nullable ChunkStatus getStatus(int level) {
		return getStatusForAdditionalLevel(level - FULL, null);
	}

	@Contract("_,!null->!null;_,_->_")
	public static @Nullable ChunkStatus getStatusForAdditionalLevel(
			int additionalLevel,
			@Nullable ChunkStatus emptyStatus
	) {
		if (additionalLevel > FULL_GENERATION_REQUIRED_LEVEL) {
			return emptyStatus;
		}
		else {
			return additionalLevel <= 0 ? ChunkStatus.FULL
			                            : FULL_GENERATION_STEP.accumulatedDependencies().get(additionalLevel);
		}
	}

	public static ChunkStatus getStatusForAdditionalLevel(int level) {
		return getStatusForAdditionalLevel(level, ChunkStatus.EMPTY);
	}

	public static int getLevelFromStatus(ChunkStatus status) {
		return FULL + FULL_GENERATION_STEP.getAdditionalLevel(status);
	}

	public static ChunkLevelType getType(int level) {
		if (level <= ENTITY_TICKING) {
			return ChunkLevelType.ENTITY_TICKING;
		}
		else if (level <= BLOCK_TICKING) {
			return ChunkLevelType.BLOCK_TICKING;
		}
		else {
			return level <= FULL ? ChunkLevelType.FULL : ChunkLevelType.INACCESSIBLE;
		}
	}

	public static int getLevelFromType(ChunkLevelType type) {
		return switch (type) {
			case INACCESSIBLE -> INACCESSIBLE;
			case FULL -> FULL;
			case BLOCK_TICKING -> BLOCK_TICKING;
			case ENTITY_TICKING -> ENTITY_TICKING;
		};
	}

	/**
	 * Определяет, следует ли tick entities.
	 *
	 * @param level level
	 *
	 * @return boolean — результат операции
	 */
	public static boolean shouldTickEntities(int level) {
		return level <= ENTITY_TICKING;
	}

	/**
	 * Определяет, следует ли tick blocks.
	 *
	 * @param level level
	 *
	 * @return boolean — результат операции
	 */
	public static boolean shouldTickBlocks(int level) {
		return level <= BLOCK_TICKING;
	}

	public static boolean isAccessible(int level) {
		return level <= INACCESSIBLE;
	}
}
