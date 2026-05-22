package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ShriekParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;

/**
 * Частица крика Warden'а: отображается как два перекрещенных спрайта,
 * наклонённых под углом ~60° к горизонту. Поддерживает задержку перед
 * появлением (параметр {@code delay} из эффекта). Плавно исчезает к концу
 * жизни (30 тиков). Движется вертикально вверх.
 */
@Environment(EnvType.CLIENT)
public class ShriekParticle extends BillboardParticle {

	/** Угол наклона спрайта в радианах (~60°). */
	private static final float X_ROTATION = 1.0472F;
	private static final float INITIAL_SCALE = 0.85F;
	private static final int LIFETIME = 30;
	private static final float VERTICAL_VELOCITY = 0.1F;
	private static final int FULL_BRIGHTNESS = 240;

	private int delay;

	ShriekParticle(ClientWorld world, double x, double y, double z, int delay, Sprite sprite) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.scale = INITIAL_SCALE;
		this.delay = delay;
		this.maxAge = LIFETIME;
		this.gravityStrength = 0.0F;
		this.velocityX = 0.0;
		this.velocityY = VERTICAL_VELOCITY;
		this.velocityZ = 0.0;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 0.75F, 0.0F, 1.0F);
	}

	/**
	 * Рендерит два перекрещенных спрайта для создания объёмного эффекта крика.
	 * Первый спрайт наклонён вперёд, второй — назад, образуя X-образную форму.
	 */
	@Override
	public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
		if (this.delay > 0) {
			return;
		}

		this.alpha = 1.0F - MathHelper.clamp((this.age + tickProgress) / this.maxAge, 0.0F, 1.0F);
		Quaternionf rotation = new Quaternionf();
		rotation.rotationX(-X_ROTATION);
		this.render(submittable, camera, rotation, tickProgress);
		rotation.rotationYXZ((float) -Math.PI, X_ROTATION, 0.0F);
		this.render(submittable, camera, rotation, tickProgress);
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		if (this.delay > 0) {
			this.delay--;
			return;
		}

		super.tick();
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<ShriekParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				ShriekParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			ShriekParticle particle = new ShriekParticle(
					world, x, y, z,
					effect.getDelay(),
					this.spriteProvider.getSprite(random)
			);
			particle.setAlpha(1.0F);
			return particle;
		}
	}
}
