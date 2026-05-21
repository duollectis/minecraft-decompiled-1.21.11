package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code EnergySwirlOverlayFeatureRenderer}.
 */
public abstract class EnergySwirlOverlayFeatureRenderer<S extends EntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {

	public EnergySwirlOverlayFeatureRenderer(FeatureRendererContext<S, M> featureRendererContext) {
		super(featureRendererContext);
	}

	@Override
	public void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			S state,
			float limbAngle,
			float limbDistance
	) {
		if (this.shouldRender(state)) {
			float f = state.age;
			M entityModel = this.getEnergySwirlModel();
			queue.getBatchingQueue(1)
			     .submitModel(
					     entityModel,
					     state,
					     matrices,
					     RenderLayers.energySwirl(
							     this.getEnergySwirlTexture(),
							     this.getEnergySwirlX(f) % 1.0F,
							     f * 0.01F % 1.0F
					     ),
					     light,
					     OverlayTexture.DEFAULT_UV,
					     -8355712,
					     null,
					     state.outlineColor,
					     null
			     );
		}
	}

	protected abstract boolean shouldRender(S state);

	protected abstract float getEnergySwirlX(float partialAge);

	protected abstract Identifier getEnergySwirlTexture();

	protected abstract M getEnergySwirlModel();
}
