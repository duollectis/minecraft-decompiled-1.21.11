package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

/**
 * Частица пыли блока — маленький фрагмент текстуры блока, разлетающийся
 * при его разрушении или взаимодействии. Цвет берётся из системы окраски блоков
 * (BlockColors), а UV-координаты выбираются случайно из 4×4 сетки текстуры.
 */
@Environment(EnvType.CLIENT)
public class BlockDustParticle extends BillboardParticle {

	private static final float BASE_COLOR = 0.6F;
	private static final float UV_GRID_SIZE = 4.0F;

	private final BillboardParticle.RenderType renderType;
	private final BlockPos blockPos;
	private final float sampleU;
	private final float sampleV;

	public BlockDustParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			BlockState state
	) {
		this(world, x, y, z, velocityX, velocityY, velocityZ, state, BlockPos.ofFloored(x, y, z));
	}

	public BlockDustParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			BlockState state,
			BlockPos blockPos
	) {
		super(
				world,
				x,
				y,
				z,
				velocityX,
				velocityY,
				velocityZ,
				MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(state)
		);
		this.blockPos = blockPos;
		this.gravityStrength = 1.0F;
		this.red = BASE_COLOR;
		this.green = BASE_COLOR;
		this.blue = BASE_COLOR;

		// Трава — особый случай: её цвет не применяется к частицам (иначе они были бы зелёными)
		if (!state.isOf(Blocks.GRASS_BLOCK)) {
			int blockColor = MinecraftClient.getInstance().getBlockColors().getColor(state, world, blockPos, 0);
			this.red *= (blockColor >> 16 & 0xFF) / 255.0F;
			this.green *= (blockColor >> 8 & 0xFF) / 255.0F;
			this.blue *= (blockColor & 0xFF) / 255.0F;
		}

		this.scale /= 2.0F;
		this.sampleU = this.random.nextFloat() * 3.0F;
		this.sampleV = this.random.nextFloat() * 3.0F;
		this.renderType = this.sprite.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
				? BillboardParticle.RenderType.BLOCK_ATLAS_TRANSLUCENT
				: BillboardParticle.RenderType.ITEM_ATLAS_TRANSLUCENT;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return renderType;
	}

	@Override
	protected float getMinU() {
		return this.sprite.getFrameU((this.sampleU + 1.0F) / UV_GRID_SIZE);
	}

	@Override
	protected float getMaxU() {
		return this.sprite.getFrameU(this.sampleU / UV_GRID_SIZE);
	}

	@Override
	protected float getMinV() {
		return this.sprite.getFrameV(this.sampleV / UV_GRID_SIZE);
	}

	@Override
	protected float getMaxV() {
		return this.sprite.getFrameV((this.sampleV + 1.0F) / UV_GRID_SIZE);
	}

	@Override
	public int getBrightness(float tint) {
		int brightness = super.getBrightness(tint);
		return brightness == 0 && this.world.isChunkLoaded(this.blockPos)
				? WorldRenderer.getLightmapCoordinates(this.world, this.blockPos)
				: brightness;
	}

	/**
	 * Создаёт частицу пыли блока, если состояние блока допускает частицы разрушения.
	 * Возвращает {@code null} для воздуха, движущихся поршней и блоков без частиц.
	 */
	static @Nullable BlockDustParticle create(
			BlockStateParticleEffect parameters,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		BlockState blockState = parameters.getBlockState();
		return !blockState.isAir() && !blockState.isOf(Blocks.MOVING_PISTON) && blockState.hasBlockBreakParticles()
				? new BlockDustParticle(world, x, y, z, velocityX, velocityY, velocityZ, blockState)
				: null;
	}

	/**
	 * Фабрика для частиц крошения блока — без начальной скорости, живут 1–10 тиков.
	 */
	@Environment(EnvType.CLIENT)
	public static class CrumbleFactory implements ParticleFactory<BlockStateParticleEffect> {

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
			Particle particle = BlockDustParticle.create(effect, world, x, y, z, velocityX, velocityY, velocityZ);

			if (particle != null) {
				particle.setVelocity(0.0, 0.0, 0.0);
				particle.setMaxAge(random.nextInt(10) + 1);
			}

			return particle;
		}
	}

	/**
	 * Фабрика для частиц пылевого столба — гауссово рассеивание по горизонтали,
	 * живут 20–39 тиков.
	 */
	@Environment(EnvType.CLIENT)
	public static class DustPillarFactory implements ParticleFactory<BlockStateParticleEffect> {

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
			Particle particle = BlockDustParticle.create(effect, world, x, y, z, velocityX, velocityY, velocityZ);

			if (particle != null) {
				particle.setVelocity(
						random.nextGaussian() / 30.0,
						velocityY + random.nextGaussian() / 2.0,
						random.nextGaussian() / 30.0
				);
				particle.setMaxAge(random.nextInt(20) + 20);
			}

			return particle;
		}
	}

	/**
	 * Базовая фабрика для частиц пыли блока без модификаций.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<BlockStateParticleEffect> {

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
			return BlockDustParticle.create(effect, world, x, y, z, velocityX, velocityY, velocityZ);
		}
	}
}
