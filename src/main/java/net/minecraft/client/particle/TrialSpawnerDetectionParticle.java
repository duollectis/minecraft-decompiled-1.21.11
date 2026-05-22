package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица обнаружения пробного спаунера (Trial Spawner Detection).
 * Поднимается вверх с анимированным спрайтом; размер нарастает в первые тики
 * через clamp-функцию, создавая эффект «появления» частицы.
 */
@Environment(EnvType.CLIENT)
public class TrialSpawnerDetectionParticle extends BillboardParticle {

	private static final int BASE_LIFETIME = 8;
	private static final float FULL_BRIGHTNESS = 240;
	private static final float VELOCITY_MULTIPLIER = 0.96F;
	private static final float GRAVITY_STRENGTH = -0.1F;
	private static final float SCALE_FACTOR = 0.75F;
	private static final float SIZE_RAMP_TICKS = 32.0F;

	private final SpriteProvider spriteProvider;

	protected TrialSpawnerDetectionParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			float scale,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.velocityMultiplier = VELOCITY_MULTIPLIER;
		this.gravityStrength = GRAVITY_STRENGTH;
		this.ascending = true;
		this.velocityX *= 0.0;
		this.velocityY *= 0.9;
		this.velocityZ *= 0.0;
		this.velocityX += velocityX;
		this.velocityY += velocityY;
		this.velocityZ += velocityZ;
		this.scale *= SCALE_FACTOR * scale;
		this.maxAge = Math.max((int) (BASE_LIFETIME / MathHelper.nextBetween(random, 0.5F, 1.0F) * scale), 1);
		this.updateSprite(spriteProvider);
		this.collidesWithWorld = true;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public int getBrightness(float tint) {
		return (int) FULL_BRIGHTNESS;
	}

	@Override
	public BillboardParticle.Rotator getRotator() {
		return BillboardParticle.Rotator.Y_AND_W_ONLY;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(spriteProvider);
	}

	@Override
	public float getSize(float tickProgress) {
		return scale * MathHelper.clamp((age + tickProgress) / maxAge * SIZE_RAMP_TICKS, 0.0F, 1.0F);
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final float DEFAULT_SCALE = 1.5F;

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
			return new TrialSpawnerDetectionParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					DEFAULT_SCALE,
					spriteProvider
			);
		}
	}
}
