package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.BreezeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.BreezeEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code BreezeWindFeatureRenderer}.
 */
public class BreezeWindFeatureRenderer extends FeatureRenderer<BreezeEntityRenderState, BreezeEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/breeze/breeze_wind.png");
	private final BreezeEntityModel model;

	public BreezeWindFeatureRenderer(
			FeatureRendererContext<BreezeEntityRenderState, BreezeEntityModel> featureRendererContext,
			LoadedEntityModels loadedEntityModels
	) {
		super(featureRendererContext);
		this.model = new BreezeEntityModel(loadedEntityModels.getModelPart(EntityModelLayers.BREEZE_WIND));
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			BreezeEntityRenderState breezeEntityRenderState,
			float f,
			float g
	) {
		RenderLayer
				renderLayer =
				RenderLayers.breezeWind(TEXTURE, this.getXOffset(breezeEntityRenderState.age) % 1.0F, 0.0F);
		orderedRenderCommandQueue.getBatchingQueue(1)
		                         .submitModel(
				                         this.model,
				                         breezeEntityRenderState,
				                         matrixStack,
				                         renderLayer,
				                         i,
				                         OverlayTexture.DEFAULT_UV,
				                         -1,
				                         null,
				                         breezeEntityRenderState.outlineColor,
				                         null
		                         );
	}

	private float getXOffset(float tickProgress) {
		return tickProgress * 0.02F;
	}
}
