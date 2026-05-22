package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица нисходящего водного течения (bubble column downward current).
 * Движется вниз по спиральной траектории, имитируя водоворот.
 * Исчезает при выходе из воды или касании земли.
 */
@Environment(EnvType.CLIENT)
public class CurrentDownParticle extends BillboardParticle {

	private static final float SPIRAL_AMPLITUDE = 0.6F;
	private static final float ANGLE_INCREMENT = 0.08F;

	private float accelerationAngle;

	CurrentDownParticle(ClientWorld world, double x, double y, double z, Sprite sprite) {
		super(world, x, y, z, sprite);
		this.maxAge = (int) (this.random.nextFloat() * 60.0F) + 30;
		this.collidesWithWorld = false;
		this.velocityX = 0.0;
		this.velocityY = -0.05;
		this.velocityZ = 0.0;
		this.setBoundingBoxSpacing(0.02F, 0.02F);
		this.scale = this.scale * (this.random.nextFloat() * 0.6F + 0.2F);
		this.gravityStrength = 0.002F;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
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

		// Спиральное движение: добавляем горизонтальное ускорение по синусоиде
		this.velocityX = (this.velocityX + SPIRAL_AMPLITUDE * MathHelper.cos(this.accelerationAngle)) * 0.07;
		this.velocityZ = (this.velocityZ + SPIRAL_AMPLITUDE * MathHelper.sin(this.accelerationAngle)) * 0.07;
		this.move(this.velocityX, this.velocityY, this.velocityZ);

		if (!this.world.getFluidState(BlockPos.ofFloored(this.x, this.y, this.z)).isIn(FluidTags.WATER)
				|| this.onGround) {
			this.markDead();
		}

		this.accelerationAngle += ANGLE_INCREMENT;
	}

	/**
	 * Фабрика для создания частиц нисходящего течения.
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
			return new CurrentDownParticle(world, x, y, z, this.spriteProvider.getSprite(random));
		}
	}
}
