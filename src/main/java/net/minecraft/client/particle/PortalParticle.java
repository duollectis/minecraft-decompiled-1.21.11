package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code PortalParticle}.
 */
public class PortalParticle extends BillboardParticle {

	private final double startX;
	private final double startY;
	private final double startZ;

	protected PortalParticle(
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
		this.velocityX = g;
		this.velocityY = h;
		this.velocityZ = i;
		this.x = d;
		this.y = e;
		this.z = f;
		this.startX = this.x;
		this.startY = this.y;
		this.startZ = this.z;
		this.scale = 0.1F * (this.random.nextFloat() * 0.2F + 0.5F);
		float j = this.random.nextFloat() * 0.6F + 0.4F;
		this.red = j * 0.9F;
		this.green = j * 0.3F;
		this.blue = j;
		this.maxAge = (int) (this.random.nextFloat() * 10.0F) + 40;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void move(double dx, double dy, double dz) {
		this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
		this.repositionFromBoundingBox();
	}

	@Override
	public float getSize(float tickProgress) {
		float f = (this.age + tickProgress) / this.maxAge;
		f = 1.0F - f;
		f *= f;
		f = 1.0F - f;
		return this.scale * f;
	}

	@Override
	public int getBrightness(float tint) {
		int i = super.getBrightness(tint);
		float f = (float) this.age / this.maxAge;
		f *= f;
		f *= f;
		int j = i & 0xFF;
		int k = i >> 16 & 0xFF;
		k += (int) (f * 15.0F * 16.0F);
		if (k > 240) {
			k = 240;
		}

		return j | k << 16;
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
			float var3 = -f + f * f * 2.0F;
			float var4 = 1.0F - var3;
			this.x = this.startX + this.velocityX * var4;
			this.y = this.startY + this.velocityY * var4 + (1.0F - f);
			this.z = this.startZ + this.velocityZ * var4;
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
			return new PortalParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
		}
	}
}
