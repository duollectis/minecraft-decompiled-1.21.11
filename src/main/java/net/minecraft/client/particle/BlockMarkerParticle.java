package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.math.random.Random;

/**
 * Статичная частица-маркер блока, отображающая текстуру блока в виде парящего
 * спрайта. Используется для визуальной отладки и эффектов (например, структурные блоки).
 * Не подчиняется гравитации и не сталкивается с миром.
 */
@Environment(EnvType.CLIENT)
public class BlockMarkerParticle extends BillboardParticle {

	private static final int MAX_AGE = 80;
	private static final float FIXED_SIZE = 0.5F;

	private final BillboardParticle.RenderType renderType;

	BlockMarkerParticle(ClientWorld world, double x, double y, double z, BlockState state) {
		super(
				world,
				x,
				y,
				z,
				MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(state)
		);
		this.gravityStrength = 0.0F;
		this.maxAge = MAX_AGE;
		this.collidesWithWorld = false;
		this.renderType = this.sprite.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
				? BillboardParticle.RenderType.BLOCK_ATLAS_TRANSLUCENT
				: BillboardParticle.RenderType.ITEM_ATLAS_TRANSLUCENT;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return renderType;
	}

	@Override
	public float getSize(float tickProgress) {
		return FIXED_SIZE;
	}

	/**
	 * Фабрика для создания маркерных частиц из состояния блока.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<BlockStateParticleEffect> {

		@Override
		public Particle createParticle(
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
			return new BlockMarkerParticle(world, x, y, z, effect.getBlockState());
		}
	}
}
