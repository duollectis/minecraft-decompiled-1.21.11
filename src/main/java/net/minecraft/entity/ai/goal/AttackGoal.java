package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;

/** Простая цель атаки: преследует и атакует текущую цель моба. */
public class AttackGoal extends Goal {

	private static final double MAX_TRACK_DISTANCE_SQ = 225.0;
	private static final double CLOSE_RANGE_SQ = 16.0;
	private static final double SPEED_NORMAL = 0.8;
	private static final double SPEED_CLOSE = 1.33;
	private static final double SPEED_VERY_CLOSE = 0.6;
	private static final int ATTACK_COOLDOWN = 20;

	private final MobEntity mob;
	private LivingEntity target;
	private int cooldown;

	public AttackGoal(MobEntity mob) {
		this.mob = mob;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity mobTarget = mob.getTarget();

		if (mobTarget == null) {
			return false;
		}

		target = mobTarget;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (!target.isAlive()) {
			return false;
		}

		if (mob.squaredDistanceTo(target) > MAX_TRACK_DISTANCE_SQ) {
			return false;
		}

		return !mob.getNavigation().isIdle() || canStart();
	}

	@Override
	public void stop() {
		target = null;
		mob.getNavigation().stop();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		mob.getLookControl().lookAt(target, 30.0F, 30.0F);

		double attackRangeSq = mob.getWidth() * 2.0F * (mob.getWidth() * 2.0F);
		double distSq = mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());

		double moveSpeed;

		if (distSq > attackRangeSq && distSq < CLOSE_RANGE_SQ) {
			moveSpeed = SPEED_CLOSE;
		}
		else if (distSq < MAX_TRACK_DISTANCE_SQ) {
			moveSpeed = SPEED_VERY_CLOSE;
		}
		else {
			moveSpeed = SPEED_NORMAL;
		}

		mob.getNavigation().startMovingTo(target, moveSpeed);
		cooldown = Math.max(cooldown - 1, 0);

		if (distSq <= attackRangeSq && cooldown <= 0) {
			cooldown = ATTACK_COOLDOWN;
			mob.tryAttack(getServerWorld(mob), target);
		}
	}
}
