package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;

import java.util.EnumSet;

/**
 * Цель атаки луком: управляет стрейфом, прицеливанием и выстрелом.
 * Моб стрейфится влево/вправо и назад/вперёд в зависимости от дистанции до цели.
 */
public class BowAttackGoal<T extends HostileEntity & RangedAttackMob> extends Goal {

	private static final int STRAFE_RESET_TICKS = 20;
	private static final int MIN_SEEING_TICKS_TO_STOP = 20;
	private static final int MAX_UNSEEN_TICKS = -60;
	private static final int MIN_DRAW_TICKS = 20;
	private static final float STRAFE_CHANGE_CHANCE = 0.3F;
	private static final float FAR_RANGE_FACTOR = 0.75F;
	private static final float CLOSE_RANGE_FACTOR = 0.25F;
	private static final float STRAFE_SPEED = 0.5F;
	private static final float LOOK_ANGLE = 30.0F;

	private final T actor;
	private final double speed;
	private int attackInterval;
	private final float squaredRange;
	private int cooldown = -1;
	private int targetSeeingTicker;
	private boolean movingToLeft;
	private boolean backward;
	private int combatTicks = -1;

	public BowAttackGoal(T actor, double speed, int attackInterval, float range) {
		this.actor = actor;
		this.speed = speed;
		this.attackInterval = attackInterval;
		this.squaredRange = range * range;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	public void setAttackInterval(int attackInterval) {
		this.attackInterval = attackInterval;
	}

	@Override
	public boolean canStart() {
		return actor.getTarget() != null && isHoldingBow();
	}

	protected boolean isHoldingBow() {
		return actor.isHolding(Items.BOW);
	}

	@Override
	public boolean shouldContinue() {
		return (canStart() || !actor.getNavigation().isIdle()) && isHoldingBow();
	}

	@Override
	public void start() {
		super.start();
		actor.setAttacking(true);
	}

	@Override
	public void stop() {
		super.stop();
		actor.setAttacking(false);
		targetSeeingTicker = 0;
		cooldown = -1;
		actor.clearActiveItem();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = actor.getTarget();
		if (target == null) {
			return;
		}

		double distSq = actor.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		boolean canSee = actor.getVisibilityCache().canSee(target);
		boolean wasSeeingTarget = targetSeeingTicker > 0;
		if (canSee != wasSeeingTarget) {
			targetSeeingTicker = 0;
		}

		targetSeeingTicker = canSee ? targetSeeingTicker + 1 : targetSeeingTicker - 1;

		if (distSq <= squaredRange && targetSeeingTicker >= MIN_SEEING_TICKS_TO_STOP) {
			actor.getNavigation().stop();
			combatTicks++;
		}
		else {
			actor.getNavigation().startMovingTo(target, speed);
			combatTicks = -1;
		}

		if (combatTicks >= STRAFE_RESET_TICKS) {
			if (actor.getRandom().nextFloat() < STRAFE_CHANGE_CHANCE) {
				movingToLeft = !movingToLeft;
			}

			if (actor.getRandom().nextFloat() < STRAFE_CHANGE_CHANCE) {
				backward = !backward;
			}

			combatTicks = 0;
		}

		if (combatTicks > -1) {
			if (distSq > squaredRange * FAR_RANGE_FACTOR) {
				backward = false;
			}
			else if (distSq < squaredRange * CLOSE_RANGE_FACTOR) {
				backward = true;
			}

			float forwardStrafe = backward ? -STRAFE_SPEED : STRAFE_SPEED;
			float sideStrafe = movingToLeft ? STRAFE_SPEED : -STRAFE_SPEED;
			actor.getMoveControl().strafeTo(forwardStrafe, sideStrafe);
			if (actor.getControllingVehicle() instanceof MobEntity vehicle) {
				vehicle.lookAtEntity(target, LOOK_ANGLE, LOOK_ANGLE);
			}

			actor.lookAtEntity(target, LOOK_ANGLE, LOOK_ANGLE);
		}
		else {
			actor.getLookControl().lookAt(target, LOOK_ANGLE, LOOK_ANGLE);
		}

		if (actor.isUsingItem()) {
			if (!canSee && targetSeeingTicker < MAX_UNSEEN_TICKS) {
				actor.clearActiveItem();
			}
			else if (canSee) {
				int drawTicks = actor.getItemUseTime();
				if (drawTicks >= MIN_DRAW_TICKS) {
					actor.clearActiveItem();
					actor.shootAt(target, BowItem.getPullProgress(drawTicks));
					cooldown = attackInterval;
				}
			}
		}
		else if (--cooldown <= 0 && targetSeeingTicker >= MAX_UNSEEN_TICKS) {
			actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(actor, Items.BOW));
		}
	}
}
