package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Крупная частица дыма от огня — увеличенная версия {@link FireSmokeParticle}
 * с масштабным коэффициентом 2.5. Используется для больших источников огня,
 * например костров и факелов в Нижнем мире.
 */
@Environment(EnvType.CLIENT)
public class LargeFireSmokeParticle extends FireSmokeParticle {

	private static final float LARGE_SCALE = 2.5F;

	protected LargeFireSmokeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, LARGE_SCALE, spriteProvider);
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
			return new LargeFireSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
		}
	}
}
