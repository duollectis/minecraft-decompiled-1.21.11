package net.minecraft.world;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.light.LevelPropagator;

/**
 * Распространитель уровней расстояния по секциям чанков.
 * Обходит все 26 соседних секций (3×3×3 минус центр) при распространении
 * и пересчёте уровней.
 */
public abstract class SectionDistanceLevelPropagator extends LevelPropagator {

	protected SectionDistanceLevelPropagator(int levelCount, int expectedLevelSize, int expectedTotalSize) {
		super(levelCount, expectedLevelSize, expectedTotalSize);
	}

	@Override
	protected void propagateLevel(long id, int level, boolean decrease) {
		if (decrease && level >= levelCount - 2) {
			return;
		}

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					long neighborId = ChunkSectionPos.offset(id, dx, dy, dz);

					if (neighborId != id) {
						propagateLevel(id, neighborId, level, decrease);
					}
				}
			}
		}
	}

	@Override
	protected int recalculateLevel(long id, long excludedId, int maxLevel) {
		int minLevel = maxLevel;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					// Центральная секция ссылается сама на себя через Long.MAX_VALUE (маркер)
					long neighborId = ChunkSectionPos.offset(id, dx, dy, dz);

					if (neighborId == id) {
						neighborId = Long.MAX_VALUE;
					}

					if (neighborId == excludedId) {
						continue;
					}

					int propagated = getPropagatedLevel(neighborId, id, getLevel(neighborId));

					if (minLevel > propagated) {
						minLevel = propagated;
					}

					if (minLevel == 0) {
						return minLevel;
					}
				}
			}
		}

		return minLevel;
	}

	@Override
	protected int getPropagatedLevel(long sourceId, long targetId, int level) {
		return isMarker(sourceId) ? getInitialLevel(targetId) : level + 1;
	}

	protected abstract int getInitialLevel(long id);

	/** Инициирует обновление уровня для секции с заданным ID. */
	public void update(long id, int level, boolean decrease) {
		updateLevel(Long.MAX_VALUE, id, level, decrease);
	}
}
