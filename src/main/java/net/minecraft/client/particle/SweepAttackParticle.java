package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code SweepAttackParticle}.
 */
public class SweepAttackParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	SweepAttackParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.maxAge = 4;
		float f = this.random.nextFloat() * 0.6F + 0.4F;
		this.red = f;
		this.green = f;
		this.blue = f;
		this.scale = 1.0F - (float) velocityX * 0.5F;
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return 15728880;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		if (this.age++ >= this.maxAge) {
			this.markDead();
		}
		else {
			this.updateSprite(this.spriteProvider);
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
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
			return new SweepAttackParticle(clientWorld, d, e, f, g, this.spriteProvider);
		}
	}
}
