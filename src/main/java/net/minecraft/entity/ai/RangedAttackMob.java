package net.minecraft.entity.ai;

import net.minecraft.entity.LivingEntity;

/**
 * {@code RangedAttackMob}.
 */
public interface RangedAttackMob {

	void shootAt(LivingEntity target, float pullProgress);
}
