package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ParrotEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.ParrotEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.ParrotEntity;

@Environment(EnvType.CLIENT)
/**
 * {@code ShoulderParrotFeatureRenderer}.
 */
public class ShoulderParrotFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

	private final ParrotEntityModel model;

	public ShoulderParrotFeatureRenderer(
			FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
			LoadedEntityModels loader
	) {
		super(context);
		this.model = new ParrotEntityModel(loader.getModelPart(EntityModelLayers.PARROT));
	}

	public void render(
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			PlayerEntityRenderState playerEntityRenderState,
			float f,
			float g
	) {
		ParrotEntity.Variant variant = playerEntityRenderState.leftShoulderParrotVariant;
		if (variant != null) {
			this.render(matrixStack, orderedRenderCommandQueue, i, playerEntityRenderState, variant, f, g, true);
		}

		ParrotEntity.Variant variant2 = playerEntityRenderState.rightShoulderParrotVariant;
		if (variant2 != null) {
			this.render(matrixStack, orderedRenderCommandQueue, i, playerEntityRenderState, variant2, f, g, false);
		}
	}

	private void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			PlayerEntityRenderState state,
			ParrotEntity.Variant parrotVariant,
			float headYaw,
			float headPitch,
			boolean left
	) {
		matrices.push();
		matrices.translate(left ? 0.4F : -0.4F, state.isInSneakingPose ? -1.3F : -1.5F, 0.0F);
		ParrotEntityRenderState parrotEntityRenderState = new ParrotEntityRenderState();
		parrotEntityRenderState.parrotPose = ParrotEntityModel.Pose.ON_SHOULDER;
		parrotEntityRenderState.age = state.age;
		parrotEntityRenderState.limbSwingAnimationProgress = state.limbSwingAnimationProgress;
		parrotEntityRenderState.limbSwingAmplitude = state.limbSwingAmplitude;
		parrotEntityRenderState.relativeHeadYaw = headYaw;
		parrotEntityRenderState.pitch = headPitch;
		orderedRenderCommandQueue.submitModel(
				this.model,
				parrotEntityRenderState,
				matrices,
				this.model.getLayer(ParrotEntityRenderer.getTexture(parrotVariant)),
				light,
				OverlayTexture.DEFAULT_UV,
				state.outlineColor,
				null
		);
		matrices.pop();
	}
}
