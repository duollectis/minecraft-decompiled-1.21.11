package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Эффект насыщения (Saturation).
 *
 * <p>Мгновенный эффект, восстанавливающий голод и насыщение игрока.
 * Каждый тик добавляет {@code amplifier + 1} единиц голода и 1.0 насыщения.
 * На не-игроков эффект не действует.</p>
 */
class SaturationStatusEffect extends InstantStatusEffect {

	/** Значение насыщения, добавляемое за один тик. */
	private static final float SATURATION_PER_TICK = 1.0F;

	protected SaturationStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (entity instanceof PlayerEntity player) {
			player.getHungerManager().add(amplifier + 1, SATURATION_PER_TICK);
		}

		return true;
	}
}
