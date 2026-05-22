package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект голода (Hunger).
 *
 * <p>Каждый тик добавляет усталость игроку, ускоряя расход шкалы голода.
 * На не-игроков эффект не действует.</p>
 */
class HungerStatusEffect extends StatusEffect {

	/** Базовое значение усталости за тик на уровне I. */
	private static final float BASE_EXHAUSTION_PER_TICK = 0.005F;

	protected HungerStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	/**
	 * Добавляет усталость игроку. Формула: {@code 0.005 * (amplifier + 1)} за тик.
	 */
	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (entity instanceof PlayerEntity player) {
			player.addExhaustion(BASE_EXHAUSTION_PER_TICK * (amplifier + 1));
		}

		return true;
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}
}
