package net.minecraft.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import org.jspecify.annotations.Nullable;

/**
 * {@code TargetPredicate}.
 */
public class TargetPredicate {

	public static final TargetPredicate DEFAULT = createAttackable();
	private static final double MIN_DISTANCE = 2.0;
	private final boolean attackable;
	private double baseMaxDistance = -1.0;
	private boolean respectsVisibility = true;
	private boolean useDistanceScalingFactor = true;
	private TargetPredicate.@Nullable EntityPredicate predicate;

	private TargetPredicate(boolean attackable) {
		this.attackable = attackable;
	}

	/**
	 * Создаёт attackable.
	 *
	 * @return TargetPredicate — результат операции
	 */
	public static TargetPredicate createAttackable() {
		return new TargetPredicate(true);
	}

	/**
	 * Создаёт non attackable.
	 *
	 * @return TargetPredicate — результат операции
	 */
	public static TargetPredicate createNonAttackable() {
		return new TargetPredicate(false);
	}

	/**
	 * Copy.
	 *
	 * @return TargetPredicate — результат операции
	 */
	public TargetPredicate copy() {
		TargetPredicate targetPredicate = this.attackable ? createAttackable() : createNonAttackable();
		targetPredicate.baseMaxDistance = this.baseMaxDistance;
		targetPredicate.respectsVisibility = this.respectsVisibility;
		targetPredicate.useDistanceScalingFactor = this.useDistanceScalingFactor;
		targetPredicate.predicate = this.predicate;
		return targetPredicate;
	}

	public TargetPredicate setBaseMaxDistance(double baseMaxDistance) {
		this.baseMaxDistance = baseMaxDistance;
		return this;
	}

	/**
	 * Ignore visibility.
	 *
	 * @return TargetPredicate — результат операции
	 */
	public TargetPredicate ignoreVisibility() {
		this.respectsVisibility = false;
		return this;
	}

	/**
	 * Ignore distance scaling factor.
	 *
	 * @return TargetPredicate — результат операции
	 */
	public TargetPredicate ignoreDistanceScalingFactor() {
		this.useDistanceScalingFactor = false;
		return this;
	}

	public TargetPredicate setPredicate(TargetPredicate.@Nullable EntityPredicate predicate) {
		this.predicate = predicate;
		return this;
	}

	/**
	 * Test.
	 *
	 * @param world world
	 * @param tester tester
	 * @param target target
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(ServerWorld world, @Nullable LivingEntity tester, LivingEntity target) {
		if (tester == target) {
			return false;
		}
		else if (!target.isPartOfGame()) {
			return false;
		}
		else if (this.predicate != null && !this.predicate.test(target, world)) {
			return false;
		}
		else {
			if (tester == null) {
				if (this.attackable && (!target.canTakeDamage() || world.getDifficulty() == Difficulty.PEACEFUL)) {
					return false;
				}
			}
			else {
				if (this.attackable && (!tester.canTarget(target) || !tester.canTarget(target.getType())
						|| tester.isTeammate(target)
				)) {
					return false;
				}

				if (this.baseMaxDistance > 0.0) {
					double d = this.useDistanceScalingFactor ? target.getAttackDistanceScalingFactor(tester) : 1.0;
					double e = Math.max(this.baseMaxDistance * d, 2.0);
					double f = tester.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
					if (f > e * e) {
						return false;
					}
				}

				if (this.respectsVisibility && tester instanceof MobEntity mobEntity && !mobEntity
						.getVisibilityCache()
						.canSee(target)) {
					return false;
				}
			}

			return true;
		}
	}

	@FunctionalInterface
	/**
	 * {@code EntityPredicate}.
	 */
	public interface EntityPredicate {

		boolean test(LivingEntity target, ServerWorld world);
	}
}
