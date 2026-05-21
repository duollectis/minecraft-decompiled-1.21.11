package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code RainSplashParticle}.
 */
public class RainSplashParticle extends BillboardParticle {

	protected RainSplashParticle(ClientWorld clientWorld, double d, double e, double f, Sprite sprite) {
		super(clientWorld, d, e, f, 0.0, 0.0, 0.0, sprite);
		this.velocityX *= 0.3F;
		this.velocityY = this.random.nextFloat() * 0.2F + 0.1F;
		this.velocityZ *= 0.3F;
		this.setBoundingBoxSpacing(0.01F, 0.01F);
		this.gravityStrength = 0.06F;
		this.maxAge = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.2));
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
		if (this.maxAge-- <= 0) {
			this.markDead();
		}
		else {
			this.velocityY = this.velocityY - this.gravityStrength;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			this.velocityX *= 0.98F;
			this.velocityY *= 0.98F;
			this.velocityZ *= 0.98F;
			if (this.onGround) {
				if (this.random.nextFloat() < 0.5F) {
					this.markDead();
				}

				this.velocityX *= 0.7F;
				this.velocityZ *= 0.7F;
			}

			BlockPos blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
			double d = Math.max(
					this.world
							.getBlockState(blockPos)
							.getCollisionShape(this.world, blockPos)
							.getEndingCoord(Direction.Axis.Y, this.x - blockPos.getX(), this.z - blockPos.getZ()),
					(double) this.world.getFluidState(blockPos).getHeight(this.world, blockPos)
			);
			if (d > 0.0 && this.y < blockPos.getY() + d) {
				this.markDead();
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				SimpleParticleType simpleParticleType,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			return new RainSplashParticle(clientWorld, d, e, f, this.spriteProvider.getSprite(random));
		}
	}
}
