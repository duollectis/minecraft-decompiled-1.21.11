package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица души: анимированная полупрозрачная частица, поднимающаяся вверх.
 * Используется для эффектов душ на блоках из Нижнего мира (soul sand, soul soil).
 * Вариант {@link SculkSoulFactory} создаёт частицу с полной яркостью (для скалка).
 */
@Environment(EnvType.CLIENT)
public class SoulParticle extends AbstractSlowingParticle {

	private static final float INITIAL_SCALE = 1.5F;
	private static final int FULL_BRIGHTNESS = 240;

	private final SpriteProvider spriteProvider;
	protected boolean sculk;

	SoulParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.scale(INITIAL_SCALE);
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return this.sculk ? FULL_BRIGHTNESS : super.getBrightness(tint);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
	}

	/** Фабрика для обычной частицы души (зависит от освещения). */
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
			SoulParticle particle = new SoulParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
			particle.setAlpha(1.0F);
			return particle;
		}
	}

	/** Фабрика для частицы души скалка (всегда полная яркость). */
	@Environment(EnvType.CLIENT)
	public static class SculkSoulFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public SculkSoulFactory(SpriteProvider spriteProvider) {
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
			SoulParticle particle = new SoulParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
			particle.setAlpha(1.0F);
			particle.sculk = true;
			return particle;
		}
	}
}
