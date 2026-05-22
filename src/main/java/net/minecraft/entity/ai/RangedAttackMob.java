package net.minecraft.entity.ai;

import net.minecraft.entity.LivingEntity;

/**
 * Интерфейс для существ, способных атаковать на расстоянии (луком, снарядами и т.п.).
 * Реализуется мобами, которые стреляют в цель с заданным прогрессом натяжения.
 */
public interface RangedAttackMob {

	/**
	 * Производит дальнобойную атаку по цели.
	 *
	 * @param target цель атаки
	 * @param pullProgress прогресс натяжения/заряда от 0.0 до 1.0
	 */
	void shootAt(LivingEntity target, float pullProgress);
}
