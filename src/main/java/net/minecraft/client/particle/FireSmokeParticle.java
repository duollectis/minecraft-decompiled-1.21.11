package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица дыма от огня — тёмный поднимающийся клуб дыма,
 * испускаемый горящими блоками и кострами. Наследует логику
 * подъёма и затухания от {@link AscendingParticle}.
 */
@Environment(EnvType.CLIENT)
public class FireSmokeParticle extends AscendingParticle {

	protected FireSmokeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			float scaleMultiplier,
			SpriteProvider spriteProvider
	) {
		super(
				world,
				x,
				y,
				z,
				0.1F,
				0.1F,
				0.1F,
				velocityX,
				velocityY,
				velocityZ,
				scaleMultiplier,
				spriteProvider,
				0.3F,
				8,
				-0.1F,
				true
		);
	}

	/**
	 * Фабрика для создания частиц дыма от огня с масштабом 1.0.
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
			return new FireSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ, 1.0F, this.spriteProvider);
		}
	}
}
