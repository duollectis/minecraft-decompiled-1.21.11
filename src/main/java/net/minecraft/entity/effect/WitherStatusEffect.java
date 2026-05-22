package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект иссушения (Wither).
 *
 * <p>Периодически наносит 1 единицу урона иссушения, который, в отличие от яда,
 * может убить сущность. Интервал: {@code FLOWER_CONTACT_EFFECT_DURATION >> amplifier} тиков.
 * При высоком уровне усиления интервал стремится к 0 — урон каждый тик.</p>
 */
public class WitherStatusEffect extends StatusEffect {

	/**
	 * Базовый интервал между тиками урона (тики).
	 * Используется также как длительность эффекта при контакте с цветком иссушения.
	 */
	public static final int FLOWER_CONTACT_EFFECT_DURATION = 40;

	protected WitherStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		entity.damage(world, entity.getDamageSources().wither(), 1.0F);
		return true;
	}

	/**
	 * Разрешает применение каждые {@code FLOWER_CONTACT_EFFECT_DURATION >> amplifier} тиков.
	 * Если интервал равен 0 (очень высокий уровень) — применяется каждый тик.
	 */
	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = FLOWER_CONTACT_EFFECT_DURATION >> amplifier;
		return interval > 0 ? duration % interval == 0 : true;
	}
}
