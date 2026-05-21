package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.TameableEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code UntamedActiveTargetGoal}.
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
		return !this.tameable.isTamed() && super.canStart();
	}

	@Override
	public boolean shouldContinue() {
		return this.targetPredicate != null ? this.targetPredicate.test(
				getServerWorld(this.mob),
				this.mob,
				this.targetEntity
		) : super.shouldContinue();
	}
}
