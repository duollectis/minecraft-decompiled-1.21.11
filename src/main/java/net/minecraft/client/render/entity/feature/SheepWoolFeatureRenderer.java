package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.SheepEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code SheepWoolFeatureRenderer}.
 */
public class SheepWoolFeatureRenderer extends FeatureRenderer<SheepEntityRenderState, SheepEntityModel> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/sheep/sheep_wool.png");
	private final EntityModel<SheepEntityRenderState> woolModel;
	private final EntityModel<SheepEntityRenderState> babyWoolModel;

	public SheepWoolFeatureRenderer(
			FeatureRendererContext<SheepEntityRenderState, SheepEntityModel> context,
			LoadedEntityModels loader
	) {
		super(context);
		this.woolModel = new SheepWoolEntityModel(loader.getModelPart(EntityModelLayers.SHEEP_WOOL));
		this.babyWoolModel = new SheepWoolEntityModel(loader.getModelPart(EntityModelLayers.SHEEP_BABY_WOOL));
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			SheepEntityRenderState sheepEntityRenderState,
			float f,
			float g
	) {
		if (!sheepEntityRenderState.sheared) {
			EntityModel<SheepEntityRenderState>
					entityModel =
					sheepEntityRenderState.baby ? this.babyWoolModel : this.woolModel;
			if (sheepEntityRenderState.invisible) {
				if (sheepEntityRenderState.hasOutline()) {
					orderedRenderCommandQueue.submitModel(
							entityModel,
							sheepEntityRenderState,
							matrixStack,
							RenderLayers.outlineNoCull(TEXTURE),
							i,
							LivingEntityRenderer.getOverlay(sheepEntityRenderState, 0.0F),
							-16777216,
							null,
							sheepEntityRenderState.outlineColor,
							null
					);
				}
			}
			else {
				render(
						entityModel,
						TEXTURE,
						matrixStack,
						orderedRenderCommandQueue,
						i,
						sheepEntityRenderState,
						sheepEntityRenderState.getRgbColor(),
						0
				);
			}
		}
	}
}
