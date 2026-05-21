package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code SoulParticle}.
 */
public class SoulParticle extends AbstractSlowingParticle {

	private final SpriteProvider spriteProvider;
	protected boolean sculk;

	SoulParticle(
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
		this.spriteProvider = spriteProvider;
		this.scale(1.5F);
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return this.sculk ? 240 : super.getBrightness(tint);
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
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
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
			SoulParticle soulParticle = new SoulParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			soulParticle.setAlpha(1.0F);
			return soulParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SculkSoulFactory}.
	 */
	public static class SculkSoulFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public SculkSoulFactory(SpriteProvider spriteProvider) {
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
			SoulParticle soulParticle = new SoulParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			soulParticle.setAlpha(1.0F);
			soulParticle.sculk = true;
			return soulParticle;
		}
	}
}
