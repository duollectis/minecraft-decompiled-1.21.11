package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица пылевого шлейфа — серо-бежевый клуб пыли, поднимающийся вверх
 * при разрушении блоков в пустынных биомах. Гравитация и множитель скорости
 * экспоненциально затухают каждый тик, создавая эффект рассеивания.
 */
@Environment(EnvType.CLIENT)
public class DustPlumeParticle extends AscendingParticle {

	// Базовый цвет пыли: #BAB282 (бежево-серый)
	private static final int BASE_COLOR = 12235202;
	private static final float GRAVITY_DECAY = 0.88F;
	private static final float VELOCITY_DECAY = 0.92F;

	protected DustPlumeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			float scaleMultiplier,
			SpriteProvider spriteProvider
	) {
		super(
				world,
				x,
				y,
				z,
				0.7F,
				0.6F,
				0.7F,
				velocityX,
				velocityY + 0.15F,
				velocityZ,
				scaleMultiplier,
				spriteProvider,
				0.5F,
				7,
				0.5F,
				false
		);
		float randomDarken = this.random.nextFloat() * 0.2F;
		this.red = ColorHelper.getRed(BASE_COLOR) / 255.0F - randomDarken;
		this.green = ColorHelper.getGreen(BASE_COLOR) / 255.0F - randomDarken;
		this.blue = ColorHelper.getBlue(BASE_COLOR) / 255.0F - randomDarken;
	}

	@Override
	public void tick() {
		this.gravityStrength = GRAVITY_DECAY * this.gravityStrength;
		this.velocityMultiplier = VELOCITY_DECAY * this.velocityMultiplier;
		super.tick();
	}

	/**
	 * Фабрика для создания частиц пылевого шлейфа с масштабом 1.0.
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
			return new DustPlumeParticle(world, x, y, z, velocityX, velocityY, velocityZ, 1.0F, this.spriteProvider);
		}
	}
}
