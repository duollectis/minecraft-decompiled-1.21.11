package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект поглощения урона (Absorption).
 *
 * <p>При применении устанавливает минимальный уровень поглощения в зависимости от усиления.
 * Эффект считается активным, пока у сущности есть очки поглощения.</p>
 */
class AbsorptionStatusEffect extends StatusEffect {

	/** Базовое количество очков поглощения на уровень I. */
	private static final float BASE_ABSORPTION_PER_LEVEL = 4.0F;

	protected AbsorptionStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		return entity.getAbsorptionAmount() > 0.0F;
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	/**
	 * При применении эффекта гарантирует минимальный уровень поглощения.
	 * Формула: {@code 4 * (amplifier + 1)} очков поглощения.
	 * Не уменьшает существующее поглощение, если оно уже выше.
	 */
	@Override
	public void onApplied(LivingEntity entity, int amplifier) {
		super.onApplied(entity, amplifier);
		float minAbsorption = BASE_ABSORPTION_PER_LEVEL * (amplifier + 1);
		entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), minAbsorption));
	}
}
