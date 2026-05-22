package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code GlowParticle}.
 */
public class GlowParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	GlowParticle(
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
		this.velocityMultiplier = 0.96F;
		this.ascending = true;
		this.spriteProvider = spriteProvider;
		this.scale *= 0.75F;
		this.collidesWithWorld = false;
		this.updateSprite(spriteProvider);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public int getBrightness(float tint) {
		float f = (this.age + tint) / this.maxAge;
		f = MathHelper.clamp(f, 0.0F, 1.0F);
		int i = super.getBrightness(tint);
		int j = i & 0xFF;
		int k = i >> 16 & 0xFF;
		j += (int) (f * 15.0F * 16.0F);
		if (j > 240) {
			j = 240;
		}

		return j | k << 16;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ElectricSparkFactory}.
	 */
	public static class ElectricSparkFactory implements ParticleFactory<SimpleParticleType> {

		private static final double velocityMultiplier = 0.25;
		private final SpriteProvider spriteProvider;

		public ElectricSparkFactory(SpriteProvider spriteProvider) {
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
			GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0, 0.0, 0.0, this.spriteProvider);
			glowParticle.setColor(1.0F, 0.9F, 1.0F);
			glowParticle.setVelocity(g * velocityMultiplier, h * velocityMultiplier, i * velocityMultiplier);
			int j = 2;
			int k = 4;
			glowParticle.setMaxAge(random.nextInt(2) + 2);
			return glowParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code GlowFactory}.
	 */
	public static class GlowFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public GlowFactory(SpriteProvider spriteProvider) {
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
			GlowParticle
					glowParticle =
					new GlowParticle(
							clientWorld,
							d,
							e,
							f,
							0.5 - random.nextDouble(),
							h,
							0.5 - random.nextDouble(),
							this.spriteProvider
					);
			if (random.nextBoolean()) {
				glowParticle.setColor(0.6F, 1.0F, 0.8F);
			}
			else {
				glowParticle.setColor(0.08F, 0.4F, 0.4F);
			}

			glowParticle.velocityY *= 0.2F;
			if (g == 0.0 && i == 0.0) {
				glowParticle.velocityX *= 0.1F;
				glowParticle.velocityZ *= 0.1F;
			}

			glowParticle.setMaxAge((int) (8.0 / (random.nextDouble() * 0.8 + 0.2)));
			return glowParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ScrapeFactory}.
	 */
	public static class ScrapeFactory implements ParticleFactory<SimpleParticleType> {

		private static final double velocityMultiplier = 0.01;
		private final SpriteProvider spriteProvider;

		public ScrapeFactory(SpriteProvider spriteProvider) {
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
			GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0, 0.0, 0.0, this.spriteProvider);
			if (random.nextBoolean()) {
				glowParticle.setColor(0.29F, 0.58F, 0.51F);
			}
			else {
				glowParticle.setColor(0.43F, 0.77F, 0.62F);
			}

			glowParticle.setVelocity(g * velocityMultiplier, h * 0.01, i * 0.01);
			int j = 10;
			int k = 40;
			glowParticle.setMaxAge(random.nextInt(30) + 10);
			return glowParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WaxOffFactory}.
	 */
	public static class WaxOffFactory implements ParticleFactory<SimpleParticleType> {

		private static final double velocityMultiplier = 0.01;
		private final SpriteProvider spriteProvider;

		public WaxOffFactory(SpriteProvider spriteProvider) {
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
			GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0, 0.0, 0.0, this.spriteProvider);
			glowParticle.setColor(1.0F, 0.9F, 1.0F);
			glowParticle.setVelocity(g * velocityMultiplier / 2.0, h * 0.01, i * 0.01 / 2.0);
			int j = 10;
			int k = 40;
			glowParticle.setMaxAge(random.nextInt(30) + 10);
			return glowParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WaxOnFactory}.
	 */
	public static class WaxOnFactory implements ParticleFactory<SimpleParticleType> {

		private static final double velocityMultiplier = 0.01;
		private final SpriteProvider spriteProvider;

		public WaxOnFactory(SpriteProvider spriteProvider) {
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
			GlowParticle glowParticle = new GlowParticle(clientWorld, d, e, f, 0.0, 0.0, 0.0, this.spriteProvider);
			glowParticle.setColor(0.91F, 0.55F, 0.08F);
			glowParticle.setVelocity(g * velocityMultiplier / 2.0, h * 0.01, i * 0.01 / 2.0);
			int j = 10;
			int k = 40;
			glowParticle.setMaxAge(random.nextInt(30) + 10);
			return glowParticle;
		}
	}
}
