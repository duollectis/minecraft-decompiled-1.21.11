package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

/**
 * Базовая цель навигации к конкретной позиции блока: ищет подходящую позицию
 * в радиусе {@code range} и ведёт моба к ней, пока не достигнет или не истечёт
 * время ожидания. Подклассы определяют критерий целевой позиции через
 * {@link #isTargetPos(WorldView, BlockPos)}.
 */
public abstract class MoveToTargetPosGoal extends Goal {

	private static final int MIN_WAITING_TIME = 1200;
	private static final int MIN_INTERVAL = 200;

	protected final PathAwareEntity mob;
	public final double speed;
	protected int cooldown;
	protected int tryingTime;
	private int safeWaitingTime;
	protected BlockPos targetPos = BlockPos.ORIGIN;
	private boolean reached;
	private final int range;
	private final int maxYDifference;
	protected int lowestY;

	public MoveToTargetPosGoal(PathAwareEntity mob, double speed, int range) {
		this(mob, speed, range, 1);
	}

	public MoveToTargetPosGoal(PathAwareEntity mob, double speed, int range, int maxYDifference) {
		this.mob = mob;
		this.speed = speed;
		this.range = range;
		this.lowestY = 0;
		this.maxYDifference = maxYDifference;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
	}

	@Override
	public boolean canStart() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		cooldown = getInterval(mob);
		return findTargetPos();
	}

	protected int getInterval(PathAwareEntity mob) {
		return toGoalTicks(MIN_INTERVAL + mob.getRandom().nextInt(MIN_INTERVAL));
	}

	@Override
	public boolean shouldContinue() {
		return tryingTime >= -safeWaitingTime
			&& tryingTime <= MIN_WAITING_TIME
			&& isTargetPos(mob.getEntityWorld(), targetPos);
	}

	@Override
	public void start() {
		startMovingToTarget();
		tryingTime = 0;
		safeWaitingTime = mob.getRandom().nextInt(mob.getRandom().nextInt(MIN_WAITING_TIME) + MIN_WAITING_TIME) + MIN_WAITING_TIME;
	}

	protected void startMovingToTarget() {
		mob.getNavigation().startMovingTo(
			targetPos.getX() + 0.5,
			targetPos.getY() + 1,
			targetPos.getZ() + 0.5,
			speed
		);
	}

	public double getDesiredDistanceToTarget() {
		return 1.0;
	}

	protected BlockPos getTargetPos() {
		return targetPos.up();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		BlockPos pos = getTargetPos();

		if (!pos.isWithinDistance(mob.getEntityPos(), getDesiredDistanceToTarget())) {
			reached = false;
			tryingTime++;

			if (shouldResetPath()) {
				mob.getNavigation().startMovingTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
			}
		} else {
			reached = true;
			tryingTime--;
		}
	}

	public boolean shouldResetPath() {
		return tryingTime % 40 == 0;
	}

	protected boolean hasReached() {
		return reached;
	}

	/**
	 * Спиральный поиск подходящей позиции блока в радиусе {@code range} и диапазоне
	 * высот от {@code lowestY} до {@code maxYDifference}. Возвращает {@code true},
	 * если позиция найдена и сохранена в {@link #targetPos}.
	 */
	protected boolean findTargetPos() {
		BlockPos origin = mob.getBlockPos();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int dy = lowestY; dy <= maxYDifference; dy = dy > 0 ? -dy : 1 - dy) {
			for (int radius = 0; radius < range; radius++) {
				for (int dx = 0; dx <= radius; dx = dx > 0 ? -dx : 1 - dx) {
					for (int dz = dx < radius && dx > -radius ? radius : 0; dz <= radius; dz = dz > 0 ? -dz : 1 - dz) {
						mutable.set(origin, dx, dy - 1, dz);

						if (mob.isInPositionTargetRange(mutable) && isTargetPos(mob.getEntityWorld(), mutable)) {
							targetPos = mutable;
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	protected abstract boolean isTargetPos(WorldView world, BlockPos pos);
}
