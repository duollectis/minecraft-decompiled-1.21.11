package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект яда (Poison).
 *
 * <p>Периодически наносит 1 единицу магического урона, но не убивает:
 * урон применяется только если у сущности больше 1 HP.</p>
 *
 * <p>Интервал применения: {@code FLOWER_CONTACT_EFFECT_DURATION >> amplifier} тиков.
 * При высоком уровне усиления интервал стремится к 0, и эффект применяется каждый тик.</p>
 */
public class PoisonStatusEffect extends StatusEffect {

	/**
	 * Базовый интервал между тиками урона (тики).
	 * Используется также как длительность эффекта при контакте с цветком.
	 */
	public static final int FLOWER_CONTACT_EFFECT_DURATION = 25;

	protected PoisonStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (entity.getHealth() > 1.0F) {
			entity.damage(world, entity.getDamageSources().magic(), 1.0F);
		}

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
