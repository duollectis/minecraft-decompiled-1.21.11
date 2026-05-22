package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица портала Нижнего мира. Движется от стартовой точки к центру портала
 * по нелинейной траектории (ускоряется к концу), постепенно увеличиваясь в
 * размере. Яркость нарастает по мере приближения к порталу.
 */
@Environment(EnvType.CLIENT)
public class PortalParticle extends BillboardParticle {

	private static final float SCALE_BASE = 0.1F;
	private static final float SCALE_VARIANCE = 0.2F;
	private static final float SCALE_MIN = 0.5F;
	private static final float COLOR_VARIANCE = 0.6F;
	private static final float COLOR_MIN = 0.4F;
	private static final float RED_FACTOR = 0.9F;
	private static final float GREEN_FACTOR = 0.3F;
	private static final int MIN_LIFETIME = 40;
	private static final int LIFETIME_VARIANCE = 10;
	private static final int MAX_SKY_BRIGHTNESS = 240;
	private static final float SKY_BRIGHTNESS_SCALE = 15.0F * 16.0F;

	private final double startX;
	private final double startY;
	private final double startZ;

	protected PortalParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		super(world, x, y, z, sprite);
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.x = x;
		this.y = y;
		this.z = z;
		this.startX = x;
		this.startY = y;
		this.startZ = z;
		this.scale = SCALE_BASE * (this.random.nextFloat() * SCALE_VARIANCE + SCALE_MIN);
		float colorBase = this.random.nextFloat() * COLOR_VARIANCE + COLOR_MIN;
		this.red = colorBase * RED_FACTOR;
		this.green = colorBase * GREEN_FACTOR;
		this.blue = colorBase;
		this.maxAge = (int) (this.random.nextFloat() * LIFETIME_VARIANCE) + MIN_LIFETIME;
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
		float lifeProgress = (this.age + tickProgress) / this.maxAge;
		float invProgress = 1.0F - lifeProgress;
		float eased = 1.0F - invProgress * invProgress;
		return this.scale * eased;
	}

	@Override
	public int getBrightness(float tint) {
		int baseBrightness = super.getBrightness(tint);
		float lifeProgress = (float) this.age / this.maxAge;
		float lifeProgressQuad = lifeProgress * lifeProgress * lifeProgress * lifeProgress;
		int blockLight = baseBrightness & 0xFF;
		int skyLight = baseBrightness >> 16 & 0xFF;
		skyLight += (int) (lifeProgressQuad * SKY_BRIGHTNESS_SCALE);
		if (skyLight > MAX_SKY_BRIGHTNESS) {
			skyLight = MAX_SKY_BRIGHTNESS;
		}

		return blockLight | skyLight << 16;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (this.age++ >= this.maxAge) {
			this.markDead();
			return;
		}

		float lifeProgress = (float) this.age / this.maxAge;
		// Нелинейная траектория: частица ускоряется к концу жизни
		float easeOut = -lifeProgress + lifeProgress * lifeProgress * 2.0F;
		float remaining = 1.0F - easeOut;
		this.x = this.startX + this.velocityX * remaining;
		this.y = this.startY + this.velocityY * remaining + (1.0F - lifeProgress);
		this.z = this.startZ + this.velocityZ * remaining;
	}

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
			return new PortalParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
		}
	}
}
