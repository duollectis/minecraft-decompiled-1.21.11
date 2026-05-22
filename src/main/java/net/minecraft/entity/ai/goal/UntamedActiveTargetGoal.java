package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.TameableEntity;
import org.jspecify.annotations.Nullable;

/**
 * Цель активного поиска цели, работающая только пока существо не приручено.
 * Если задан {@code targetPredicate}, он используется вместо стандартной проверки {@code shouldContinue}.
 */
public class UntamedActiveTargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {

	private final TameableEntity tameable;

	public UntamedActiveTargetGoal(
			TameableEntity tameable,
			Class<T> targetClass,
			boolean checkVisibility,
			TargetPredicate.@Nullable EntityPredicate targetPredicate
	) {
		super(tameable, targetClass, 10, checkVisibility, false, targetPredicate);
		this.tameable = tameable;
	}

	@Override
	public boolean canStart() {
		return !tameable.isTamed() && super.canStart();
	}

	@Override
	public boolean shouldContinue() {
		return targetPredicate != null
				? targetPredicate.test(getServerWorld(mob), mob, targetEntity)
				: super.shouldContinue();
	}
}
