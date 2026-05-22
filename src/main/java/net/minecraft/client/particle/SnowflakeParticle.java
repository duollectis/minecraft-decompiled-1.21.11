package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица снежинки: медленно падает вниз с небольшим случайным горизонтальным
 * дрейфом. Анимирована через спрайт-провайдер. Цвет задаётся фабрикой
 * (холодный голубовато-белый).
 */
@Environment(EnvType.CLIENT)
public class SnowflakeParticle extends BillboardParticle {

	private static final float GRAVITY = 0.225F;
	private static final float VELOCITY_MULTIPLIER = 1.0F;
	private static final float VELOCITY_JITTER = 0.05F;
	private static final float SCALE_BASE = 0.1F;
	private static final float HORIZONTAL_DAMPING = 0.95F;
	private static final float VERTICAL_DAMPING = 0.9F;

	private final SpriteProvider spriteProvider;

	protected SnowflakeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.gravityStrength = GRAVITY;
		this.velocityMultiplier = VELOCITY_MULTIPLIER;
		this.spriteProvider = spriteProvider;
		this.velocityX = velocityX + (this.random.nextFloat() * 2.0F - 1.0F) * VELOCITY_JITTER;
		this.velocityY = velocityY + (this.random.nextFloat() * 2.0F - 1.0F) * VELOCITY_JITTER;
		this.velocityZ = velocityZ + (this.random.nextFloat() * 2.0F - 1.0F) * VELOCITY_JITTER;
		this.scale = SCALE_BASE * (this.random.nextFloat() * this.random.nextFloat() + 1.0F);
		this.maxAge = (int) (16.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 2;
		this.updateSprite(spriteProvider);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
		this.velocityX *= HORIZONTAL_DAMPING;
		this.velocityY *= VERTICAL_DAMPING;
		this.velocityZ *= HORIZONTAL_DAMPING;
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final float COLOR_RED = 0.923F;
		private static final float COLOR_GREEN = 0.964F;
		private static final float COLOR_BLUE = 0.999F;

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
			SnowflakeParticle particle = new SnowflakeParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
			particle.setColor(COLOR_RED, COLOR_GREEN, COLOR_BLUE);
			return particle;
		}
	}
}
