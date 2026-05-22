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
 * Частица пузыря, поднимающегося вверх в столбе пузырей (bubble column).
 * Движется против гравитации и исчезает, покидая воду.
 */
@Environment(EnvType.CLIENT)
public class BubbleColumnUpParticle extends BillboardParticle {

	BubbleColumnUpParticle(
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
		this.gravityStrength = -0.125F;
		this.velocityMultiplier = 0.85F;
		this.setBoundingBoxSpacing(0.02F, 0.02F);
		this.scale = this.scale * (this.random.nextFloat() * 0.6F + 0.2F);
		this.velocityX = velocityX * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
		this.velocityY = velocityY * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
		this.velocityZ = velocityZ * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
		this.maxAge = (int) (40.0 / (this.random.nextFloat() * 0.8 + 0.2));
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.dead && !this.world.getFluidState(BlockPos.ofFloored(this.x, this.y, this.z)).isIn(FluidTags.WATER)) {
			this.markDead();
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	/**
	 * Фабрика для создания частиц восходящего пузырного столба.
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
			return new BubbleColumnUpParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					this.spriteProvider.getSprite(random)
			);
		}
	}
}
