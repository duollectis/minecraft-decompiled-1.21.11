package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Hand;

import java.util.EnumSet;

/**
 * Цель ближнего боя: преследует и атакует цель моба.
 * Адаптирует частоту обновления пути в зависимости от расстояния до цели.
 */
public class MeleeAttackGoal extends Goal {

	private static final long ATTACK_INTERVAL_TICKS = 20L;
	private static final int PATH_UPDATE_BASE = 4;
	private static final int PATH_UPDATE_JITTER = 7;
	private static final int PATH_UPDATE_FAR_BONUS = 10;
	private static final int PATH_UPDATE_MID_BONUS = 5;
	private static final int PATH_UPDATE_FAIL_BONUS = 15;
	private static final double FAR_DISTANCE_SQ = 1024.0;
	private static final double MID_DISTANCE_SQ = 256.0;
	private static final double FLEE_SPEED_THRESHOLD_SQ = 49.0;
	private static final float RANDOM_RETARGET_CHANCE = 0.05F;
	private static final float LOOK_YAW_CHANGE = 30.0F;
	private static final float LOOK_PITCH_CHANGE = 30.0F;

	protected final PathAwareEntity mob;
	private final double speed;
	private final boolean pauseWhenMobIdle;
	private Path path;
	private double lastTargetX;
	private double lastTargetY;
	private double lastTargetZ;
	private int updateCountdownTicks;
	private int cooldown;
	private long lastUpdateTime;

	public MeleeAttackGoal(PathAwareEntity mob, double speed, boolean pauseWhenMobIdle) {
		this.mob = mob;
		this.speed = speed;
		this.pauseWhenMobIdle = pauseWhenMobIdle;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		long currentTime = mob.getEntityWorld().getTime();

		if (currentTime - lastUpdateTime < ATTACK_INTERVAL_TICKS) {
			return false;
		}

		lastUpdateTime = currentTime;
		LivingEntity target = mob.getTarget();

		if (target == null) {
			return false;
		}

		if (!target.isAlive()) {
			return false;
		}

		path = mob.getNavigation().findPathTo(target, 0);
		return path != null || mob.isInAttackRange(target);
	}

	@Override
	public boolean shouldContinue() {
		LivingEntity target = mob.getTarget();

		if (target == null) {
			return false;
		}

		if (!target.isAlive()) {
			return false;
		}

		if (!pauseWhenMobIdle) {
			return !mob.getNavigation().isIdle();
		}

		if (!mob.isInPositionTargetRange(target.getBlockPos())) {
			return false;
		}

		return !(target instanceof PlayerEntity player)
				|| (!player.isSpectator() && !player.isCreative());
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingAlong(path, speed);
		mob.setAttacking(true);
		updateCountdownTicks = 0;
		cooldown = 0;
	}

	@Override
	public void stop() {
		LivingEntity target = mob.getTarget();

		if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(target)) {
			mob.setTarget(null);
		}

		mob.setAttacking(false);
		mob.getNavigation().stop();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();

		if (target == null) {
			return;
		}

		mob.getLookControl().lookAt(target, LOOK_YAW_CHANGE, LOOK_PITCH_CHANGE);
		updateCountdownTicks = Math.max(updateCountdownTicks - 1, 0);

		boolean canSeeOrIdle = pauseWhenMobIdle || mob.getVisibilityCache().canSee(target);
		boolean targetMoved = target.squaredDistanceTo(lastTargetX, lastTargetY, lastTargetZ) >= 1.0;
		boolean noLastTarget = lastTargetX == 0.0 && lastTargetY == 0.0 && lastTargetZ == 0.0;
		boolean shouldUpdate = canSeeOrIdle
				&& updateCountdownTicks <= 0
				&& (noLastTarget || targetMoved || mob.getRandom().nextFloat() < RANDOM_RETARGET_CHANCE);

		if (shouldUpdate) {
			lastTargetX = target.getX();
			lastTargetY = target.getY();
			lastTargetZ = target.getZ();
			updateCountdownTicks = PATH_UPDATE_BASE + mob.getRandom().nextInt(PATH_UPDATE_JITTER);

			double distSq = mob.squaredDistanceTo(target);

			if (distSq > FAR_DISTANCE_SQ) {
				updateCountdownTicks += PATH_UPDATE_FAR_BONUS;
			}
			else if (distSq > MID_DISTANCE_SQ) {
				updateCountdownTicks += PATH_UPDATE_MID_BONUS;
			}

			if (!mob.getNavigation().startMovingTo(target, speed)) {
				updateCountdownTicks += PATH_UPDATE_FAIL_BONUS;
			}

			updateCountdownTicks = getTickCount(updateCountdownTicks);
		}

		cooldown = Math.max(cooldown - 1, 0);
		attack(target);
	}

	protected void attack(LivingEntity target) {
		if (canAttack(target)) {
			resetCooldown();
			mob.swingHand(Hand.MAIN_HAND);
			mob.tryAttack(getServerWorld(mob), target);
		}
	}

	protected void resetCooldown() {
		cooldown = getTickCount((int) ATTACK_INTERVAL_TICKS);
	}

	protected boolean isCooledDown() {
		return cooldown <= 0;
	}

	protected boolean canAttack(LivingEntity target) {
		return isCooledDown() && mob.isInAttackRange(target) && mob.getVisibilityCache().canSee(target);
	}

	protected int getCooldown() {
		return cooldown;
	}

	protected int getMaxCooldown() {
		return getTickCount((int) ATTACK_INTERVAL_TICKS);
	}
}
