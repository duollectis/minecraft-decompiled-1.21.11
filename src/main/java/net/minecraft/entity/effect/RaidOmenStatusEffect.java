package net.minecraft.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Эффект предзнаменования рейда (Raid Omen).
 *
 * <p>Применяется ровно на последнем тике (duration == 1).
 * Запускает рейд в позиции, сохранённой при конвертации из Bad Omen.
 * После запуска рейда эффект снимается (возвращает {@code false}).</p>
 */
class RaidOmenStatusEffect extends StatusEffect {

	protected RaidOmenStatusEffect(StatusEffectCategory category, int color, ParticleEffect particleEffect) {
		super(category, color, particleEffect);
	}

	/** Применяется только на последнем тике длительности. */
	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return duration == 1;
	}

	/**
	 * Запускает рейд в сохранённой позиции и снимает эффект.
	 *
	 * @return {@code false} при успешном запуске рейда; {@code true} если условия не выполнены
	 */
	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		if (!(entity instanceof ServerPlayerEntity player) || player.isSpectator()) {
			return true;
		}

		BlockPos raidPos = player.getStartRaidPos();
		if (raidPos == null) {
			return true;
		}

		world.getRaidManager().startRaid(player, raidPos);
		player.clearStartRaidPos();
		return false;
	}
}
