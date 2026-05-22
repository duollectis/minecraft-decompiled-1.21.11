package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.util.math.random.Random;

/**
 * Частица заряда скалка: анимированная полупрозрачная частица с полной
 * яркостью, которая вращается в соответствии с параметром {@code roll}
 * из эффекта. Используется для визуализации распространения заряда скалка
 * по поверхностям.
 */
@Environment(EnvType.CLIENT)
public class SculkChargeParticle extends BillboardParticle {

	private static final float VELOCITY_MULTIPLIER = 0.96F;
	private static final float INITIAL_SCALE = 1.5F;
	private static final int FULL_BRIGHTNESS = 240;
	private static final int MIN_LIFETIME = 8;
	private static final int LIFETIME_VARIANCE = 12;

	private final SpriteProvider spriteProvider;

	SculkChargeParticle(
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
		this.scale(INITIAL_SCALE);
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

	/**
	 * Фабрика для создания частиц заряда скалка. Устанавливает угол вращения
	 * из параметра {@code roll} эффекта и случайное время жизни 8–19 тиков.
	 */
	@Environment(EnvType.CLIENT)
	public record Factory(SpriteProvider spriteProvider) implements ParticleFactory<SculkChargeParticleEffect> {

		@Override
		public Particle createParticle(
				SculkChargeParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			SculkChargeParticle particle = new SculkChargeParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
			particle.setAlpha(1.0F);
			particle.setVelocity(velocityX, velocityY, velocityZ);
			particle.lastZRotation = effect.roll();
			particle.zRotation = effect.roll();
			particle.setMaxAge(random.nextInt(LIFETIME_VARIANCE) + MIN_LIFETIME);
			return particle;
		}
	}
}
