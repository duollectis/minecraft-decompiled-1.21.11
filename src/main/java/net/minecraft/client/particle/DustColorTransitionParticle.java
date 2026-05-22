package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Частица пыли с плавным переходом цвета — анимированная пылинка,
 * интерполирующая RGB-цвет от начального к конечному за время своей жизни.
 * Используется для эффектов заряженных блоков (Sculk Catalyst и др.).
 */
@Environment(EnvType.CLIENT)
public class DustColorTransitionParticle extends AbstractDustParticle<DustColorTransitionParticleEffect> {

	private final Vector3f startColor;
	private final Vector3f endColor;

	protected DustColorTransitionParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			DustColorTransitionParticleEffect parameters,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, parameters, spriteProvider);
		float darkenFactor = this.random.nextFloat() * 0.4F + 0.6F;
		this.startColor = darken(parameters.getFromColor(), darkenFactor);
		this.endColor = darken(parameters.getToColor(), darkenFactor);
	}

	private Vector3f darken(Vector3f color, float multiplier) {
		return new Vector3f(
				darken(color.x(), multiplier),
				darken(color.y(), multiplier),
				darken(color.z(), multiplier)
		);
	}

	private void updateColor(float tickProgress) {
		float lifeRatio = (this.age + tickProgress) / (this.maxAge + 1.0F);
		Vector3f interpolated = new Vector3f(this.startColor).lerp(this.endColor, lifeRatio);
		this.red = interpolated.x();
		this.green = interpolated.y();
		this.blue = interpolated.z();
	}

	@Override
	public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
		this.updateColor(tickProgress);
		super.render(submittable, camera, tickProgress);
	}

	/**
	 * Фабрика для создания частиц пыли с переходом цвета.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<DustColorTransitionParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				DustColorTransitionParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new DustColorTransitionParticle(world, x, y, z, velocityX, velocityY, velocityZ, effect, this.spriteProvider);
		}
	}
}
