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

@Environment(EnvType.CLIENT)
/**
 * {@code BiomeEffectSoundPlayer}.
 */
public class BiomeEffectSoundPlayer implements ClientPlayerTickable {

	private static final int MAX_STRENGTH = 40;
	private static final float MOOD_INCREMENT_PER_TICK = 0.001F;
	private final ClientPlayerEntity player;
	private final SoundManager soundManager;
	private final Random random;
	private final Object2ObjectArrayMap<RegistryEntry<SoundEvent>, BiomeEffectSoundPlayer.MusicLoop>
			soundLoops =
			new Object2ObjectArrayMap();
	private float moodPercentage;
	private @Nullable RegistryEntry<SoundEvent> currentLoopSound;

	public BiomeEffectSoundPlayer(ClientPlayerEntity player, SoundManager soundManager) {
		this.random = player.getEntityWorld().getRandom();
		this.player = player;
		this.soundManager = soundManager;
	}

	public float getMoodPercentage() {
		return this.moodPercentage;
	}

	@Override
	public void tick() {
		this.soundLoops.values().removeIf(MovingSoundInstance::isDone);
		World world = this.player.getEntityWorld();
		WorldEnvironmentAttributeAccess worldEnvironmentAttributeAccess = world.getEnvironmentAttributes();
		AmbientSounds
				ambientSounds =
				worldEnvironmentAttributeAccess.getAttributeValue(
						EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
						this.player.getEntityPos()
				);
		RegistryEntry<SoundEvent> registryEntry = ambientSounds.loop().orElse(null);
		if (!Objects.equals(registryEntry, this.currentLoopSound)) {
			this.currentLoopSound = registryEntry;
			this.soundLoops.values().forEach(BiomeEffectSoundPlayer.MusicLoop::fadeOut);
			if (registryEntry != null) {
				this.soundLoops.compute(
						registryEntry, (registryEntry2, loop) -> {
							if (loop == null) {
								loop = new BiomeEffectSoundPlayer.MusicLoop(registryEntry.value());
								this.soundManager.play(loop);
							}

							loop.fadeIn();
							return loop;
						}
				);
			}
		}

		for (BiomeAdditionsSound biomeAdditionsSound : ambientSounds.additions()) {
			if (this.random.nextDouble() < biomeAdditionsSound.tickChance()) {
				this.soundManager.play(PositionedSoundInstance.ambient(biomeAdditionsSound.sound().value()));
			}
		}

		ambientSounds.mood()
		             .ifPresent(
				             biomeMoodSound -> {
					             int i = biomeMoodSound.blockSearchExtent() * 2 + 1;
					             BlockPos blockPos = BlockPos.ofFloored(
							             this.player.getX() + this.random.nextInt(i)
									             - biomeMoodSound.blockSearchExtent(),
							             this.player.getEyeY() + this.random.nextInt(i)
									             - biomeMoodSound.blockSearchExtent(),
							             this.player.getZ() + this.random.nextInt(i)
									             - biomeMoodSound.blockSearchExtent()
					             );
					             int j = world.getLightLevel(LightType.SKY, blockPos);
					             if (j > 0) {
						             this.moodPercentage -= j / 15.0F * 0.001F;
					             }
					             else {
						             this.moodPercentage =
								             this.moodPercentage
										             - (float) (world.getLightLevel(LightType.BLOCK, blockPos) - 1)
										             / biomeMoodSound.tickDelay();
					             }

					             if (this.moodPercentage >= 1.0F) {
						             double d = blockPos.getX() + 0.5;
						             double e = blockPos.getY() + 0.5;
						             double f = blockPos.getZ() + 0.5;
						             double g = d - this.player.getX();
						             double h = e - this.player.getEyeY();
						             double k = f - this.player.getZ();
						             double l = Math.sqrt(g * g + h * h + k * k);
						             double m = l + biomeMoodSound.offset();
						             PositionedSoundInstance positionedSoundInstance = PositionedSoundInstance.ambient(
								             biomeMoodSound.sound().value(),
								             this.random,
								             this.player.getX() + g / l * m,
								             this.player.getEyeY() + h / l * m,
								             this.player.getZ() + k / l * m
						             );
						             this.soundManager.play(positionedSoundInstance);
						             this.moodPercentage = 0.0F;
					             }
					             else {
						             this.moodPercentage = Math.max(this.moodPercentage, 0.0F);
					             }
				             }
		             );
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code MusicLoop}.
	 */
	public static class MusicLoop extends MovingSoundInstance {

		private int delta;
		private int strength;

		public MusicLoop(SoundEvent sound) {
			super(sound, SoundCategory.AMBIENT, SoundInstance.createRandom());
			this.repeat = true;
			this.repeatDelay = 0;
			this.volume = 1.0F;
			this.relative = true;
		}

		@Override
		public void tick() {
			if (this.strength < 0) {
				this.setDone();
			}

			this.strength = this.strength + this.delta;
			this.volume = MathHelper.clamp(this.strength / 40.0F, 0.0F, 1.0F);
		}

		/**
		 * Fade out.
		 */
		public void fadeOut() {
			this.strength = Math.min(this.strength, 40);
			this.delta = -1;
		}

		/**
		 * Fade in.
		 */
		public void fadeIn() {
			this.strength = Math.max(0, this.strength);
			this.delta = 1;
		}
	}
}
