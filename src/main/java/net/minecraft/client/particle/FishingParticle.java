package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица поплавка удочки — анимированный брызг воды, появляющийся
 * при забросе удочки. Проигрывает 4-кадровую анимацию и постепенно
 * увеличивает хитбокс по мере старения.
 */
@Environment(EnvType.CLIENT)
public class FishingParticle extends BillboardParticle {

	private static final int ANIMATION_FRAMES = 4;
	private static final float VELOCITY_DAMPING = 0.3F;
	private static final float TICK_DAMPING = 0.98F;

	private final SpriteProvider spriteProvider;

	FishingParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.velocityX *= VELOCITY_DAMPING;
		this.velocityY = this.random.nextFloat() * 0.2F + 0.1F;
		this.velocityZ *= VELOCITY_DAMPING;
		this.setBoundingBoxSpacing(0.01F, 0.01F);
		this.maxAge = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.updateSprite(spriteProvider);
		this.gravityStrength = 0.0F;
		// Перезаписываем скорость переданными значениями после инициализации
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		// animationAge считается от 60 назад, чтобы анимация шла в обратном порядке
		int animationAge = 60 - this.maxAge;

		if (this.maxAge-- <= 0) {
			this.markDead();
			return;
		}

		this.velocityY = this.velocityY - this.gravityStrength;
		this.move(this.velocityX, this.velocityY, this.velocityZ);
		this.velocityX *= TICK_DAMPING;
		this.velocityY *= TICK_DAMPING;
		this.velocityZ *= TICK_DAMPING;

		float boundingBoxSize = animationAge * 0.001F;
		this.setBoundingBoxSpacing(boundingBoxSize, boundingBoxSize);
		this.setSprite(this.spriteProvider.getSprite(animationAge % ANIMATION_FRAMES, ANIMATION_FRAMES));
	}

	/**
	 * Фабрика для создания частиц поплавка удочки.
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
			return new FishingParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
		}
	}
}
