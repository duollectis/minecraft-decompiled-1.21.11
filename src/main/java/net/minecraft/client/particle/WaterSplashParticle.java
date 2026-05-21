package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code WaterSplashParticle}.
 */
public class WaterSplashParticle extends RainSplashParticle {

	WaterSplashParticle(
			ClientWorld clientWorld,
			double d,
			double e,
			double f,
			double g,
			double h,
			double i,
			Sprite sprite
	) {
		super(clientWorld, d, e, f, sprite);
		this.gravityStrength = 0.04F;
		if (h == 0.0 && (g != 0.0 || i != 0.0)) {
			this.velocityX = g;
			this.velocityY = 0.1;
			this.velocityZ = i;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SplashFactory}.
	 */
	public static class SplashFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public SplashFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				SimpleParticleType simpleParticleType,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			return new WaterSplashParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
		}
	}
}
