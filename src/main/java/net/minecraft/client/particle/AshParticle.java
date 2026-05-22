package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица пепла, медленно поднимающаяся вверх в биомах с пепельной атмосферой
 * (например, в Basalt Deltas). Наследует логику подъёма от {@link AscendingParticle}.
 */
@Environment(EnvType.CLIENT)
public class AshParticle extends AscendingParticle {

	protected AshParticle(
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
				-0.1F,
				0.1F,
				velocityX,
				velocityY,
				velocityZ,
				scaleMultiplier,
				spriteProvider,
				0.5F,
				20,
				0.1F,
				false
		);
	}

	/**
	 * Фабрика для создания частиц пепла без начальной скорости.
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
			return new AshParticle(world, x, y, z, 0.0, 0.0, 0.0, 1.0F, this.spriteProvider);
		}
	}
}
