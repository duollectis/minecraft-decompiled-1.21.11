package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockMarkerParticle}.
 */
public class BlockMarkerParticle extends BillboardParticle {

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
		this.maxAge = 80;
		this.collidesWithWorld = false;
		this.renderType = this.sprite.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
		                  ? BillboardParticle.RenderType.BLOCK_ATLAS_TRANSLUCENT
		                  : BillboardParticle.RenderType.ITEM_ATLAS_TRANSLUCENT;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return this.renderType;
	}

	@Override
	public float getSize(float tickProgress) {
		return 0.5F;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<BlockStateParticleEffect> {

		public Particle createParticle(
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
			return new BlockMarkerParticle(clientWorld, d, e, f, blockStateParticleEffect.getBlockState());
		}
	}
}
