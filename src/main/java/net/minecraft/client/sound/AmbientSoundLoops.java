package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Фоновые звуковые петли, привязанные к состоянию игрока.
 * Содержит реализации для музыкальных петель и звука под водой.
 */
@Environment(EnvType.CLIENT)
public class AmbientSoundLoops {

	/**
	 * Однократная музыкальная петля, воспроизводимая пока игрок находится под водой.
	 * Останавливается при выходе игрока из воды или его удалении из мира.
	 */
	@Environment(EnvType.CLIENT)
	public static class MusicLoop extends MovingSoundInstance {

		private final ClientPlayerEntity player;

		protected MusicLoop(ClientPlayerEntity player, SoundEvent soundEvent) {
			super(soundEvent, SoundCategory.AMBIENT, SoundInstance.createRandom());
			this.player = player;
			repeat = false;
			repeatDelay = 0;
			volume = 1.0F;
			relative = true;
		}

		@Override
		public void tick() {
			if (player.isRemoved() || player.isSubmergedInWater() == false) {
				setDone();
			}
		}
	}

	/**
	 * Зацикленный звук подводного окружения с плавным нарастанием и затуханием громкости.
	 * Громкость плавно увеличивается при погружении и уменьшается при выходе из воды.
	 */
	@Environment(EnvType.CLIENT)
	public static class Underwater extends MovingSoundInstance {

		public static final int MAX_TRANSITION_TIMER = 40;

		private final ClientPlayerEntity player;
		private int transitionTimer;

		public Underwater(ClientPlayerEntity player) {
			super(SoundEvents.AMBIENT_UNDERWATER_LOOP, SoundCategory.AMBIENT, SoundInstance.createRandom());
			this.player = player;
			repeat = true;
			repeatDelay = 0;
			volume = 1.0F;
			relative = true;
		}

		@Override
		public void tick() {
			if (player.isRemoved() || transitionTimer < 0) {
				setDone();
				return;
			}

			if (player.isSubmergedInWater()) {
				transitionTimer++;
			} else {
				transitionTimer -= 2;
			}

			transitionTimer = Math.min(transitionTimer, MAX_TRANSITION_TIMER);
			volume = Math.max(0.0F, Math.min(transitionTimer / (float) MAX_TRANSITION_TIMER, 1.0F));
		}
	}
}
