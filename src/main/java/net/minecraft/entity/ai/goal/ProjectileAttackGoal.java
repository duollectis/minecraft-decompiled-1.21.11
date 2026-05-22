package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель дальнобойной атаки снарядами: сближается с целью пока не войдёт в зону стрельбы,
 * затем стреляет с интервалом, масштабируемым по дистанции.
 */
public class ProjectileAttackGoal extends Goal {

	private static final int COOLDOWN_UNSET = -1;
	private static final int MIN_SEEN_TICKS = 5;
	private static final float LOOK_ANGLE = 30.0F;
	private static final float MIN_POWER = 0.1F;

	private final MobEntity mob;
	private final RangedAttackMob owner;
	private @Nullable LivingEntity target;
	private int updateCountdownTicks = COOLDOWN_UNSET;
	private final double mobSpeed;
	private int seenTargetTicks;
	private final int minIntervalTicks;
	private final int maxIntervalTicks;
	private final float maxShootRange;
	private final float squaredMaxShootRange;

	public ProjectileAttackGoal(RangedAttackMob mob, double mobSpeed, int intervalTicks, float maxShootRange) {
		this(mob, mobSpeed, intervalTicks, intervalTicks, maxShootRange);
	}

	public ProjectileAttackGoal(
			RangedAttackMob mob,
			double mobSpeed,
			int minIntervalTicks,
			int maxIntervalTicks,
			float maxShootRange
	) {
		if (!(mob instanceof LivingEntity)) {
			throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
		}

		this.owner = mob;
		this.mob = (MobEntity) mob;
		this.mobSpeed = mobSpeed;
		this.minIntervalTicks = minIntervalTicks;
		this.maxIntervalTicks = maxIntervalTicks;
		this.maxShootRange = maxShootRange;
		this.squaredMaxShootRange = maxShootRange * maxShootRange;
		setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity currentTarget = mob.getTarget();
		if (currentTarget == null || !currentTarget.isAlive()) {
			return false;
		}

		target = currentTarget;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return canStart() || target.isAlive() && !mob.getNavigation().isIdle();
	}

	@Override
	public void stop() {
		target = null;
		seenTargetTicks = 0;
		updateCountdownTicks = COOLDOWN_UNSET;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		double distSq = mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		boolean canSee = mob.getVisibilityCache().canSee(target);
		seenTargetTicks = canSee ? seenTargetTicks + 1 : 0;

		if (distSq <= squaredMaxShootRange && seenTargetTicks >= MIN_SEEN_TICKS) {
			mob.getNavigation().stop();
		}
		else {
			mob.getNavigation().startMovingTo(target, mobSpeed);
		}

		mob.getLookControl().lookAt(target, LOOK_ANGLE, LOOK_ANGLE);

		if (--updateCountdownTicks == 0) {
			if (!canSee) {
				return;
			}

			float distanceFactor = (float) Math.sqrt(distSq) / maxShootRange;
			float power = MathHelper.clamp(distanceFactor, MIN_POWER, 1.0F);
			owner.shootAt(target, power);
			updateCountdownTicks = MathHelper.floor(
					distanceFactor * (maxIntervalTicks - minIntervalTicks) + minIntervalTicks
			);
		}
		else if (updateCountdownTicks < 0) {
			updateCountdownTicks = MathHelper.floor(
					MathHelper.lerp(Math.sqrt(distSq) / maxShootRange, minIntervalTicks, maxIntervalTicks)
			);
		}
	}
}
