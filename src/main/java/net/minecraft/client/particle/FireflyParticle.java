package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица светлячка — медленно парящая светящаяся точка, появляющаяся
 * в мангровых болотах. Плавно появляется и исчезает (fade in/out),
 * меняет направление движения каждые ~20 тиков. Живёт 200–300 тиков.
 */
@Environment(EnvType.CLIENT)
public class FireflyParticle extends BillboardParticle {

	private static final float FADE_OUT_THRESHOLD = 0.3F;
	private static final float FADE_IN_THRESHOLD = 0.1F;
	private static final float MAX_ALPHA = 0.5F;
	private static final float MIN_ALPHA = 0.3F;
	private static final int MIN_MAX_AGE = 200;
	private static final int MAX_MAX_AGE = 300;
	private static final float VELOCITY_CHANGE_CHANCE = 0.95F;
	private static final float VELOCITY_RANGE = 0.1F;
	private static final float VELOCITY_HALF = VELOCITY_RANGE / 2.0F;

	FireflyParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, sprite);
		this.ascending = true;
		this.velocityMultiplier = 0.96F;
		this.scale *= 0.75F;
		this.velocityY *= 0.8F;
		this.velocityX *= 0.8F;
		this.velocityZ *= 0.8F;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public int getBrightness(float tint) {
		return (int) (255.0F * computeFadeAlpha(normalizeAge(this.age + tint), FADE_IN_THRESHOLD, FADE_OUT_THRESHOLD));
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z)).isAir()) {
			this.markDead();
			return;
		}

		this.setAlpha(computeFadeAlpha(normalizeAge(this.age), FADE_OUT_THRESHOLD, MAX_ALPHA));

		if (this.random.nextFloat() > VELOCITY_CHANGE_CHANCE || this.age == 1) {
			this.setVelocity(
					-VELOCITY_HALF + FADE_IN_THRESHOLD * this.random.nextFloat(),
					-VELOCITY_HALF + FADE_IN_THRESHOLD * this.random.nextFloat(),
					-VELOCITY_HALF + FADE_IN_THRESHOLD * this.random.nextFloat()
			);
		}
	}

	private float normalizeAge(float age) {
		return MathHelper.clamp(age / this.maxAge, 0.0F, 1.0F);
	}

	/**
	 * Вычисляет коэффициент прозрачности с плавным появлением и исчезновением.
	 *
	 * @param normalizedAge нормализованный возраст [0..1]
	 * @param fadeInEnd     порог окончания появления
	 * @param fadeOutStart  порог начала исчезновения (от конца жизни)
	 */
	private static float computeFadeAlpha(float normalizedAge, float fadeInEnd, float fadeOutStart) {
		if (normalizedAge >= 1.0F - fadeOutStart) {
			return (1.0F - normalizedAge) / fadeOutStart;
		}

		return normalizedAge <= fadeInEnd ? normalizedAge / fadeInEnd : 1.0F;
	}

	/**
	 * Фабрика для создания частиц светлячков со случайным направлением движения.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
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
			FireflyParticle particle = new FireflyParticle(
					world,
					x,
					y,
					z,
					0.5 - random.nextDouble(),
					random.nextBoolean() ? velocityY : -velocityY,
					0.5 - random.nextDouble(),
					this.spriteProvider.getSprite(random)
			);
			particle.setMaxAge(random.nextBetween(MIN_MAX_AGE, MAX_MAX_AGE));
			particle.scale(1.5F);
			particle.setAlpha(0.0F);
			return particle;
		}
	}
}
