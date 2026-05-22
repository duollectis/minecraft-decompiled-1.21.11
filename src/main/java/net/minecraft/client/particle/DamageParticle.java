package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица урона — белая/серая искра, появляющаяся при получении урона.
 * Быстро желтеет и краснеет (зелёный и синий каналы затухают), имитируя
 * вспышку удара. Не сталкивается с миром.
 */
@Environment(EnvType.CLIENT)
public class DamageParticle extends BillboardParticle {

	private static final float VELOCITY_SCALE = 0.1F;
	private static final float IMPULSE_SCALE = 0.4F;
	private static final float SCALE_FACTOR = 0.75F;
	private static final float GREEN_DECAY = 0.96F;
	private static final float BLUE_DECAY = 0.9F;
	private static final float SIZE_RAMP_FACTOR = 32.0F;

	DamageParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double impulseX,
			double impulseY,
			double impulseZ,
			Sprite sprite
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.velocityMultiplier = 0.7F;
		this.gravityStrength = 0.5F;
		this.velocityX = this.velocityX * VELOCITY_SCALE + impulseX * IMPULSE_SCALE;
		this.velocityY = this.velocityY * VELOCITY_SCALE + impulseY * IMPULSE_SCALE;
		this.velocityZ = this.velocityZ * VELOCITY_SCALE + impulseZ * IMPULSE_SCALE;
		float grayTone = this.random.nextFloat() * 0.3F + 0.6F;
		this.red = grayTone;
		this.green = grayTone;
		this.blue = grayTone;
		this.scale *= SCALE_FACTOR;
		this.maxAge = Math.max((int) (6.0 / (this.random.nextFloat() * 0.8 + 0.6)), 1);
		this.collidesWithWorld = false;
		this.tick();
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * SIZE_RAMP_FACTOR, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		this.green *= GREEN_DECAY;
		this.blue *= BLUE_DECAY;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	/**
	 * Фабрика для стандартных частиц урона с увеличенным временем жизни (20 тиков)
	 * и небольшим вертикальным импульсом вверх.
	 */
	@Environment(EnvType.CLIENT)
	public static class DefaultFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DefaultFactory(SpriteProvider spriteProvider) {
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
			DamageParticle particle = new DamageParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY + 1.0,
					velocityZ,
					this.spriteProvider.getSprite(random)
			);
			particle.setMaxAge(20);
			return particle;
		}
	}

	/**
	 * Фабрика для частиц зачарованного удара — синеватый оттенок
	 * (красный ослаблен до 30%, зелёный до 80%).
	 */
	@Environment(EnvType.CLIENT)
	public static class EnchantedHitFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public EnchantedHitFactory(SpriteProvider spriteProvider) {
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
			DamageParticle particle = new DamageParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					this.spriteProvider.getSprite(random)
			);
			particle.red *= 0.3F;
			particle.green *= 0.8F;
			return particle;
		}
	}

	/**
	 * Базовая фабрика для частиц урона без модификаций.
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
			return new DamageParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider.getSprite(random));
		}
	}
}
