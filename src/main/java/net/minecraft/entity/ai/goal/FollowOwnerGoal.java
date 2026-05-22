package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.TameableEntity;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель, заставляющая прирученное существо следовать за своим владельцем.
 * Автоматически телепортируется к владельцу, если расстояние слишком велико.
 */
public class FollowOwnerGoal extends Goal {

	private final TameableEntity tameable;
	private @Nullable LivingEntity owner;
	private final double speed;
	private final EntityNavigation navigation;
	private int updateCountdownTicks;
	private final float maxDistance;
	private final float minDistance;
	private float oldWaterPathfindingPenalty;

	public FollowOwnerGoal(TameableEntity tameable, double speed, float minDistance, float maxDistance) {
		this.tameable = tameable;
		this.speed = speed;
		this.navigation = tameable.getNavigation();
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		if (!(tameable.getNavigation() instanceof MobNavigation)
				&& !(tameable.getNavigation() instanceof BirdNavigation)) {
			throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
		}
	}

	@Override
	public boolean canStart() {
		LivingEntity livingEntity = tameable.getOwner();
		if (livingEntity == null) {
			return false;
		}

		if (tameable.cannotFollowOwner()) {
			return false;
		}

		if (tameable.squaredDistanceTo(livingEntity) < minDistance * minDistance) {
			return false;
		}

		owner = livingEntity;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (navigation.isIdle()) {
			return false;
		}

		if (tameable.cannotFollowOwner()) {
			return false;
		}

		return tameable.squaredDistanceTo(owner) > maxDistance * maxDistance;
	}

	@Override
	public void start() {
		updateCountdownTicks = 0;
		oldWaterPathfindingPenalty = tameable.getPathfindingPenalty(PathNodeType.WATER);
		tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	@Override
	public void stop() {
		owner = null;
		navigation.stop();
		tameable.setPathfindingPenalty(PathNodeType.WATER, oldWaterPathfindingPenalty);
	}

	@Override
	public void tick() {
		boolean shouldTeleport = tameable.shouldTryTeleportToOwner();
		if (!shouldTeleport) {
			tameable.getLookControl().lookAt(owner, 10.0F, tameable.getMaxLookPitchChange());
		}

		if (--updateCountdownTicks <= 0) {
			updateCountdownTicks = getTickCount(10);
			if (shouldTeleport) {
				tameable.tryTeleportToOwner();
			}
			else {
				navigation.startMovingTo(owner, speed);
			}
		}
	}
}
