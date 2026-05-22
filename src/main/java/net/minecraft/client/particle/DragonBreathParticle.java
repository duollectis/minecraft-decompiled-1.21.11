package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица дыхания Дракона Края — фиолетово-пурпурный клуб дыма,
 * испускаемый при атаке Эндер-дракона. После касания земли начинает
 * медленно подниматься вверх, имитируя растекание яда.
 */
@Environment(EnvType.CLIENT)
public class DragonBreathParticle extends BillboardParticle {

	// Диапазон цветов: от тёмно-фиолетового (0xB700D2) до светло-фиолетового (0xDF00F9)
	private static final float MIN_RED = 0.7176471F;
	private static final float MAX_RED = 0.8745098F;
	private static final float MIN_BLUE = 0.8235294F;
	private static final float MAX_BLUE = 0.9764706F;
	private static final float SCALE_FACTOR = 0.75F;
	private static final float SIZE_RAMP_FACTOR = 32.0F;
	private static final double GROUND_RISE_SPEED = 0.002;
	private static final float HORIZONTAL_SPREAD = 1.1F;

	private boolean reachedGround;
	private final SpriteProvider spriteProvider;

	DragonBreathParticle(
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
		this.velocityMultiplier = 0.96F;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		// Зелёный канал всегда 0 — дыхание дракона не содержит зелёного
		this.red = MathHelper.nextFloat(this.random, MIN_RED, MAX_RED);
		this.green = 0.0F;
		this.blue = MathHelper.nextFloat(this.random, MIN_BLUE, MAX_BLUE);
		this.scale *= SCALE_FACTOR;
		this.maxAge = (int) (20.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.reachedGround = false;
		this.collidesWithWorld = false;
		this.spriteProvider = spriteProvider;
		this.updateSprite(spriteProvider);
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

		this.updateSprite(this.spriteProvider);

		if (this.onGround) {
			this.velocityY = 0.0;
			this.reachedGround = true;
		}

		if (this.reachedGround) {
			this.velocityY += GROUND_RISE_SPEED;
		}

		this.move(this.velocityX, this.velocityY, this.velocityZ);

		// Если частица застряла по Y — расширяем горизонтальное движение
		if (this.y == this.lastY) {
			this.velocityX *= HORIZONTAL_SPREAD;
			this.velocityZ *= HORIZONTAL_SPREAD;
		}

		this.velocityX *= this.velocityMultiplier;
		this.velocityZ *= this.velocityMultiplier;

		if (this.reachedGround) {
			this.velocityY *= this.velocityMultiplier;
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * SIZE_RAMP_FACTOR, 0.0F, 1.0F);
	}

	/**
	 * Фабрика для создания частиц дыхания дракона.
	 * Применяет силу из {@link DragonBreathParticleEffect} для начального движения.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<DragonBreathParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				DragonBreathParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			DragonBreathParticle particle = new DragonBreathParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					this.spriteProvider
			);
			particle.move(effect.getPower());
			return particle;
		}
	}
}
