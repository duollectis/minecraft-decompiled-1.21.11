package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица стержня Края (End Rod) — светящаяся анимированная искра
 * бело-жёлтого цвета (0xF2E9C9). Не сталкивается с блоками,
 * живёт 60–71 тик и плавно меняет спрайт по анимации.
 */
@Environment(EnvType.CLIENT)
public class EndRodParticle extends AnimatedParticle {

	private static final int TARGET_COLOR = 15916745;
	private static final int BASE_MAX_AGE = 60;
	private static final int MAX_AGE_VARIANCE = 12;
	private static final float SCALE_FACTOR = 0.75F;

	EndRodParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider, 0.0125F);
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.scale *= SCALE_FACTOR;
		this.maxAge = BASE_MAX_AGE + this.random.nextInt(MAX_AGE_VARIANCE);
		this.setTargetColor(TARGET_COLOR);
		this.updateSprite(spriteProvider);
	}

	@Override
	public void move(double dx, double dy, double dz) {
		this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
		this.repositionFromBoundingBox();
	}

	/**
	 * Фабрика для создания частиц стержня Края.
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
			return new EndRodParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
		}
	}
}
