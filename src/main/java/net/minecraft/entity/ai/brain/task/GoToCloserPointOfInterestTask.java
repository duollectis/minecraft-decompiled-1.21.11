package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.poi.PointOfInterestStorage;

/**
 * Фабричный класс задачи мозга, направляющей жителя ближе к занятой точке интереса (POI).
 * Использует несколько попыток нечёткого поиска для выбора оптимальной позиции.
 */
public class GoToCloserPointOfInterestTask {

	private static final int SEARCH_ATTEMPTS = 5;
	private static final int HORIZONTAL_RANGE = 15;
	private static final int VERTICAL_RANGE = 7;

	public static Task<VillagerEntity> create(float speed, int completionRange) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET)).apply(
						context,
						walkTarget -> (world, entity, time) -> {
							if (world.isNearOccupiedPointOfInterest(entity.getBlockPos())) {
								return false;
							}

							PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();
							int currentDist = poiStorage.getDistanceFromNearestOccupied(
									ChunkSectionPos.from(entity.getBlockPos())
							);
							Vec3d bestPos = null;

							for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
								Vec3d candidate = FuzzyTargeting.find(
										entity,
										HORIZONTAL_RANGE,
										VERTICAL_RANGE,
										pos -> -poiStorage.getDistanceFromNearestOccupied(ChunkSectionPos.from(pos))
								);

								if (candidate == null) {
									continue;
								}

								int candidateDist = poiStorage.getDistanceFromNearestOccupied(
										ChunkSectionPos.from(BlockPos.ofFloored(candidate))
								);

								if (candidateDist < currentDist) {
									bestPos = candidate;
									break;
								}

								if (candidateDist == currentDist) {
									bestPos = candidate;
								}
							}

							if (bestPos != null) {
								walkTarget.remember(new WalkTarget(bestPos, speed, completionRange));
							}

							return true;
						}
				)
		);
	}
}
