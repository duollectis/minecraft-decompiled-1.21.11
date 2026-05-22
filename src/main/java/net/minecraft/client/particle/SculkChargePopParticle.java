package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица «хлопка» заряда скалка: короткая анимированная вспышка (6–9 тиков),
 * появляющаяся при завершении распространения заряда. Полупрозрачная, с полной
 * яркостью, не взаимодействует с блоками.
 */
@Environment(EnvType.CLIENT)
public class SculkChargePopParticle extends BillboardParticle {

	private static final float VELOCITY_MULTIPLIER = 0.96F;
	private static final int FULL_BRIGHTNESS = 240;
	private static final int MIN_LIFETIME = 6;
	private static final int LIFETIME_VARIANCE = 4;

	private final SpriteProvider spriteProvider;

	SculkChargePopParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.getFirst());
		this.velocityMultiplier = VELOCITY_MULTIPLIER;
		this.spriteProvider = spriteProvider;
		this.scale(1.0F);
		this.collidesWithWorld = false;
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
	}

	@Environment(EnvType.CLIENT)
	public record Factory(SpriteProvider spriteProvider) implements ParticleFactory<SimpleParticleType> {

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
			SculkChargePopParticle particle = new SculkChargePopParticle(
					world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider
			);
			particle.setAlpha(1.0F);
			particle.setVelocity(velocityX, velocityY, velocityZ);
			particle.setMaxAge(random.nextInt(LIFETIME_VARIANCE) + MIN_LIFETIME);
			return particle;
		}
	}
}
