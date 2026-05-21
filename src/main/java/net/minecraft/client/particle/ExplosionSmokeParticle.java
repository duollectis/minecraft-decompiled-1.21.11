package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code ExplosionSmokeParticle}.
 */
public class ExplosionSmokeParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	protected ExplosionSmokeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.gravityStrength = -0.1F;
		this.velocityMultiplier = 0.9F;
		this.spriteProvider = spriteProvider;
		this.velocityX = velocityX + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.velocityY = velocityY + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.velocityZ = velocityZ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		float f = this.random.nextFloat() * 0.3F + 0.7F;
		this.red = f;
		this.green = f;
		this.blue = f;
		this.scale = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 6.0F + 1.0F);
		this.maxAge = (int) (16.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 2;
		this.updateSprite(spriteProvider);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
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
			return new ExplosionSmokeParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
		}
	}
}
