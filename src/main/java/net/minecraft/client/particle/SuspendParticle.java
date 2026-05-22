package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Взвешенная частица: медленно дрейфует в пространстве без гравитации,
 * не взаимодействует с блоками. Используется для различных атмосферных
 * эффектов: дельфины, счастливые жители, мицелий, треск яйца.
 */
@Environment(EnvType.CLIENT)
public class SuspendParticle extends BillboardParticle {

	private static final float VELOCITY_DAMPING = 0.99F;

	SuspendParticle(
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
		float grayShade = this.random.nextFloat() * 0.1F + 0.2F;
		this.red = grayShade;
		this.green = grayShade;
		this.blue = grayShade;
		this.setBoundingBoxSpacing(0.02F, 0.02F);
		this.scale = this.scale * (this.random.nextFloat() * 0.6F + 0.5F);
		this.velocityX *= 0.02F;
		this.velocityY *= 0.02F;
		this.velocityZ *= 0.02F;
		this.maxAge = (int) (20.0 / (this.random.nextFloat() * 0.8 + 0.2));
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
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (this.maxAge-- <= 0) {
			this.markDead();
			return;
		}

		this.move(this.velocityX, this.velocityY, this.velocityZ);
		this.velocityX *= VELOCITY_DAMPING;
		this.velocityY *= VELOCITY_DAMPING;
		this.velocityZ *= VELOCITY_DAMPING;
	}

	/** Фабрика для частиц дельфина: синеватые, полупрозрачные, короткоживущие. */
	@Environment(EnvType.CLIENT)
	public static class DolphinFactory implements ParticleFactory<SimpleParticleType> {

		private static final float DOLPHIN_RED = 0.3F;
		private static final float DOLPHIN_GREEN = 0.5F;
		private static final float DOLPHIN_BLUE = 1.0F;

		private final SpriteProvider spriteProvider;

		public DolphinFactory(SpriteProvider spriteProvider) {
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
			SuspendParticle particle = new SuspendParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
			particle.setColor(DOLPHIN_RED, DOLPHIN_GREEN, DOLPHIN_BLUE);
			particle.setAlpha(1.0F - random.nextFloat() * 0.7F);
			particle.setMaxAge(particle.getMaxAge() / 2);
			return particle;
		}
	}

	/** Фабрика для частиц треска яйца: белые. */
	@Environment(EnvType.CLIENT)
	public static class EggCrackFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public EggCrackFactory(SpriteProvider spriteProvider) {
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
			SuspendParticle particle = new SuspendParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
			particle.setColor(1.0F, 1.0F, 1.0F);
			return particle;
		}
	}

	/** Стандартная фабрика: белые частицы с коротким временем жизни 3–7 тиков. */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final int MIN_LIFETIME = 3;
		private static final int LIFETIME_VARIANCE = 5;

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
			SuspendParticle particle = new SuspendParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
			particle.setColor(1.0F, 1.0F, 1.0F);
			particle.setMaxAge(MIN_LIFETIME + world.getRandom().nextInt(LIFETIME_VARIANCE));
			return particle;
		}
	}

	/** Фабрика для частиц счастливого жителя: белые. */
	@Environment(EnvType.CLIENT)
	public static class HappyVillagerFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public HappyVillagerFactory(SpriteProvider spriteProvider) {
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
			SuspendParticle particle = new SuspendParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
			particle.setColor(1.0F, 1.0F, 1.0F);
			return particle;
		}
	}

	/** Фабрика для частиц мицелия: серые, без изменения цвета. */
	@Environment(EnvType.CLIENT)
	public static class MyceliumFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public MyceliumFactory(SpriteProvider spriteProvider) {
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
			return new SuspendParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
		}
	}
}
