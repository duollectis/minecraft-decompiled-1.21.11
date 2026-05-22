package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Цель следования за другим мобом иного вида: держится на минимальной дистанции,
 * при необходимости отступает, если цель смотрит прямо на преследователя.
 */
public class FollowMobGoal extends Goal {

	private static final int UPDATE_INTERVAL_TICKS = 10;

	private final MobEntity mob;
	private final Predicate<MobEntity> targetPredicate;
	private @Nullable MobEntity target;
	private final double speed;
	private final EntityNavigation navigation;
	private int updateCountdownTicks;
	private final float minDistance;
	private float oldWaterPathFindingPenalty;
	private final float maxDistance;

	public FollowMobGoal(MobEntity mob, double speed, float minDistance, float maxDistance) {
		this.mob = mob;
		this.targetPredicate = candidate -> mob.getClass() != candidate.getClass();
		this.speed = speed;
		this.navigation = mob.getNavigation();
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		if (!(mob.getNavigation() instanceof MobNavigation) && !(mob.getNavigation() instanceof BirdNavigation)) {
			throw new IllegalArgumentException("Unsupported mob type for FollowMobGoal");
		}
	}

	@Override
	public boolean canStart() {
		List<MobEntity> candidates = mob
				.getEntityWorld()
				.getEntitiesByClass(MobEntity.class, mob.getBoundingBox().expand(maxDistance), targetPredicate);
		for (MobEntity candidate : candidates) {
			if (!candidate.isInvisible()) {
				target = candidate;
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean shouldContinue() {
		return target != null && !navigation.isIdle()
				&& mob.squaredDistanceTo(target) > minDistance * minDistance;
	}

	@Override
	public void start() {
		updateCountdownTicks = 0;
		oldWaterPathFindingPenalty = mob.getPathfindingPenalty(PathNodeType.WATER);
		mob.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	@Override
	public void stop() {
		target = null;
		navigation.stop();
		mob.setPathfindingPenalty(PathNodeType.WATER, oldWaterPathFindingPenalty);
	}

	@Override
	public void tick() {
		if (target == null || mob.isLeashed()) {
			return;
		}

		mob.getLookControl().lookAt(target, 10.0F, mob.getMaxLookPitchChange());
		if (--updateCountdownTicks > 0) {
			return;
		}

		updateCountdownTicks = getTickCount(UPDATE_INTERVAL_TICKS);
		double dx = mob.getX() - target.getX();
		double dy = mob.getY() - target.getY();
		double dz = mob.getZ() - target.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;
		if (distSq > minDistance * minDistance) {
			navigation.startMovingTo(target, speed);
			return;
		}

		navigation.stop();
		LookControl lookControl = target.getLookControl();
		boolean targetLookingAtMob = lookControl.getLookX() == mob.getX()
				&& lookControl.getLookY() == mob.getY()
				&& lookControl.getLookZ() == mob.getZ();
		if (distSq <= minDistance || targetLookingAtMob) {
			double awayX = target.getX() - mob.getX();
			double awayZ = target.getZ() - mob.getZ();
			navigation.startMovingTo(mob.getX() - awayX, mob.getY(), mob.getZ() - awayZ, speed);
		}
	}
}
