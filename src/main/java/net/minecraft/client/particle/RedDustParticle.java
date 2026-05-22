package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Частица пыли редстоуна с настраиваемым цветом. Цвет берётся из параметров
 * эффекта {@link DustParticleEffect} и случайно затемняется на 40–100%,
 * создавая естественную вариацию оттенков.
 */
@Environment(EnvType.CLIENT)
public class RedDustParticle extends AbstractDustParticle<DustParticleEffect> {

	private static final float COLOR_MIN_FACTOR = 0.6F;
	private static final float COLOR_VARIANCE = 0.4F;

	protected RedDustParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			DustParticleEffect parameters,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, parameters, spriteProvider);
		float colorFactor = this.random.nextFloat() * COLOR_VARIANCE + COLOR_MIN_FACTOR;
		Vector3f color = parameters.getColor();
		this.red = this.darken(color.x(), colorFactor);
		this.green = this.darken(color.y(), colorFactor);
		this.blue = this.darken(color.z(), colorFactor);
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<DustParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				DustParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new RedDustParticle(world, x, y, z, velocityX, velocityY, velocityZ, effect, this.spriteProvider);
		}
	}
}
