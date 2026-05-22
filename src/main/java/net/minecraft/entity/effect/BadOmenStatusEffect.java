package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;

/**
 * Эффект Дурного предзнаменования (Bad Omen).
 *
 * <p>Каждый тик проверяет, находится ли игрок рядом с деревней в не-мирном режиме.
 * Если рейд ещё не достиг максимального уровня — конвертирует Bad Omen в Raid Omen
 * длительностью 600 тиков и запоминает позицию для старта рейда.</p>
 */
class BadOmenStatusEffect extends StatusEffect {

	/** Длительность эффекта Raid Omen, применяемого вместо Bad Omen (тики). */
	private static final int RAID_OMEN_DURATION = 600;

	protected BadOmenStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	/**
	 * Конвертирует Bad Omen в Raid Omen при входе игрока в деревню.
	 *
	 * @return {@code false} при успешной конвертации (эффект снимается),
	 *         {@code true} если условия не выполнены
	 */
	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (!(entity instanceof ServerPlayerEntity player) || player.isSpectator()) {
			return true;
		}

		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			return true;
		}

		if (!world.isNearOccupiedPointOfInterest(player.getBlockPos())) {
			return true;
		}

		Raid raid = world.getRaidAt(player.getBlockPos());

		if (raid != null && raid.getBadOmenLevel() >= raid.getMaxAcceptableBadOmenLevel()) {
			return true;
		}

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, RAID_OMEN_DURATION, amplifier));
		player.setStartRaidPos(player.getBlockPos());
		return false;
	}
}
