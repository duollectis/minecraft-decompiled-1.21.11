package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LlamaSpitEntityModel;
import net.minecraft.client.render.entity.state.LlamaSpitEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code LlamaSpitEntityRenderer}.
 */
public class LlamaSpitEntityRenderer extends EntityRenderer<LlamaSpitEntity, LlamaSpitEntityRenderState> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/llama/spit.png");
	private final LlamaSpitEntityModel model;

	public LlamaSpitEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.model = new LlamaSpitEntityModel(context.getPart(EntityModelLayers.LLAMA_SPIT));
	}

	public void render(
			LlamaSpitEntityRenderState llamaSpitEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		matrixStack.translate(0.0F, 0.15F, 0.0F);
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(llamaSpitEntityRenderState.yaw - 90.0F));
		matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(llamaSpitEntityRenderState.pitch));
		orderedRenderCommandQueue.submitModel(
				this.model,
				llamaSpitEntityRenderState,
				matrixStack,
				this.model.getLayer(TEXTURE),
				llamaSpitEntityRenderState.light,
				OverlayTexture.DEFAULT_UV,
				llamaSpitEntityRenderState.outlineColor,
				null
		);
		matrixStack.pop();
		super.render(llamaSpitEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	public LlamaSpitEntityRenderState createRenderState() {
		return new LlamaSpitEntityRenderState();
	}

	public void updateRenderState(
			LlamaSpitEntity llamaSpitEntity,
			LlamaSpitEntityRenderState llamaSpitEntityRenderState,
			float f
	) {
		super.updateRenderState(llamaSpitEntity, llamaSpitEntityRenderState, f);
		llamaSpitEntityRenderState.pitch = llamaSpitEntity.getLerpedPitch(f);
		llamaSpitEntityRenderState.yaw = llamaSpitEntity.getLerpedYaw(f);
	}
}
