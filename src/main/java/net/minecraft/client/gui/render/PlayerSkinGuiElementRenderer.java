package net.minecraft.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.state.special.PlayerSkinGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4fStack;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerSkinGuiElementRenderer}.
 */
public class PlayerSkinGuiElementRenderer extends SpecialGuiElementRenderer<PlayerSkinGuiElementRenderState> {

	public PlayerSkinGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
		super(immediate);
	}

	@Override
	public Class<PlayerSkinGuiElementRenderState> getElementClass() {
		return PlayerSkinGuiElementRenderState.class;
	}

	protected void render(PlayerSkinGuiElementRenderState playerSkinGuiElementRenderState, MatrixStack matrixStack) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.PLAYER_SKIN);
		int i = MinecraftClient.getInstance().getWindow().getScaleFactor();
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		float f = playerSkinGuiElementRenderState.scale() * i;
		matrix4fStack.rotateAround(
				RotationAxis.POSITIVE_X.rotationDegrees(playerSkinGuiElementRenderState.xRotation()),
				0.0F,
				f * -playerSkinGuiElementRenderState.yPivot(),
				0.0F
		);
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-playerSkinGuiElementRenderState.yRotation()));
		matrixStack.translate(0.0F, -1.6010001F, 0.0F);
		RenderLayer
				renderLayer =
				playerSkinGuiElementRenderState.playerModel().getLayer(playerSkinGuiElementRenderState.texture());
		playerSkinGuiElementRenderState
				.playerModel()
				.render(matrixStack, this.vertexConsumers.getBuffer(renderLayer), 15728880, OverlayTexture.DEFAULT_UV);
		this.vertexConsumers.draw();
		matrix4fStack.popMatrix();
	}

	@Override
	protected String getName() {
		return "player skin";
	}
}
