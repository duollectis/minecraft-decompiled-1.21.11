package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица звукового удара Warden'а: крупная (масштаб 1.5) анимированная
 * вспышка, живущая ровно 16 тиков. Наследует логику рендеринга от
 * {@link ExplosionLargeParticle}.
 */
@Environment(EnvType.CLIENT)
public class SonicBoomParticle extends ExplosionLargeParticle {

	private static final int LIFETIME = 16;
	private static final float SCALE = 1.5F;

	protected SonicBoomParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, spriteProvider);
		this.maxAge = LIFETIME;
		this.scale = SCALE;
		this.updateSprite(spriteProvider);
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
			return new SonicBoomParticle(world, x, y, z, velocityX, this.spriteProvider);
		}
	}
}
