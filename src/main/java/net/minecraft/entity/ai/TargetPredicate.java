package net.minecraft.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import org.jspecify.annotations.Nullable;

/**
 * Предикат для проверки допустимости цели атаки или взаимодействия.
 * Поддерживает настройку максимальной дистанции, видимости и пользовательских условий.
 * Используется в целевых AI-задачах для фильтрации кандидатов.
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

	public static TargetPredicate createAttackable() {
		return new TargetPredicate(true);
	}

	public static TargetPredicate createNonAttackable() {
		return new TargetPredicate(false);
	}

	/**
	 * Создаёт независимую копию предиката с теми же настройками.
	 * Используется для создания модифицированных вариантов без изменения оригинала.
	 */
	public TargetPredicate copy() {
		TargetPredicate copy = attackable ? createAttackable() : createNonAttackable();
		copy.baseMaxDistance = baseMaxDistance;
		copy.respectsVisibility = respectsVisibility;
		copy.useDistanceScalingFactor = useDistanceScalingFactor;
		copy.predicate = predicate;
		return copy;
	}

	public TargetPredicate setBaseMaxDistance(double baseMaxDistance) {
		this.baseMaxDistance = baseMaxDistance;
		return this;
	}

	public TargetPredicate ignoreVisibility() {
		respectsVisibility = false;
		return this;
	}

	public TargetPredicate ignoreDistanceScalingFactor() {
		useDistanceScalingFactor = false;
		return this;
	}

	public TargetPredicate setPredicate(TargetPredicate.@Nullable EntityPredicate predicate) {
		this.predicate = predicate;
		return this;
	}

	/**
	 * Проверяет, является ли {@code target} допустимой целью для {@code tester}.
	 * Последовательно применяет: проверку участия в игре, пользовательский предикат,
	 * атакуемость, дистанцию и видимость.
	 *
	 * @param world мир, в котором происходит проверка
	 * @param tester существо, выбирающее цель (может быть {@code null} для безличных проверок)
	 * @param target кандидат на роль цели
	 * @return {@code true} если цель допустима
	 */
	public boolean test(ServerWorld world, @Nullable LivingEntity tester, LivingEntity target) {
		if (tester == target) {
			return false;
		}

		if (!target.isPartOfGame()) {
			return false;
		}

		if (predicate != null && !predicate.test(target, world)) {
			return false;
		}

		if (tester == null) {
			return !attackable || (target.canTakeDamage() && world.getDifficulty() != Difficulty.PEACEFUL);
		}

		if (attackable) {
			if (!tester.canTarget(target) || !tester.canTarget(target.getType()) || tester.isTeammate(target)) {
				return false;
			}
		}

		if (baseMaxDistance > 0.0) {
			double scalingFactor = useDistanceScalingFactor ? target.getAttackDistanceScalingFactor(tester) : 1.0;
			double effectiveDistance = Math.max(baseMaxDistance * scalingFactor, MIN_DISTANCE);
			double distanceSq = tester.squaredDistanceTo(target.getX(), target.getY(), target.getZ());

			if (distanceSq > effectiveDistance * effectiveDistance) {
				return false;
			}
		}

		if (respectsVisibility && tester instanceof MobEntity mobEntity
				&& !mobEntity.getVisibilityCache().canSee(target)) {
			return false;
		}

		return true;
	}

	/** Пользовательский предикат для дополнительной фильтрации целей. */
	@FunctionalInterface
	public interface EntityPredicate {

		boolean test(LivingEntity target, ServerWorld world);
	}
}
