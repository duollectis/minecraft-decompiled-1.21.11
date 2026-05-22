package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Фаза рёва перед атакой. Дракон издаёт звук рёва и через {@code DURATION} тиков
 * переходит в {@link SittingFlamingPhase}.
 */
public class SittingAttackingPhase extends AbstractSittingPhase {

	private static final int DURATION = 40;

	private int ticks;

	public SittingAttackingPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void clientTick() {
		dragon.getEntityWorld().playSoundClient(
				dragon.getX(),
				dragon.getY(),
				dragon.getZ(),
				SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
				dragon.getSoundCategory(),
				2.5F,
				0.8F + dragon.getRandom().nextFloat() * 0.3F,
				false
		);
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (ticks++ >= DURATION) {
			dragon.getPhaseManager().setPhase(PhaseType.SITTING_FLAMING);
		}
	}

	@Override
	public void beginPhase() {
		ticks = 0;
	}

	@Override
	public PhaseType<SittingAttackingPhase> getType() {
		return PhaseType.SITTING_ATTACKING;
	}
}
