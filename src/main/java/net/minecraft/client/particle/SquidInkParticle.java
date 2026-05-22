package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица чернил кальмара: анимированная сфера, которая постепенно исчезает
 * во второй половине жизни и падает вниз при нахождении в воздухе.
 * Не взаимодействует с блоками (коллизии отключены).
 *
 * <p>Фабрика {@link Factory} создаёт чёрные чернила обычного кальмара,
 * {@link GlowSquidInkFactory} — светящиеся сине-зелёные чернила светящегося кальмара.
 */
@Environment(EnvType.CLIENT)
public class SquidInkParticle extends AnimatedParticle {

	private static final float VELOCITY_MULTIPLIER = 0.92F;
	private static final float INITIAL_SCALE = 0.5F;
	private static final float LIFETIME_SCALE = 12.0F;
	private static final double AIR_GRAVITY = 0.0074;

	SquidInkParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			int color,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider, 0.0F);
		this.velocityMultiplier = VELOCITY_MULTIPLIER;
		this.scale = INITIAL_SCALE;
		this.setAlpha(1.0F);
		this.setColor(
				ColorHelper.getRedFloat(color),
				ColorHelper.getGreenFloat(color),
				ColorHelper.getBlueFloat(color)
		);
		this.maxAge = (int) (this.scale * LIFETIME_SCALE / (this.random.nextFloat() * 0.8F + 0.2F));
		this.updateSprite(spriteProvider);
		this.collidesWithWorld = false;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.dead) {
			return;
		}

		this.updateSprite(this.spriteProvider);

		if (this.age > this.maxAge / 2) {
			this.setAlpha(1.0F - ((float) this.age - this.maxAge / 2.0F) / this.maxAge);
		}

		if (this.world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z)).isAir()) {
			this.velocityY -= AIR_GRAVITY;
		}
	}

	/** Фабрика для чёрных чернил обычного кальмара. */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final int BLACK_COLOR = -16777216; // 0xFF000000

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
			return new SquidInkParticle(world, x, y, z, velocityX, velocityY, velocityZ, BLACK_COLOR, this.spriteProvider);
		}
	}

	/** Фабрика для светящихся сине-зелёных чернил светящегося кальмара. */
	@Environment(EnvType.CLIENT)
	public static class GlowSquidInkFactory implements ParticleFactory<SimpleParticleType> {

		private static final float GLOW_ALPHA = 1.0F;
		private static final float GLOW_RED = 0.2F;
		private static final float GLOW_GREEN = 0.8F;
		private static final float GLOW_BLUE = 0.6F;

		private final SpriteProvider spriteProvider;

		public GlowSquidInkFactory(SpriteProvider spriteProvider) {
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
			return new SquidInkParticle(
					world, x, y, z, velocityX, velocityY, velocityZ,
					ColorHelper.fromFloats(GLOW_ALPHA, GLOW_RED, GLOW_GREEN, GLOW_BLUE),
					this.spriteProvider
			);
		}
	}
}
