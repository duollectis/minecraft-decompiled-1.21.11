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

/**
 * Частица падающего блока (песок, гравий, бетонный порошок и т.д.) —
 * вращающийся спрайт, медленно ускоряющийся вниз. Цвет берётся из
 * {@link FallingBlock#getColor} или системы окраски частиц блока.
 */
@Environment(EnvType.CLIENT)
public class BlockFallingDustParticle extends BillboardParticle {

	private static final float SCALE_FACTOR = 0.67499995F;
	private static final float SIZE_RAMP_FACTOR = 32.0F;
	private static final float GRAVITY_INCREMENT = 0.003F;
	private static final float MAX_FALL_SPEED = -0.14F;
	private static final float ROTATION_SPEED_RANGE = 0.1F;

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
		this.scale *= SCALE_FACTOR;
		int baseAge = (int) (32.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.maxAge = (int) Math.max(baseAge * 0.9F, 1.0F);
		this.updateSprite(spriteProvider);
		this.rotationSpeed = (this.random.nextFloat() - 0.5F) * ROTATION_SPEED_RANGE;
		this.zRotation = this.random.nextFloat() * (float) (Math.PI * 2);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * SIZE_RAMP_FACTOR, 0.0F, 1.0F);
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

		this.updateSprite(this.spriteProvider);
		this.lastZRotation = this.zRotation;
		this.zRotation = this.zRotation + (float) Math.PI * this.rotationSpeed * 2.0F;

		if (this.onGround) {
			this.lastZRotation = 0.0F;
			this.zRotation = 0.0F;
		}

		this.move(this.velocityX, this.velocityY, this.velocityZ);
		this.velocityY -= GRAVITY_INCREMENT;
		this.velocityY = Math.max(this.velocityY, MAX_FALL_SPEED);
	}

	/**
	 * Фабрика для создания частиц падающего блока.
	 * Извлекает цвет из {@link FallingBlock} или системы окраски частиц.
	 * Возвращает {@code null} для невидимых блоков.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<BlockStateParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public @Nullable Particle createParticle(
				BlockStateParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			BlockState blockState = effect.getBlockState();

			if (!blockState.isAir() && blockState.getRenderType() == BlockRenderType.INVISIBLE) {
				return null;
			}

			BlockPos blockPos = BlockPos.ofFloored(x, y, z);
			int packedColor = MinecraftClient.getInstance()
					.getBlockColors()
					.getParticleColor(blockState, world, blockPos);

			if (blockState.getBlock() instanceof FallingBlock fallingBlock) {
				packedColor = fallingBlock.getColor(blockState, world, blockPos);
			}

			float red = (packedColor >> 16 & 0xFF) / 255.0F;
			float green = (packedColor >> 8 & 0xFF) / 255.0F;
			float blue = (packedColor & 0xFF) / 255.0F;

			return new BlockFallingDustParticle(world, x, y, z, red, green, blue, this.spriteProvider);
		}
	}
}
