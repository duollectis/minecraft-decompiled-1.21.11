package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockFallingDustParticle}.
 */
public class BlockFallingDustParticle extends BillboardParticle {

	private final float rotationSpeed;
	private final SpriteProvider spriteProvider;

	BlockFallingDustParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			float red,
			float green,
			float blue,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.spriteProvider = spriteProvider;
		this.red = red;
		this.green = green;
		this.blue = blue;
		float f = 0.9F;
		this.scale *= 0.67499995F;
		int i = (int) (32.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.maxAge = (int) Math.max(i * 0.9F, 1.0F);
		this.updateSprite(spriteProvider);
		this.rotationSpeed = (this.random.nextFloat() - 0.5F) * 0.1F;
		this.zRotation = this.random.nextFloat() * (float) (Math.PI * 2);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		if (this.age++ >= this.maxAge) {
			this.markDead();
		}
		else {
			this.updateSprite(this.spriteProvider);
			this.lastZRotation = this.zRotation;
			this.zRotation = this.zRotation + (float) Math.PI * this.rotationSpeed * 2.0F;
			if (this.onGround) {
				this.lastZRotation = this.zRotation = 0.0F;
			}

			this.move(this.velocityX, this.velocityY, this.velocityZ);
			this.velocityY -= 0.003F;
			this.velocityY = Math.max(this.velocityY, -0.14F);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<BlockStateParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public @Nullable Particle createParticle(
				BlockStateParticleEffect blockStateParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			BlockState blockState = blockStateParticleEffect.getBlockState();
			if (!blockState.isAir() && blockState.getRenderType() == BlockRenderType.INVISIBLE) {
				return null;
			}
			else {
				BlockPos blockPos = BlockPos.ofFloored(d, e, f);
				int
						j =
						MinecraftClient
								.getInstance()
								.getBlockColors()
								.getParticleColor(blockState, clientWorld, blockPos);
				if (blockState.getBlock() instanceof FallingBlock) {
					j = ((FallingBlock) blockState.getBlock()).getColor(blockState, clientWorld, blockPos);
				}

				float k = (j >> 16 & 0xFF) / 255.0F;
				float l = (j >> 8 & 0xFF) / 255.0F;
				float m = (j & 0xFF) / 255.0F;
				return new BlockFallingDustParticle(clientWorld, d, e, f, k, l, m, this.spriteProvider);
			}
		}
	}
}
