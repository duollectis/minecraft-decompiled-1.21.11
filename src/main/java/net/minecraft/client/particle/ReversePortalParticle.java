package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Обратная частица портала: в отличие от {@link PortalParticle}, движется
 * от центра наружу с нарастающей скоростью. Крупнее стандартной (×1.5) и
 * живёт дольше (60–61 тик). Используется для эффекта выхода из портала.
 */
@Environment(EnvType.CLIENT)
public class ReversePortalParticle extends PortalParticle {

	private static final float SCALE_MULTIPLIER = 1.5F;
	private static final int BASE_LIFETIME = 60;
	private static final int LIFETIME_VARIANCE = 2;
	private static final float LIFETIME_SCALE_FACTOR = 1.5F;

	ReversePortalParticle(
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
		this.scale *= SCALE_MULTIPLIER;
		this.maxAge = (int) (this.random.nextFloat() * LIFETIME_VARIANCE) + BASE_LIFETIME;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * (1.0F - (this.age + tickProgress) / (this.maxAge * LIFETIME_SCALE_FACTOR));
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (this.age++ >= this.maxAge) {
			this.markDead();
			return;
		}

		float lifeProgress = (float) this.age / this.maxAge;
		this.x += this.velocityX * lifeProgress;
		this.y += this.velocityY * lifeProgress;
		this.z += this.velocityZ * lifeProgress;
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
			return new ReversePortalParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
		}
	}
}
