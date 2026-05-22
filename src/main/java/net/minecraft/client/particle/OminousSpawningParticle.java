package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица зловещего спавна: движется от стартовой точки к центру по убывающей
 * траектории, плавно меняя цвет от {@code fromColor} к {@code toColor}.
 * Используется для визуального эффекта появления мобов из Ominous Trial Spawner.
 * Не взаимодействует с миром (коллизии отключены), имеет полную яркость.
 */
@Environment(EnvType.CLIENT)
public class OminousSpawningParticle extends BillboardParticle {

	private static final float SCALE_BASE = 0.1F;
	private static final float SCALE_VARIANCE = 0.5F;
	private static final float SCALE_MIN = 0.2F;
	private static final int MIN_LIFETIME = 25;
	private static final int LIFETIME_VARIANCE = 5;
	private static final int FULL_BRIGHTNESS = 240;

	private final double startX;
	private final double startY;
	private final double startZ;
	private final int fromColor;
	private final int toColor;

	OminousSpawningParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			int fromColor,
			int toColor,
			Sprite sprite
	) {
		super(world, x, y, z, sprite);
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.startX = x;
		this.startY = y;
		this.startZ = z;
		this.lastX = x + velocityX;
		this.lastY = y + velocityY;
		this.lastZ = z + velocityZ;
		this.x = this.lastX;
		this.y = this.lastY;
		this.z = this.lastZ;
		this.scale = SCALE_BASE * (this.random.nextFloat() * SCALE_VARIANCE + SCALE_MIN);
		this.collidesWithWorld = false;
		this.maxAge = (int) (this.random.nextFloat() * LIFETIME_VARIANCE) + MIN_LIFETIME;
		this.fromColor = fromColor;
		this.toColor = toColor;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void move(double dx, double dy, double dz) {
		// Частица движется по заданной траектории, игнорируя физику мира
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
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
		float remaining = 1.0F - lifeProgress;
		this.x = this.startX + this.velocityX * remaining;
		this.y = this.startY + this.velocityY * remaining;
		this.z = this.startZ + this.velocityZ * remaining;

		int blendedColor = ColorHelper.lerp(lifeProgress, this.fromColor, this.toColor);
		this.setColor(
				ColorHelper.getRed(blendedColor) / 255.0F,
				ColorHelper.getGreen(blendedColor) / 255.0F,
				ColorHelper.getBlue(blendedColor) / 255.0F
		);
		this.setAlpha(ColorHelper.getAlpha(blendedColor) / 255.0F);
	}

	/**
	 * Фабрика для создания частиц зловещего спавна. Цвет интерполируется от
	 * тёмно-фиолетового {@code 0xFF3C3C3E} до белого {@code 0xFFFFFFFF},
	 * масштаб случайно выбирается в диапазоне 3.0–5.0.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final int FROM_COLOR = -12210434; // 0xFF3C3C3E — тёмно-фиолетовый
		private static final int TO_COLOR = -1;          // 0xFFFFFFFF — белый
		private static final float MIN_SCALE = 3.0F;
		private static final float MAX_SCALE = 5.0F;

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
			OminousSpawningParticle particle = new OminousSpawningParticle(
					world, x, y, z, velocityX, velocityY, velocityZ,
					FROM_COLOR, TO_COLOR, this.spriteProvider.getSprite(random)
			);
			particle.scale(MathHelper.nextBetween(world.getRandom(), MIN_SCALE, MAX_SCALE));
			return particle;
		}
	}
}
