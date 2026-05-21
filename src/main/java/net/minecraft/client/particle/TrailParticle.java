package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.TrailParticleEffect;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code TrailParticle}.
 */
public class TrailParticle extends BillboardParticle {

	private final Vec3d target;

	TrailParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Vec3d target,
			int color,
			Sprite sprite
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, sprite);
		color = ColorHelper.scaleRgb(
				color,
				0.875F + this.random.nextFloat() * 0.25F,
				0.875F + this.random.nextFloat() * 0.25F,
				0.875F + this.random.nextFloat() * 0.25F
		);
		this.red = ColorHelper.getRed(color) / 255.0F;
		this.green = ColorHelper.getGreen(color) / 255.0F;
		this.blue = ColorHelper.getBlue(color) / 255.0F;
		this.scale = 0.26F;
		this.target = target;
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
		if (this.age++ >= this.maxAge) {
			this.markDead();
		}
		else {
			int i = this.maxAge - this.age;
			double d = 1.0 / i;
			this.x = MathHelper.lerp(d, this.x, this.target.getX());
			this.y = MathHelper.lerp(d, this.y, this.target.getY());
			this.z = MathHelper.lerp(d, this.z, this.target.getZ());
		}
	}

	@Override
	public int getBrightness(float tint) {
		return 15728880;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<TrailParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				TrailParticleEffect trailParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			TrailParticle trailParticle = new TrailParticle(
					clientWorld,
					d,
					e,
					f,
					g,
					h,
					i,
					trailParticleEffect.target(),
					trailParticleEffect.color(),
					this.spriteProvider.getSprite(random)
			);
			trailParticle.setMaxAge(trailParticleEffect.duration());
			return trailParticle;
		}
	}
}
