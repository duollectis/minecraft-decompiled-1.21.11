package net.minecraft.client.particle;

import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Контейнерный класс для всех частиц фейерверка. Содержит:
 * <ul>
 *   <li>{@link Explosion} — одиночная искра взрыва с поддержкой шлейфа и мерцания;</li>
 *   <li>{@link FireworkParticle} — невидимый оркестратор, порождающий взрывы по паттернам;</li>
 *   <li>{@link Flash} — кратковременная вспышка при взрыве;</li>
 *   <li>{@link ExplosionFactory} и {@link FlashFactory} — фабрики для регистрации в системе частиц.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class FireworksSparkParticle {

	/**
	 * Одиночная искра взрыва фейерверка. Поддерживает шлейф (trail) —
	 * порождение дочерних искр позади себя, и мерцание (flicker) —
	 * периодическое скрытие частицы.
	 */
	@Environment(EnvType.CLIENT)
	static class Explosion extends AnimatedParticle {

		private static final int BASE_MAX_AGE = 48;
		private static final int MAX_AGE_VARIANCE = 12;
		private static final float SCALE_FACTOR = 0.75F;

		private boolean trail;
		private boolean flicker;
		private final ParticleManager particleManager;
		private float targetRed;
		private float targetGreen;
		private float targetBlue;
		private boolean hasFadeColor;

		Explosion(
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				ParticleManager particleManager,
				SpriteProvider spriteProvider
		) {
			super(world, x, y, z, spriteProvider, 0.1F);
			this.velocityX = velocityX;
			this.velocityY = velocityY;
			this.velocityZ = velocityZ;
			this.particleManager = particleManager;
			this.scale *= SCALE_FACTOR;
			this.maxAge = BASE_MAX_AGE + this.random.nextInt(MAX_AGE_VARIANCE);
			this.updateSprite(spriteProvider);
		}

		public void setTrail(boolean trail) {
			this.trail = trail;
		}

		public void setFlicker(boolean flicker) {
			this.flicker = flicker;
		}

		@Override
		public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
			if (!this.flicker || this.age < this.maxAge / 3 || (this.age + this.maxAge) / 3 % 2 == 0) {
				super.render(submittable, camera, tickProgress);
			}
		}

		@Override
		public void tick() {
			super.tick();

			if (!this.trail || this.age >= this.maxAge / 2 || (this.age + this.maxAge) % 2 != 0) {
				return;
			}

			Explosion trailExplosion = new Explosion(
					this.world,
					this.x,
					this.y,
					this.z,
					0.0,
					0.0,
					0.0,
					this.particleManager,
					this.spriteProvider
			);
			trailExplosion.setAlpha(0.99F);
			trailExplosion.setColor(this.red, this.green, this.blue);
			trailExplosion.age = trailExplosion.maxAge / 2;

			if (this.hasFadeColor) {
				trailExplosion.hasFadeColor = true;
				trailExplosion.targetRed = this.targetRed;
				trailExplosion.targetGreen = this.targetGreen;
				trailExplosion.targetBlue = this.targetBlue;
			}

			trailExplosion.flicker = this.flicker;
			this.particleManager.addParticle(trailExplosion);
		}
	}

	/**
	 * Фабрика для создания одиночных искр взрыва фейерверка.
	 */
	@Environment(EnvType.CLIENT)
	public static class ExplosionFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public ExplosionFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType type,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			Explosion explosion = new Explosion(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					MinecraftClient.getInstance().particleManager,
					this.spriteProvider
			);
			explosion.setAlpha(0.99F);
			return explosion;
		}
	}

	/**
	 * Невидимый оркестратор взрывов фейерверка. Последовательно запускает
	 * взрывы по списку {@link FireworkExplosionComponent}, воспроизводит
	 * соответствующие звуки и порождает вспышки.
	 */
	@Environment(EnvType.CLIENT)
	public static class FireworkParticle extends NoRenderParticle {

		private static final double[][] CREEPER_PATTERN = {
				{0.0, 0.2},
				{0.2, 0.2},
				{0.2, 0.6},
				{0.6, 0.6},
				{0.6, 0.2},
				{0.2, 0.2},
				{0.2, 0.0},
				{0.4, 0.0},
				{0.4, -0.6},
				{0.2, -0.6},
				{0.2, -0.4},
				{0.0, -0.4}
		};
		private static final double[][] STAR_PATTERN = {
				{0.0, 1.0},
				{0.3455, 0.309},
				{0.9511, 0.309},
				{0.3795918367346939, -0.12653061224489795},
				{0.6122448979591837, -0.8040816326530612},
				{0.0, -0.35918367346938773}
		};
		private static final double FAR_DISTANCE_SQUARED = 256.0;
		private static final int LARGE_EXPLOSION_THRESHOLD = 3;
		private static final int BURST_PARTICLE_COUNT = 70;

		private int age;
		private final ParticleManager particleManager;
		private final List<FireworkExplosionComponent> explosions;
		private boolean flicker;

		public FireworkParticle(
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				ParticleManager particleManager,
				List<FireworkExplosionComponent> fireworkExplosions
		) {
			super(world, x, y, z);
			this.velocityX = velocityX;
			this.velocityY = velocityY;
			this.velocityZ = velocityZ;
			this.particleManager = particleManager;

			if (fireworkExplosions.isEmpty()) {
				throw new IllegalArgumentException("Cannot create firework starter with no explosions");
			}

			this.explosions = fireworkExplosions;
			this.maxAge = fireworkExplosions.size() * 2 - 1;

			for (FireworkExplosionComponent component : fireworkExplosions) {
				if (component.hasTwinkle()) {
					this.flicker = true;
					this.maxAge += 15;
					break;
				}
			}
		}

		@Override
		public void tick() {
			if (this.age == 0) {
				playExplosionSound();
			}

			if (this.age % 2 == 0 && this.age / 2 < this.explosions.size()) {
				triggerExplosion(this.age / 2);
			}

			this.age++;

			if (this.age > this.maxAge) {
				if (this.flicker) {
					playTwinkleSound();
				}

				this.markDead();
			}
		}

		private void playExplosionSound() {
			boolean isFar = isFar();
			boolean isLarge = this.explosions.size() >= LARGE_EXPLOSION_THRESHOLD;

			if (!isLarge) {
				for (FireworkExplosionComponent component : this.explosions) {
					if (component.shape() == FireworkExplosionComponent.Type.LARGE_BALL) {
						isLarge = true;
						break;
					}
				}
			}

			SoundEvent sound = isLarge
					? (isFar ? SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR : SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST)
					: (isFar ? SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST_FAR : SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST);

			this.world.playSoundClient(
					this.x,
					this.y,
					this.z,
					sound,
					SoundCategory.AMBIENT,
					20.0F,
					0.95F + this.random.nextFloat() * 0.1F,
					true
			);
		}

		private void playTwinkleSound() {
			boolean isFar = isFar();
			SoundEvent sound = isFar
					? SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR
					: SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE;

			this.world.playSoundClient(
					this.x,
					this.y,
					this.z,
					sound,
					SoundCategory.AMBIENT,
					20.0F,
					0.9F + this.random.nextFloat() * 0.15F,
					true
			);
		}

		private void triggerExplosion(int explosionIndex) {
			FireworkExplosionComponent component = this.explosions.get(explosionIndex);
			boolean hasTrail = component.hasTrail();
			boolean hasTwinkle = component.hasTwinkle();
			IntList colors = component.colors();
			IntList fadeColors = component.fadeColors();

			if (colors.isEmpty()) {
				colors = IntList.of(DyeColor.BLACK.getFireworkColor());
			}

			switch (component.shape()) {
				case SMALL_BALL -> this.explodeBall(0.25, 2, colors, fadeColors, hasTrail, hasTwinkle);
				case LARGE_BALL -> this.explodeBall(0.5, 4, colors, fadeColors, hasTrail, hasTwinkle);
				case STAR -> this.explodeStar(0.5, STAR_PATTERN, colors, fadeColors, hasTrail, hasTwinkle, false);
				case CREEPER -> this.explodeStar(0.5, CREEPER_PATTERN, colors, fadeColors, hasTrail, hasTwinkle, true);
				case BURST -> this.explodeBurst(colors, fadeColors, hasTrail, hasTwinkle);
			}

			int firstColor = colors.getInt(0);
			this.particleManager.addParticle(
					TintedParticleEffect.create(ParticleTypes.FLASH, firstColor),
					this.x,
					this.y,
					this.z,
					0.0,
					0.0,
					0.0
			);
		}

		private boolean isFar() {
			return MinecraftClient.getInstance()
					.gameRenderer
					.getCamera()
					.getCameraPos()
					.squaredDistanceTo(this.x, this.y, this.z) >= FAR_DISTANCE_SQUARED;
		}

		private void addExplosionParticle(
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				IntList colors,
				IntList targetColors,
				boolean trail,
				boolean flicker
		) {
			Explosion explosion = (Explosion) this.particleManager.addParticle(
					ParticleTypes.FIREWORK,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ
			);
			explosion.setTrail(trail);
			explosion.setFlicker(flicker);
			explosion.setAlpha(0.99F);
			explosion.setColor(Util.<Integer>getRandom(colors, this.random));

			if (!targetColors.isEmpty()) {
				explosion.setTargetColor(Util.<Integer>getRandom(targetColors, this.random));
			}
		}

		private void explodeBall(
				double size,
				int amount,
				IntList colors,
				IntList targetColors,
				boolean trail,
				boolean flicker
		) {
			for (int dx = -amount; dx <= amount; dx++) {
				for (int dy = -amount; dy <= amount; dy++) {
					for (int dz = -amount; dz <= amount; dz++) {
						double jitteredDy = dy + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
						double jitteredDx = dx + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
						double jitteredDz = dz + (this.random.nextDouble() - this.random.nextDouble()) * 0.5;
						double distance = Math.sqrt(
								jitteredDy * jitteredDy + jitteredDx * jitteredDx + jitteredDz * jitteredDz
						) / size + this.random.nextGaussian() * 0.05;

						this.addExplosionParticle(
								this.x,
								this.y,
								this.z,
								jitteredDy / distance,
								jitteredDx / distance,
								jitteredDz / distance,
								colors,
								targetColors,
								trail,
								flicker
						);

						if (dx != -amount && dx != amount && dy != -amount && dy != amount) {
							dz += amount * 2 - 1;
						}
					}
				}
			}
		}

		private void explodeStar(
				double size,
				double[][] pattern,
				IntList colors,
				IntList targetColors,
				boolean trail,
				boolean flicker,
				boolean keepShape
		) {
			double startX = pattern[0][0];
			double startY = pattern[0][1];
			this.addExplosionParticle(
					this.x,
					this.y,
					this.z,
					startX * size,
					startY * size,
					0.0,
					colors,
					targetColors,
					trail,
					flicker
			);

			float baseAngle = this.random.nextFloat() * (float) Math.PI;
			double angleStep = keepShape ? 0.034 : 0.34;

			for (int rotation = 0; rotation < 3; rotation++) {
				double angle = baseAngle + rotation * (float) Math.PI * angleStep;
				double prevX = startX;
				double prevY = startY;

				for (int segmentIndex = 1; segmentIndex < pattern.length; segmentIndex++) {
					double nextX = pattern[segmentIndex][0];
					double nextY = pattern[segmentIndex][1];

					for (double t = 0.25; t <= 1.0; t += 0.25) {
						double lerpedX = MathHelper.lerp(t, prevX, nextX) * size;
						double lerpedY = MathHelper.lerp(t, prevY, nextY) * size;
						double rotatedZ = lerpedX * Math.sin(angle);
						lerpedX *= Math.cos(angle);

						for (double side = -1.0; side <= 1.0; side += 2.0) {
							this.addExplosionParticle(
									this.x,
									this.y,
									this.z,
									lerpedX * side,
									lerpedY,
									rotatedZ * side,
									colors,
									targetColors,
									trail,
									flicker
							);
						}
					}

					prevX = nextX;
					prevY = nextY;
				}
			}
		}

		private void explodeBurst(IntList colors, IntList targetColors, boolean trail, boolean flicker) {
			double offsetX = this.random.nextGaussian() * 0.05;
			double offsetZ = this.random.nextGaussian() * 0.05;

			for (int particleIndex = 0; particleIndex < BURST_PARTICLE_COUNT; particleIndex++) {
				double velocityX = this.velocityX * 0.5 + this.random.nextGaussian() * 0.15 + offsetX;
				double velocityZ = this.velocityZ * 0.5 + this.random.nextGaussian() * 0.15 + offsetZ;
				double velocityY = this.velocityY * 0.5 + this.random.nextDouble() * 0.5;
				this.addExplosionParticle(
						this.x,
						this.y,
						this.z,
						velocityX,
						velocityY,
						velocityZ,
						colors,
						targetColors,
						trail,
						flicker
				);
			}
		}
	}

	/**
	 * Кратковременная вспышка при взрыве фейерверка — полупрозрачный
	 * расширяющийся диск, живущий 4 тика.
	 */
	@Environment(EnvType.CLIENT)
	public static class Flash extends BillboardParticle {

		private static final int MAX_AGE = 4;
		private static final float ALPHA_DECAY = 0.25F * 0.5F;
		private static final float SIZE_SCALE = 7.1F;

		Flash(ClientWorld world, double x, double y, double z, Sprite sprite) {
			super(world, x, y, z, sprite);
			this.maxAge = MAX_AGE;
		}

		@Override
		public BillboardParticle.RenderType getRenderType() {
			return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
		}

		@Override
		public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
			this.setAlpha(0.6F - (this.age + tickProgress - 1.0F) * ALPHA_DECAY);
			super.render(submittable, camera, tickProgress);
		}

		@Override
		public float getSize(float tickProgress) {
			return SIZE_SCALE * MathHelper.sin((this.age + tickProgress - 1.0F) * 0.25F * (float) Math.PI);
		}
	}

	/**
	 * Фабрика для создания вспышек фейерверка с цветом из {@link TintedParticleEffect}.
	 */
	@Environment(EnvType.CLIENT)
	public static class FlashFactory implements ParticleFactory<TintedParticleEffect> {

		private final SpriteProvider spriteProvider;

		public FlashFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				TintedParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			Flash flash = new Flash(world, x, y, z, this.spriteProvider.getSprite(random));
			flash.setColor(effect.getRed(), effect.getGreen(), effect.getBlue());
			flash.setAlpha(effect.getAlpha());
			return flash;
		}
	}
}
