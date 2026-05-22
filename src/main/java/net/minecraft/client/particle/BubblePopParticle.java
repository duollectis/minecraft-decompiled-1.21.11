package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица лопающегося пузыря воды. Проигрывает анимацию из 4 кадров
 * за 4 тика, после чего исчезает. Используется при выходе пузырей на поверхность воды.
 */
@Environment(EnvType.CLIENT)
public class BubblePopParticle extends BillboardParticle {

	private static final int MAX_AGE = 4;

	private final SpriteProvider spriteProvider;

	BubblePopParticle(
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
		this.spriteProvider = spriteProvider;
		this.maxAge = MAX_AGE;
		this.gravityStrength = 0.008F;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.updateSprite(spriteProvider);
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
			this.velocityY = this.velocityY - this.gravityStrength;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			this.updateSprite(this.spriteProvider);
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	/**
	 * Фабрика для создания частиц лопающихся пузырей.
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
			return new BubblePopParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
		}
	}
}
