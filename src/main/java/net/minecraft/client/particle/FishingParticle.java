package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code FishingParticle}.
 */
public class FishingParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	FishingParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.velocityX *= 0.3F;
		this.velocityY = this.random.nextFloat() * 0.2F + 0.1F;
		this.velocityZ *= 0.3F;
		this.setBoundingBoxSpacing(0.01F, 0.01F);
		this.maxAge = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.updateSprite(spriteProvider);
		this.gravityStrength = 0.0F;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		int i = 60 - this.maxAge;
		if (this.maxAge-- <= 0) {
			this.markDead();
		}
		else {
			this.velocityY = this.velocityY - this.gravityStrength;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			this.velocityX *= 0.98F;
			this.velocityY *= 0.98F;
			this.velocityZ *= 0.98F;
			float f = i * 0.001F;
			this.setBoundingBoxSpacing(f, f);
			this.setSprite(this.spriteProvider.getSprite(i % 4, 4));
		}
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
			return new FishingParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
		}
	}
}
