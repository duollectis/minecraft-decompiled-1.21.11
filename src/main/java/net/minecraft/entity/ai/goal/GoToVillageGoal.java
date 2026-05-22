package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель, заставляющая моба ночью двигаться в сторону ближайшей деревни
 * (зоны с занятыми точками интереса). Использует нечёткое целеполагание
 * с оценкой расстояния до POI.
 */
public class GoToVillageGoal extends Goal {

	private static final int ARRIVAL_DISTANCE = 10;
	private static final int SEARCH_HORIZONTAL_RANGE = 15;
	private static final int SEARCH_VERTICAL_RANGE = 7;
	private static final int POI_NEAR_RADIUS = 6;
	private static final double WAYPOINT_BLEND = 0.4;
	private static final double WAYPOINT_STEP = 10.0;
	private static final int RANDOM_WAYPOINT_RANGE = 16;

	private final PathAwareEntity mob;
	private final int searchRange;
	private @Nullable BlockPos targetPosition;

	public GoToVillageGoal(PathAwareEntity mob, int searchRange) {
		this.mob = mob;
		this.searchRange = toGoalTicks(searchRange);
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (mob.hasControllingPassenger()) {
			return false;
		}

		if (mob.getEntityWorld().isDay()) {
			return false;
		}

		if (mob.getRandom().nextInt(searchRange) != 0) {
			return false;
		}

		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		BlockPos pos = mob.getBlockPos();

		if (!serverWorld.isNearOccupiedPointOfInterest(pos, POI_NEAR_RADIUS)) {
			return false;
		}

		Vec3d target = FuzzyTargeting.find(
			mob,
			SEARCH_HORIZONTAL_RANGE,
			SEARCH_VERTICAL_RANGE,
			blockPos -> -serverWorld.getOccupiedPointOfInterestDistance(ChunkSectionPos.from(blockPos))
		);

		targetPosition = target == null ? null : BlockPos.ofFloored(target);
		return targetPosition != null;
	}

	@Override
	public boolean shouldContinue() {
		return targetPosition != null
			&& !mob.getNavigation().isIdle()
			&& mob.getNavigation().getTargetPos().equals(targetPosition);
	}

	@Override
	public void tick() {
		if (targetPosition == null) {
			return;
		}

		EntityNavigation navigation = mob.getNavigation();

		if (navigation.isIdle() && !targetPosition.isWithinDistance(mob.getEntityPos(), ARRIVAL_DISTANCE)) {
			Vec3d targetCenter = Vec3d.ofBottomCenter(targetPosition);
			Vec3d mobPos = mob.getEntityPos();
			Vec3d blended = mobPos.subtract(targetCenter).multiply(WAYPOINT_BLEND).add(targetCenter);
			Vec3d waypoint = blended.subtract(mobPos).normalize().multiply(WAYPOINT_STEP).add(mobPos);
			BlockPos waypointPos = mob.getEntityWorld().getTopPosition(
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				BlockPos.ofFloored(waypoint)
			);

			if (!navigation.startMovingTo(waypointPos.getX(), waypointPos.getY(), waypointPos.getZ(), 1.0)) {
				findOtherWaypoint();
			}
		}
	}

	private void findOtherWaypoint() {
		Random random = mob.getRandom();
		BlockPos randomPos = mob.getEntityWorld().getTopPosition(
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
			mob.getBlockPos().add(
				-8 + random.nextInt(RANDOM_WAYPOINT_RANGE),
				0,
				-8 + random.nextInt(RANDOM_WAYPOINT_RANGE)
			)
		);
		mob.getNavigation().startMovingTo(randomPos.getX(), randomPos.getY(), randomPos.getZ(), 1.0);
	}
}
