package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Крупная частица взрыва — анимированный огненный шар, отображаемый
 * при взрывах (TNT, крипер, заряды). Всегда рендерится с максимальной
 * яркостью (full bright), живёт 6–9 тиков.
 */
@Environment(EnvType.CLIENT)
public class ExplosionLargeParticle extends BillboardParticle {

	private static final int FULL_BRIGHTNESS = 15728880;

	private final SpriteProvider spriteProvider;

	protected ExplosionLargeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double sizeMultiplier,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.maxAge = 6 + this.random.nextInt(4);
		float brightness = this.random.nextFloat() * 0.6F + 0.4F;
		this.red = brightness;
		this.green = brightness;
		this.blue = brightness;
		this.scale = 2.0F * (1.0F - (float) sizeMultiplier * 0.5F);
		this.spriteProvider = spriteProvider;
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

	/**
	 * Фабрика для создания крупных частиц взрыва.
	 * Параметр {@code velocityX} используется как множитель размера частицы.
	 */
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
			return new ExplosionLargeParticle(world, x, y, z, velocityX, this.spriteProvider);
		}
	}
}
