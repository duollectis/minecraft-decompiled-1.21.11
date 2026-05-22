package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.task.TargetUtil;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Цель блуждания к ближайшей точке интереса (POI): если моб уже рядом с занятым POI,
 * цель не активируется; иначе ищет путь к ближайшей секции с POI.
 */
public class WanderAroundPointOfInterestGoal extends WanderAroundGoal {

	private static final int HORIZONTAL_RANGE = 10;
	private static final int VERTICAL_RANGE = 7;
	private static final int POI_SEARCH_RADIUS = 2;
	private static final float HALF_PI = (float) (Math.PI / 2);

	public WanderAroundPointOfInterestGoal(PathAwareEntity entity, double speed, boolean canDespawn) {
		super(entity, speed, HORIZONTAL_RANGE, canDespawn);
	}

	@Override
	public boolean canStart() {
		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		BlockPos pos = mob.getBlockPos();

		if (serverWorld.isNearOccupiedPointOfInterest(pos)) {
			return false;
		}

		return super.canStart();
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		BlockPos pos = mob.getBlockPos();
		ChunkSectionPos currentSection = ChunkSectionPos.from(pos);
		ChunkSectionPos targetSection = TargetUtil.getPosClosestToOccupiedPointOfInterest(
				serverWorld,
				currentSection,
				POI_SEARCH_RADIUS
		);

		return targetSection != currentSection
				? NoPenaltyTargeting.findTo(
						mob,
						HORIZONTAL_RANGE,
						VERTICAL_RANGE,
						Vec3d.ofBottomCenter(targetSection.getCenterPos()),
						HALF_PI
				)
				: null;
	}
}
