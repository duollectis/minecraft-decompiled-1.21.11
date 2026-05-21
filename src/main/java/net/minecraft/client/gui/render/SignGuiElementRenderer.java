package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.state.special.SignGuiElementRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
/**
 * {@code SignGuiElementRenderer}.
 */
public class SignGuiElementRenderer extends SpecialGuiElementRenderer<SignGuiElementRenderState> {

	private final SpriteHolder sprite;

	public SignGuiElementRenderer(VertexConsumerProvider.Immediate immediate, SpriteHolder sprite) {
		super(immediate);
		this.sprite = sprite;
	}

	@Override
	public Class<SignGuiElementRenderState> getElementClass() {
		return SignGuiElementRenderState.class;
	}

	/**
	 * Render.
	 *
	 * @param signGuiElementRenderState sign gui element render state
	 * @param matrixStack matrix stack
	 */
	protected void render(SignGuiElementRenderState signGuiElementRenderState, MatrixStack matrixStack) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);
		matrixStack.translate(0.0F, -0.75F, 0.0F);
		SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getSignTextureId(signGuiElementRenderState.woodType());
		Model.SinglePartModel singlePartModel = signGuiElementRenderState.signModel();
		VertexConsumer
				vertexConsumer =
				spriteIdentifier.getVertexConsumer(this.sprite, this.vertexConsumers, singlePartModel::getLayer);
		singlePartModel.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
	}

	@Override
	protected String getName() {
		return "sign";
	}
}
