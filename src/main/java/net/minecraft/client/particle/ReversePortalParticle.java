package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code ReversePortalParticle}.
 */
public class ReversePortalParticle extends PortalParticle {

	ReversePortalParticle(
			ClientWorld clientWorld,
			double d,
			double e,
			double f,
			double g,
			double h,
			double i,
			Sprite sprite
	) {
		super(clientWorld, d, e, f, g, h, i, sprite);
		this.scale *= 1.5F;
		this.maxAge = (int) (this.random.nextFloat() * 2.0F) + 60;
	}

	@Override
	public float getSize(float tickProgress) {
		float f = 1.0F - (this.age + tickProgress) / (this.maxAge * 1.5F);
		return this.scale * f;
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
			float f = (float) this.age / this.maxAge;
			this.x = this.x + this.velocityX * f;
			this.y = this.y + this.velocityY * f;
			this.z = this.z + this.velocityZ * f;
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
			return new ReversePortalParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
		}
	}
}
