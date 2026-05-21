package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.state.MooshroomEntityRenderState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code MooshroomMushroomFeatureRenderer}.
 */
public class MooshroomMushroomFeatureRenderer extends FeatureRenderer<MooshroomEntityRenderState, CowEntityModel> {

	private final BlockRenderManager blockRenderManager;

	public MooshroomMushroomFeatureRenderer(
			FeatureRendererContext<MooshroomEntityRenderState, CowEntityModel> context,
			BlockRenderManager blockRenderManager
	) {
		super(context);
		this.blockRenderManager = blockRenderManager;
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			MooshroomEntityRenderState mooshroomEntityRenderState,
			float f,
			float g
	) {
		if (!mooshroomEntityRenderState.baby) {
			boolean bl = mooshroomEntityRenderState.hasOutline() && mooshroomEntityRenderState.invisible;
			if (!mooshroomEntityRenderState.invisible || bl) {
				BlockState blockState = mooshroomEntityRenderState.type.getMushroomState();
				int j = LivingEntityRenderer.getOverlay(mooshroomEntityRenderState, 0.0F);
				BlockStateModel blockStateModel = this.blockRenderManager.getModel(blockState);
				matrixStack.push();
				matrixStack.translate(0.2F, -0.35F, 0.5F);
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-48.0F));
				matrixStack.scale(-1.0F, -1.0F, 1.0F);
				matrixStack.translate(-0.5F, -0.5F, -0.5F);
				this.renderMushroom(
						matrixStack,
						orderedRenderCommandQueue,
						i,
						bl,
						mooshroomEntityRenderState.outlineColor,
						blockState,
						j,
						blockStateModel
				);
				matrixStack.pop();
				matrixStack.push();
				matrixStack.translate(0.2F, -0.35F, 0.5F);
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(42.0F));
				matrixStack.translate(0.1F, 0.0F, -0.6F);
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-48.0F));
				matrixStack.scale(-1.0F, -1.0F, 1.0F);
				matrixStack.translate(-0.5F, -0.5F, -0.5F);
				this.renderMushroom(
						matrixStack,
						orderedRenderCommandQueue,
						i,
						bl,
						mooshroomEntityRenderState.outlineColor,
						blockState,
						j,
						blockStateModel
				);
				matrixStack.pop();
				matrixStack.push();
				this.getContextModel().getHead().applyTransform(matrixStack);
				matrixStack.translate(0.0F, -0.7F, -0.2F);
				matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-78.0F));
				matrixStack.scale(-1.0F, -1.0F, 1.0F);
				matrixStack.translate(-0.5F, -0.5F, -0.5F);
				this.renderMushroom(
						matrixStack,
						orderedRenderCommandQueue,
						i,
						bl,
						mooshroomEntityRenderState.outlineColor,
						blockState,
						j,
						blockStateModel
				);
				matrixStack.pop();
			}
		}
	}

	private void renderMushroom(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			boolean renderAsModel,
			int i,
			BlockState blockState,
			int j,
			BlockStateModel blockStateModel
	) {
		if (renderAsModel) {
			orderedRenderCommandQueue.submitBlockStateModel(
					matrices,
					RenderLayers.outlineNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE),
					blockStateModel,
					0.0F,
					0.0F,
					0.0F,
					light,
					j,
					i
			);
		}
		else {
			orderedRenderCommandQueue.submitBlock(matrices, blockState, light, j, i);
		}
	}
}
