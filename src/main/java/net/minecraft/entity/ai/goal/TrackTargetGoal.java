package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.scoreboard.AbstractTeam;
import org.jspecify.annotations.Nullable;

/**
 * Базовая цель слежения за целью. Управляет логикой продолжения преследования:
 * проверяет видимость, дальность, команду и возможность навигации к цели.
 */
public abstract class TrackTargetGoal extends Goal {

	private static final int NAVIGATE_UNSET = 0;
	private static final int NAVIGATE_CAN = 1;
	private static final int NAVIGATE_CANNOT = 2;
	private static final double MAX_PATH_END_DISTANCE_SQ = 2.25;
	private static final int NAVIGATE_COOLDOWN_BASE = 10;
	private static final int NAVIGATE_COOLDOWN_JITTER = 5;

	protected final MobEntity mob;
	protected final boolean checkVisibility;
	private final boolean checkCanNavigate;
	private int canNavigateFlag;
	private int checkCanNavigateCooldown;
	private int timeWithoutVisibility;
	protected @Nullable LivingEntity target;
	protected int maxTimeWithoutVisibility = 60;

	public TrackTargetGoal(MobEntity mob, boolean checkVisibility) {
		this(mob, checkVisibility, false);
	}

	public TrackTargetGoal(MobEntity mob, boolean checkVisibility, boolean checkNavigable) {
		this.mob = mob;
		this.checkVisibility = checkVisibility;
		this.checkCanNavigate = checkNavigable;
	}

	@Override
	public boolean shouldContinue() {
		LivingEntity currentTarget = mob.getTarget();

		if (currentTarget == null) {
			currentTarget = target;
		}

		if (currentTarget == null) {
			return false;
		}

		if (!mob.canTarget(currentTarget)) {
			return false;
		}

		AbstractTeam mobTeam = mob.getScoreboardTeam();
		AbstractTeam targetTeam = currentTarget.getScoreboardTeam();

		if (mobTeam != null && targetTeam == mobTeam) {
			return false;
		}

		double followRange = getFollowRange();

		if (mob.squaredDistanceTo(currentTarget) > followRange * followRange) {
			return false;
		}

		if (checkVisibility) {
			if (mob.getVisibilityCache().canSee(currentTarget)) {
				timeWithoutVisibility = 0;
			}
			else if (++timeWithoutVisibility > toGoalTicks(maxTimeWithoutVisibility)) {
				return false;
			}
		}

		mob.setTarget(currentTarget);
		return true;
	}

	protected double getFollowRange() {
		return mob.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
	}

	@Override
	public void start() {
		canNavigateFlag = NAVIGATE_UNSET;
		checkCanNavigateCooldown = 0;
		timeWithoutVisibility = 0;
	}

	@Override
	public void stop() {
		mob.setTarget(null);
		target = null;
	}

	/**
	 * Проверяет, может ли моб отслеживать указанную цель с учётом предиката,
	 * дальности позиционной цели и (опционально) возможности навигации.
	 */
	protected boolean canTrack(@Nullable LivingEntity target, TargetPredicate targetPredicate) {
		if (target == null) {
			return false;
		}

		if (!targetPredicate.test(getServerWorld(mob), mob, target)) {
			return false;
		}

		if (!mob.isInPositionTargetRange(target.getBlockPos())) {
			return false;
		}

		if (checkCanNavigate) {
			if (--checkCanNavigateCooldown <= 0) {
				canNavigateFlag = NAVIGATE_UNSET;
			}

			if (canNavigateFlag == NAVIGATE_UNSET) {
				canNavigateFlag = canNavigateToEntity(target) ? NAVIGATE_CAN : NAVIGATE_CANNOT;
			}

			if (canNavigateFlag == NAVIGATE_CANNOT) {
				return false;
			}
		}

		return true;
	}

	private boolean canNavigateToEntity(LivingEntity entity) {
		checkCanNavigateCooldown = toGoalTicks(NAVIGATE_COOLDOWN_BASE + mob.getRandom().nextInt(NAVIGATE_COOLDOWN_JITTER));

		Path path = mob.getNavigation().findPathTo(entity, 0);

		if (path == null) {
			return false;
		}

		PathNode endNode = path.getEnd();

		if (endNode == null) {
			return false;
		}

		int dx = endNode.x - entity.getBlockX();
		int dz = endNode.z - entity.getBlockZ();
		return dx * dx + dz * dz <= MAX_PATH_END_DISTANCE_SQ;
	}

	public TrackTargetGoal setMaxTimeWithoutVisibility(int time) {
		maxTimeWithoutVisibility = time;
		return this;
	}
}
