package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект регенерации (Regeneration).
 *
 * <p>Периодически восстанавливает 1 HP сущности, если здоровье не максимальное.
 * Интервал восстановления: {@code 50 >> amplifier} тиков (50, 25, 12, …).
 * При высоком уровне усиления интервал стремится к 0 — лечение каждый тик.</p>
 */
class RegenerationStatusEffect extends StatusEffect {

	/** Базовый интервал между тиками лечения (тики). */
	private static final int BASE_HEAL_INTERVAL = 50;

	protected RegenerationStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (entity.getHealth() < entity.getMaxHealth()) {
			entity.heal(1.0F);
		}

		return true;
	}

	/**
	 * Разрешает применение каждые {@code BASE_HEAL_INTERVAL >> amplifier} тиков.
	 * Если интервал равен 0 (очень высокий уровень) — применяется каждый тик.
	 */
	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = BASE_HEAL_INTERVAL >> amplifier;
		return interval > 0 ? duration % interval == 0 : true;
	}
}
