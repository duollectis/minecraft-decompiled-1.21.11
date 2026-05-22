package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, направляющей сущность к ближайшей занятой точке интереса (POI).
 * Если сущность уже рядом с POI — использует случайное блуждание, иначе — целенаправленное движение.
 */
public class GoToPointOfInterestTask {

	private static final int DEFAULT_HORIZONTAL_RANGE = 10;
	private static final int DEFAULT_VERTICAL_RANGE = 7;
	private static final int POI_SEARCH_RADIUS = 2;
	private static final float HALF_PI = (float) (Math.PI / 2);

	public static SingleTickTask<PathAwareEntity> create(float walkSpeed) {
		return create(walkSpeed, DEFAULT_HORIZONTAL_RANGE, DEFAULT_VERTICAL_RANGE);
	}

	public static SingleTickTask<PathAwareEntity> create(float walkSpeed, int horizontalRange, int verticalRange) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET))
						.apply(
								context,
								walkTarget -> (world, entity, time) -> {
									BlockPos pos = entity.getBlockPos();
									Vec3d target;

									if (world.isNearOccupiedPointOfInterest(pos)) {
										target = FuzzyTargeting.find(entity, horizontalRange, verticalRange);
									} else {
										ChunkSectionPos section = ChunkSectionPos.from(pos);
										ChunkSectionPos closerSection = TargetUtil.getPosClosestToOccupiedPointOfInterest(
												world, section, POI_SEARCH_RADIUS
										);

										target = closerSection != section
																? NoPenaltyTargeting.findTo(
																		entity,
																		horizontalRange,
																		verticalRange,
																		Vec3d.ofBottomCenter(closerSection.getCenterPos()),
																		HALF_PI
																)
												: FuzzyTargeting.find(entity, horizontalRange, verticalRange);
									}

									walkTarget.remember(Optional.ofNullable(target).map(p -> new WalkTarget(p, walkSpeed, 0)));
									return true;
								}
						)
		);
	}
}
