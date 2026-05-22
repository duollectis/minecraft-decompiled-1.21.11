package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Визуальная частица порыва ветра от атаки Breeze-моба. Отображается как
 * анимированный спрайт из атласа частиц с полной яркостью (не зависит от
 * освещения). Существует 12–15 тиков, последовательно перебирая кадры анимации.
 */
@Environment(EnvType.CLIENT)
public class GustParticle extends BillboardParticle {

	private static final int MIN_LIFETIME = 12;
	private static final int LIFETIME_VARIANCE = 4;
	private static final float FULL_BRIGHTNESS = 15728880;

	private final SpriteProvider spriteProvider;

	protected GustParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.updateSprite(spriteProvider);
		this.maxAge = MIN_LIFETIME + this.random.nextInt(LIFETIME_VARIANCE);
		this.scale = 1.0F;
		this.setBoundingBoxSpacing(1.0F, 1.0F);
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
	public void tick() {
		if (this.age++ >= this.maxAge) {
			this.markDead();
		} else {
			this.updateSprite(this.spriteProvider);
		}
	}

	/**
	 * Стандартная фабрика для создания полноразмерной частицы порыва ветра.
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
			return new GustParticle(world, x, y, z, this.spriteProvider);
		}
	}

	/**
	 * Фабрика для создания уменьшенной (15% от стандартного размера) частицы
	 * порыва ветра — используется для мелких визуальных эффектов.
	 */
	@Environment(EnvType.CLIENT)
	public static class SmallGustFactory implements ParticleFactory<SimpleParticleType> {

		private static final float SMALL_SCALE = 0.15F;

		private final SpriteProvider spriteProvider;

		public SmallGustFactory(SpriteProvider spriteProvider) {
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
			Particle particle = new GustParticle(world, x, y, z, this.spriteProvider);
			particle.scale(SMALL_SCALE);
			return particle;
		}
	}
}
