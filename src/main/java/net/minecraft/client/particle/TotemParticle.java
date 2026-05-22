package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица эффекта тотема бессмертия (Totem of Undying).
 * Анимированная частица с зелёно-жёлтой цветовой гаммой, с редкими золотистыми вспышками.
 */
@Environment(EnvType.CLIENT)
public class TotemParticle extends AnimatedParticle {

	private static final int BASE_LIFETIME = 60;
	private static final int LIFETIME_VARIANCE = 12;
	private static final float VELOCITY_MULTIPLIER = 0.6F;
	private static final float SCALE_FACTOR = 0.75F;
	private static final float ANIMATION_SPEED = 1.25F;
	// Вероятность 1/4 для золотистого оттенка вместо зелёного
	private static final int GOLD_CHANCE = 4;

	TotemParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider, ANIMATION_SPEED);
		this.velocityMultiplier = VELOCITY_MULTIPLIER;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.scale *= SCALE_FACTOR;
		this.maxAge = BASE_LIFETIME + random.nextInt(LIFETIME_VARIANCE);
		this.updateSprite(spriteProvider);

		if (random.nextInt(GOLD_CHANCE) == 0) {
			this.setColor(
					0.6F + random.nextFloat() * 0.2F,
					0.6F + random.nextFloat() * 0.3F,
					random.nextFloat() * 0.2F
			);
		} else {
			this.setColor(
					0.1F + random.nextFloat() * 0.2F,
					0.4F + random.nextFloat() * 0.3F,
					random.nextFloat() * 0.2F
			);
		}
	}

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
			return new TotemParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
		}
	}
}
