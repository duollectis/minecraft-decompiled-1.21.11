package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Цель случайного блуждания моба. Периодически выбирает случайную точку и идёт к ней. */
public class WanderAroundGoal extends Goal {

	public static final int DEFAULT_CHANCE = 120;
	private static final int DESPAWN_COUNTER_THRESHOLD = 100;
	private static final int WANDER_HORIZONTAL_RANGE = 10;
	private static final int WANDER_VERTICAL_RANGE = 7;

	protected final PathAwareEntity mob;
	protected double targetX;
	protected double targetY;
	protected double targetZ;
	protected final double speed;
	protected int chance;
	protected boolean ignoringChance;
	private final boolean canDespawn;

	public WanderAroundGoal(PathAwareEntity mob, double speed) {
		this(mob, speed, DEFAULT_CHANCE);
	}

	public WanderAroundGoal(PathAwareEntity mob, double speed, int chance) {
		this(mob, speed, chance, true);
	}

	public WanderAroundGoal(PathAwareEntity mob, double speed, int chance, boolean canDespawn) {
		this.mob = mob;
		this.speed = speed;
		this.chance = chance;
		this.canDespawn = canDespawn;
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (mob.hasControllingPassenger()) {
			return false;
		}

		if (!ignoringChance) {
			if (canDespawn && mob.getDespawnCounter() >= DESPAWN_COUNTER_THRESHOLD) {
				return false;
			}

			if (mob.getRandom().nextInt(toGoalTicks(chance)) != 0) {
				return false;
			}
		}

		Vec3d wanderTarget = getWanderTarget();
		if (wanderTarget == null) {
			return false;
		}

		targetX = wanderTarget.x;
		targetY = wanderTarget.y;
		targetZ = wanderTarget.z;
		ignoringChance = false;
		return true;
	}

	protected @Nullable Vec3d getWanderTarget() {
		return NoPenaltyTargeting.find(mob, WANDER_HORIZONTAL_RANGE, WANDER_VERTICAL_RANGE);
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle() && !mob.hasControllingPassenger();
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		super.stop();
	}

	public void ignoreChanceOnce() {
		ignoringChance = true;
	}

	public void setChance(int chance) {
		this.chance = chance;
	}
}
