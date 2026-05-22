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

/**
 * Частица следа (trail), движущаяся к целевой точке по линейной интерполяции.
 * На каждом тике оставшееся расстояние делится на количество оставшихся тиков,
 * что обеспечивает равномерное прибытие к цели точно в момент смерти частицы.
 */
@Environment(EnvType.CLIENT)
public class TrailParticle extends BillboardParticle {

	private static final float PARTICLE_SCALE = 0.26F;
	private static final int FULL_BRIGHTNESS = 15728880;
	private static final float COLOR_JITTER_BASE = 0.875F;
	private static final float COLOR_JITTER_RANGE = 0.25F;

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
		int jitteredColor = ColorHelper.scaleRgb(
				color,
				COLOR_JITTER_BASE + random.nextFloat() * COLOR_JITTER_RANGE,
				COLOR_JITTER_BASE + random.nextFloat() * COLOR_JITTER_RANGE,
				COLOR_JITTER_BASE + random.nextFloat() * COLOR_JITTER_RANGE
		);
		this.red = ColorHelper.getRed(jitteredColor) / 255.0F;
		this.green = ColorHelper.getGreen(jitteredColor) / 255.0F;
		this.blue = ColorHelper.getBlue(jitteredColor) / 255.0F;
		this.scale = PARTICLE_SCALE;
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

		if (age++ >= maxAge) {
			this.markDead();
			return;
		}

		int remaining = maxAge - age;
		double lerpFactor = 1.0 / remaining;
		this.x = MathHelper.lerp(lerpFactor, this.x, target.getX());
		this.y = MathHelper.lerp(lerpFactor, this.y, target.getY());
		this.z = MathHelper.lerp(lerpFactor, this.z, target.getZ());
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<TrailParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				TrailParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			TrailParticle particle = new TrailParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					effect.target(),
					effect.color(),
					spriteProvider.getSprite(random)
			);
			particle.setMaxAge(effect.duration());
			return particle;
		}
	}
}
