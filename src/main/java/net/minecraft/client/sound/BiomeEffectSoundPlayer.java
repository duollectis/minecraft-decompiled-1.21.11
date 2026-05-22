package net.minecraft.client.sound;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.ClientPlayerTickable;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BiomeAdditionsSound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Воспроизводит фоновые звуки биома для клиентского игрока:
 * зацикленные звуки окружения, случайные дополнительные звуки и звуки настроения.
 * Звук настроения накапливается в зависимости от уровня освещённости
 * и воспроизводится в случайной точке вокруг игрока при достижении 100%.
 */
@Environment(EnvType.CLIENT)
public class BiomeEffectSoundPlayer implements ClientPlayerTickable {

	private static final int MAX_STRENGTH = 40;
	private static final float MOOD_INCREMENT_PER_TICK = 0.001F;

	private final ClientPlayerEntity player;
	private final SoundManager soundManager;
	private final Random random;
	private final Object2ObjectArrayMap<RegistryEntry<SoundEvent>, BiomeEffectSoundPlayer.MusicLoop> soundLoops =
		new Object2ObjectArrayMap<>();
	private float moodPercentage;
	private @Nullable RegistryEntry<SoundEvent> currentLoopSound;

	public BiomeEffectSoundPlayer(ClientPlayerEntity player, SoundManager soundManager) {
		this.random = player.getEntityWorld().getRandom();
		this.player = player;
		this.soundManager = soundManager;
	}

	public float getMoodPercentage() {
		return moodPercentage;
	}

	@Override
	public void tick() {
		soundLoops.values().removeIf(MovingSoundInstance::isDone);

		World world = player.getEntityWorld();
		WorldEnvironmentAttributeAccess environmentAttributes = world.getEnvironmentAttributes();
		AmbientSounds ambientSounds = environmentAttributes.getAttributeValue(
			EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
			player.getEntityPos()
		);

		RegistryEntry<SoundEvent> loopSound = ambientSounds.loop().orElse(null);
		if (Objects.equals(loopSound, currentLoopSound) == false) {
			currentLoopSound = loopSound;
			soundLoops.values().forEach(BiomeEffectSoundPlayer.MusicLoop::fadeOut);

			if (loopSound != null) {
				soundLoops.compute(
					loopSound, (entry, loop) -> {
						if (loop == null) {
							loop = new BiomeEffectSoundPlayer.MusicLoop(entry.value());
							soundManager.play(loop);
						}

						loop.fadeIn();
						return loop;
					}
				);
			}
		}

		for (BiomeAdditionsSound additionsSound : ambientSounds.additions()) {
			if (random.nextDouble() < additionsSound.tickChance()) {
				soundManager.play(PositionedSoundInstance.ambient(additionsSound.sound().value()));
			}
		}

		ambientSounds.mood().ifPresent(moodSound -> {
			int searchDiameter = moodSound.blockSearchExtent() * 2 + 1;
			BlockPos searchPos = BlockPos.ofFloored(
				player.getX() + random.nextInt(searchDiameter) - moodSound.blockSearchExtent(),
				player.getEyeY() + random.nextInt(searchDiameter) - moodSound.blockSearchExtent(),
				player.getZ() + random.nextInt(searchDiameter) - moodSound.blockSearchExtent()
			);

			int skyLight = world.getLightLevel(LightType.SKY, searchPos);
			if (skyLight > 0) {
				moodPercentage -= skyLight / 15.0F * MOOD_INCREMENT_PER_TICK;
			} else {
				moodPercentage -= (float) (world.getLightLevel(LightType.BLOCK, searchPos) - 1)
					/ moodSound.tickDelay();
			}

			if (moodPercentage >= 1.0F) {
				double dx = searchPos.getX() + 0.5 - player.getX();
				double dy = searchPos.getY() + 0.5 - player.getEyeY();
				double dz = searchPos.getZ() + 0.5 - player.getZ();
				double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
				double offset = distance + moodSound.offset();

				PositionedSoundInstance soundInstance = PositionedSoundInstance.ambient(
					moodSound.sound().value(),
					random,
					player.getX() + dx / distance * offset,
					player.getEyeY() + dy / distance * offset,
					player.getZ() + dz / distance * offset
				);
				soundManager.play(soundInstance);
				moodPercentage = 0.0F;
			} else {
				moodPercentage = Math.max(moodPercentage, 0.0F);
			}
		});
	}

	/**
	 * Зацикленный звук окружения биома с плавным нарастанием и затуханием.
	 * Сила звука изменяется на {@code delta} за каждый тик.
	 */
	@Environment(EnvType.CLIENT)
	public static class MusicLoop extends MovingSoundInstance {

		private int delta;
		private int strength;

		public MusicLoop(SoundEvent sound) {
			super(sound, SoundCategory.AMBIENT, SoundInstance.createRandom());
			repeat = true;
			repeatDelay = 0;
			volume = 1.0F;
			relative = true;
		}

		@Override
		public void tick() {
			if (strength < 0) {
				setDone();
				return;
			}

			strength += delta;
			volume = MathHelper.clamp(strength / (float) MAX_STRENGTH, 0.0F, 1.0F);
		}

		public void fadeOut() {
			strength = Math.min(strength, MAX_STRENGTH);
			delta = -1;
		}

		public void fadeIn() {
			strength = Math.max(0, strength);
			delta = 1;
		}
	}
}
