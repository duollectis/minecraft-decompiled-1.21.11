package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.sound.SoundEvent;

/**
 * Цель лошади, периодически встающей на дыбы с воспроизведением звука.
 * Вероятность активации управляется кулдауном и случайным шансом {@code STAND_CHANCE}.
 */
public class AmbientStandGoal extends Goal {

	private static final int STAND_CHANCE = 10;
	private static final int COOLDOWN_THRESHOLD = 1000;

	private final AbstractHorseEntity entity;
	private int cooldown;

	public AmbientStandGoal(AbstractHorseEntity entity) {
		this.entity = entity;
		resetCooldown(entity);
	}

	@Override
	public void start() {
		entity.updateAnger();
		playAmbientStandSound();
	}

	private void playAmbientStandSound() {
		SoundEvent sound = entity.getAmbientStandSound();
		if (sound != null) {
			entity.playSoundIfNotSilent(sound);
		}
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}

	@Override
	public boolean canStart() {
		cooldown++;
		if (cooldown > 0 && entity.getRandom().nextInt(COOLDOWN_THRESHOLD) < cooldown) {
			resetCooldown(entity);
			return !entity.isImmobile() && entity.getRandom().nextInt(STAND_CHANCE) == 0;
		}

		return false;
	}

	private void resetCooldown(AbstractHorseEntity horse) {
		cooldown = -horse.getMinAmbientStandDelay();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}
}
