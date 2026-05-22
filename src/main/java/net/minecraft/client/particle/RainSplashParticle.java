package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Частица брызг дождя: появляется при ударе капли о поверхность, подпрыгивает
 * вверх и падает обратно под действием гравитации. При попадании на поверхность
 * с 50% вероятностью исчезает. Также исчезает при погружении в жидкость.
 */
@Environment(EnvType.CLIENT)
public class RainSplashParticle extends BillboardParticle {

	private static final float VELOCITY_DAMPING = 0.98F;
	private static final float GROUND_FRICTION = 0.7F;

	protected RainSplashParticle(ClientWorld world, double x, double y, double z, Sprite sprite) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
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
			return;
		}

		this.velocityY -= this.gravityStrength;
		this.move(this.velocityX, this.velocityY, this.velocityZ);
		this.velocityX *= VELOCITY_DAMPING;
		this.velocityY *= VELOCITY_DAMPING;
		this.velocityZ *= VELOCITY_DAMPING;

		if (this.onGround) {
			if (this.random.nextFloat() < 0.5F) {
				this.markDead();
				return;
			}

			this.velocityX *= GROUND_FRICTION;
			this.velocityZ *= GROUND_FRICTION;
		}

		BlockPos blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
		double surfaceHeight = Math.max(
				this.world
						.getBlockState(blockPos)
						.getCollisionShape(this.world, blockPos)
						.getEndingCoord(Direction.Axis.Y, this.x - blockPos.getX(), this.z - blockPos.getZ()),
				(double) this.world.getFluidState(blockPos).getHeight(this.world, blockPos)
		);

		if (surfaceHeight > 0.0 && this.y < blockPos.getY() + surfaceHeight) {
			this.markDead();
		}
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
			return new RainSplashParticle(world, x, y, z, this.spriteProvider.getSprite(random));
		}
	}
}
