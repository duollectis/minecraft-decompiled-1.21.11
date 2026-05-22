package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица пламени — оранжево-жёлтый огонь, испускаемый горящими блоками,
 * факелами и порталами. Не сталкивается с блоками (проходит сквозь них),
 * уменьшается по мере старения и постепенно увеличивает яркость до максимума.
 */
@Environment(EnvType.CLIENT)
public class FlameParticle extends AbstractSlowingParticle {

	private static final float MAX_BLOCK_LIGHT = 240.0F;
	private static final float BLOCK_LIGHT_SCALE = 15.0F * 16.0F;

	FlameParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, sprite);
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
		float lifeRatio = (this.age + tickProgress) / this.maxAge;
		return this.scale * (1.0F - lifeRatio * lifeRatio * 0.5F);
	}

	/**
	 * Яркость пламени нарастает от окружающего освещения до максимума
	 * по мере старения частицы, имитируя разгорание огня.
	 */
	@Override
	public int getBrightness(float tint) {
		float lifeRatio = MathHelper.clamp((this.age + tint) / this.maxAge, 0.0F, 1.0F);
		int baseBrightness = super.getBrightness(tint);
		int blockLight = baseBrightness & 0xFF;
		int skyLight = baseBrightness >> 16 & 0xFF;
		blockLight += (int) (lifeRatio * BLOCK_LIGHT_SCALE);

		if (blockLight > MAX_BLOCK_LIGHT) {
			blockLight = (int) MAX_BLOCK_LIGHT;
		}

		return blockLight | skyLight << 16;
	}

	/**
	 * Фабрика для создания обычных частиц пламени.
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
			return new FlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
		}
	}

	/**
	 * Фабрика для создания уменьшенных частиц пламени (масштаб 0.5).
	 * Используется для маленьких свечей и факелов.
	 */
	@Environment(EnvType.CLIENT)
	public static class SmallFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public SmallFactory(SpriteProvider spriteProvider) {
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
			FlameParticle particle = new FlameParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					this.spriteProvider.getSprite(random)
			);
			particle.scale(0.5F);
			return particle;
		}
	}
}
