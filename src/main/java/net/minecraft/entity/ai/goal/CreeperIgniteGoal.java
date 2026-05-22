package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель поджигания крипера: активирует фитиль при сближении с целью,
 * гасит его если цель потеряна, слишком далеко или не видна.
 */
public class CreeperIgniteGoal extends Goal {

	private static final double IGNITE_DISTANCE_SQ = 9.0;
	private static final double ABORT_DISTANCE_SQ = 49.0;
	private static final int FUSE_ACTIVE = 1;
	private static final int FUSE_INACTIVE = -1;

	private final CreeperEntity creeper;
	private @Nullable LivingEntity target;

	public CreeperIgniteGoal(CreeperEntity creeper) {
		this.creeper = creeper;
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		LivingEntity currentTarget = creeper.getTarget();
		return creeper.getFuseSpeed() > 0
				|| currentTarget != null && creeper.squaredDistanceTo(currentTarget) < IGNITE_DISTANCE_SQ;
	}

	@Override
	public void start() {
		creeper.getNavigation().stop();
		target = creeper.getTarget();
	}

	@Override
	public void stop() {
		target = null;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null
				|| creeper.squaredDistanceTo(target) > ABORT_DISTANCE_SQ
				|| !creeper.getVisibilityCache().canSee(target)
		) {
			creeper.setFuseSpeed(FUSE_INACTIVE);
			return;
		}

		creeper.setFuseSpeed(FUSE_ACTIVE);
	}
}
