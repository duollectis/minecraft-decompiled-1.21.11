package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Частица пузырька воды, поднимающегося в жидкости.
 * Умирает немедленно, если покидает блок воды.
 */
@Environment(EnvType.CLIENT)
public class WaterBubbleParticle extends BillboardParticle {

	private static final float BOUNDING_BOX_SPACING = 0.02F;
	private static final float SCALE_BASE = 0.2F;
	private static final float SCALE_VARIANCE = 0.6F;
	private static final float VELOCITY_SCALE = 0.2F;
	private static final float VELOCITY_JITTER = 0.02F;
	private static final float VELOCITY_JITTER_RANGE = 2.0F;
	private static final double BUOYANCY = 0.002;
	private static final float DRAG = 0.85F;
	private static final float BASE_LIFETIME = 8.0F;
	private static final float LIFETIME_VARIANCE = 0.8F;
	private static final float LIFETIME_BASE = 0.2F;

	WaterBubbleParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		super(world, x, y, z, sprite);
		this.setBoundingBoxSpacing(BOUNDING_BOX_SPACING, BOUNDING_BOX_SPACING);
		this.scale *= random.nextFloat() * SCALE_VARIANCE + SCALE_BASE;
		this.velocityX = velocityX * VELOCITY_SCALE + (random.nextFloat() * VELOCITY_JITTER_RANGE - 1.0F) * VELOCITY_JITTER;
		this.velocityY = velocityY * VELOCITY_SCALE + (random.nextFloat() * VELOCITY_JITTER_RANGE - 1.0F) * VELOCITY_JITTER;
		this.velocityZ = velocityZ * VELOCITY_SCALE + (random.nextFloat() * VELOCITY_JITTER_RANGE - 1.0F) * VELOCITY_JITTER;
		this.maxAge = (int) (BASE_LIFETIME / (random.nextFloat() * LIFETIME_VARIANCE + LIFETIME_BASE));
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (maxAge-- <= 0) {
			this.markDead();
			return;
		}

		this.velocityY += BUOYANCY;
		this.move(velocityX, velocityY, velocityZ);
		this.velocityX *= DRAG;
		this.velocityY *= DRAG;
		this.velocityZ *= DRAG;

		if (!world.getFluidState(BlockPos.ofFloored(this.x, this.y, this.z)).isIn(FluidTags.WATER)) {
			this.markDead();
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

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
			return new WaterBubbleParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.getSprite(random));
		}
	}
}
