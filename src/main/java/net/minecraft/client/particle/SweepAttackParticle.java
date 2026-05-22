package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица визуального эффекта круговой атаки мечом (sweep attack).
 * Отображает анимированный спрайт, масштаб которого зависит от силы удара.
 */
@Environment(EnvType.CLIENT)
public class SweepAttackParticle extends BillboardParticle {

	private static final int LIFETIME = 4;
	private static final int FULL_BRIGHTNESS = 15728880;
	private static final float COLOR_BASE = 0.4F;
	private static final float COLOR_VARIANCE = 0.6F;

	private final SpriteProvider spriteProvider;

	SweepAttackParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double sizeReduction,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.maxAge = LIFETIME;

		float grayShade = random.nextFloat() * COLOR_VARIANCE + COLOR_BASE;
		this.red = grayShade;
		this.green = grayShade;
		this.blue = grayShade;
		this.scale = 1.0F - (float) sizeReduction * 0.5F;
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
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

		this.updateSprite(spriteProvider);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
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
			return new SweepAttackParticle(world, x, y, z, velocityX, spriteProvider);
		}
	}
}
