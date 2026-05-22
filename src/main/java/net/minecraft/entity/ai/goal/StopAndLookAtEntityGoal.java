package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;

/**
 * Расширение {@link LookAtEntityGoal}, которое также блокирует движение моба
 * на время взгляда на сущность.
 */
public class StopAndLookAtEntityGoal extends LookAtEntityGoal {

	public StopAndLookAtEntityGoal(MobEntity mob, Class<? extends LivingEntity> targetClass, float range) {
		super(mob, targetClass, range);
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}

	public StopAndLookAtEntityGoal(
		MobEntity mob,
		Class<? extends LivingEntity> targetClass,
		float range,
		float chance
	) {
		super(mob, targetClass, range, chance);
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}
}
