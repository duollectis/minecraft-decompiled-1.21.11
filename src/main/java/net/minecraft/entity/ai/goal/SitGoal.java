package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;

import java.util.EnumSet;

/**
 * Цель прирученного существа, удерживающая его в сидячей позе.
 * Прерывается, если хозяин рядом и на него нападают.
 */
public class SitGoal extends Goal {

	private static final double OWNER_NEAR_DISTANCE_SQ = 144.0;

	private final TameableEntity tameable;

	public SitGoal(TameableEntity tameable) {
		this.tameable = tameable;
		setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
	}

	@Override
	public boolean shouldContinue() {
		return tameable.isSitting();
	}

	@Override
	public boolean canStart() {
		boolean isSitting = tameable.isSitting();
		if (!isSitting && !tameable.isTamed()) {
			return false;
		}

		if (tameable.isTouchingWater()) {
			return false;
		}

		if (!tameable.isOnGround()) {
			return false;
		}

		LivingEntity owner = tameable.getOwner();
		if (owner == null || owner.getEntityWorld() != tameable.getEntityWorld()) {
			return true;
		}

		if (tameable.squaredDistanceTo(owner) < OWNER_NEAR_DISTANCE_SQ && owner.getAttacker() != null) {
			return false;
		}

		return isSitting;
	}

	@Override
	public void start() {
		tameable.getNavigation().stop();
		tameable.setInSittingPose(true);
	}

	@Override
	public void stop() {
		tameable.setInSittingPose(false);
	}
}
