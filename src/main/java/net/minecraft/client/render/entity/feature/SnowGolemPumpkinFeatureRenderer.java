package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.entity.state.SnowGolemEntityRenderState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code SnowGolemPumpkinFeatureRenderer}.
 */
public class SnowGolemPumpkinFeatureRenderer extends FeatureRenderer<SnowGolemEntityRenderState, SnowGolemEntityModel> {

	private final BlockRenderManager blockRenderManager;

	public SnowGolemPumpkinFeatureRenderer(
			FeatureRendererContext<SnowGolemEntityRenderState, SnowGolemEntityModel> context,
			BlockRenderManager blockRenderManager
	) {
		super(context);
		this.blockRenderManager = blockRenderManager;
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			SnowGolemEntityRenderState snowGolemEntityRenderState,
			float f,
			float g
	) {
		if (snowGolemEntityRenderState.hasPumpkin) {
			if (!snowGolemEntityRenderState.invisible || snowGolemEntityRenderState.hasOutline()) {
				matrixStack.push();
				this.getContextModel().getHead().applyTransform(matrixStack);
				float h = 0.625F;
				matrixStack.translate(0.0F, -0.34375F, 0.0F);
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
				matrixStack.scale(0.625F, -0.625F, -0.625F);
				BlockState blockState = Blocks.CARVED_PUMPKIN.getDefaultState();
				BlockStateModel blockStateModel = this.blockRenderManager.getModel(blockState);
				int j = LivingEntityRenderer.getOverlay(snowGolemEntityRenderState, 0.0F);
				matrixStack.translate(-0.5F, -0.5F, -0.5F);
				RenderLayer
						renderLayer =
						snowGolemEntityRenderState.hasOutline() && snowGolemEntityRenderState.invisible
						? RenderLayers.outlineNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
						: BlockRenderLayers.getEntityBlockLayer(blockState);
				orderedRenderCommandQueue.submitBlockStateModel(
						matrixStack,
						renderLayer,
						blockStateModel,
						0.0F,
						0.0F,
						0.0F,
						i,
						j,
						snowGolemEntityRenderState.outlineColor
				);
				matrixStack.pop();
			}
		}
	}
}
