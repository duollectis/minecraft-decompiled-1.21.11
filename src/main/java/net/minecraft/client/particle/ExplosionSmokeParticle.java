package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица дыма взрыва — серо-белый клуб дыма, появляющийся после взрыва.
 * Поднимается вверх (отрицательная гравитация), постепенно рассеивается
 * и меняет спрайт по анимации. Живёт 18–82 тика.
 */
@Environment(EnvType.CLIENT)
public class ExplosionSmokeParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	protected ExplosionSmokeParticle(
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
		this.gravityStrength = -0.1F;
		this.velocityMultiplier = 0.9F;
		this.spriteProvider = spriteProvider;
		this.velocityX = velocityX + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.velocityY = velocityY + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.velocityZ = velocityZ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F;
		float grayTone = this.random.nextFloat() * 0.3F + 0.7F;
		this.red = grayTone;
		this.green = grayTone;
		this.blue = grayTone;
		this.scale = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 6.0F + 1.0F);
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
	}

	/**
	 * Фабрика для создания частиц дыма взрыва.
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
			return new ExplosionSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
		}
	}
}
