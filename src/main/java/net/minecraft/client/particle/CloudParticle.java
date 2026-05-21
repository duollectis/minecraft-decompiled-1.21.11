package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code CloudParticle}.
 */
public class CloudParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	CloudParticle(
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
		this.velocityMultiplier = 0.96F;
		this.spriteProvider = spriteProvider;
		float f = 2.5F;
		this.velocityX *= 0.1F;
		this.velocityY *= 0.1F;
		this.velocityZ *= 0.1F;
		this.velocityX += velocityX;
		this.velocityY += velocityY;
		this.velocityZ += velocityZ;
		float g = 1.0F - this.random.nextFloat() * 0.3F;
		this.red = g;
		this.green = g;
		this.blue = g;
		this.scale *= 1.875F;
		int i = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.3));
		this.maxAge = (int) Math.max(i * 2.5F, 1.0F);
		this.collidesWithWorld = false;
		this.updateSprite(spriteProvider);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.dead) {
			this.updateSprite(this.spriteProvider);
			PlayerEntity playerEntity = this.world.getClosestPlayer(this.x, this.y, this.z, 2.0, false);
			if (playerEntity != null) {
				double d = playerEntity.getY();
				if (this.y > d) {
					this.y = this.y + (d - this.y) * 0.2;
					this.velocityY = this.velocityY + (playerEntity.getVelocity().y - this.velocityY) * 0.2;
					this.setPos(this.x, this.y, this.z);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CloudFactory}.
	 */
	public static class CloudFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public CloudFactory(SpriteProvider spriteProvider) {
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
			return new CloudParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SneezeFactory}.
	 */
	public static class SneezeFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public SneezeFactory(SpriteProvider spriteProvider) {
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
			CloudParticle cloudParticle = new CloudParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			cloudParticle.setColor(0.22F, 1.0F, 0.53F);
			cloudParticle.setAlpha(0.4F);
			return cloudParticle;
		}
	}
}
