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

/**
 * Рендерер скина игрока в GUI (например, на экране настройки внешнего вида).
 * Применяет вращение через стек матриц модели-вида, а не через MatrixStack,
 * чтобы корректно обработать поворот вокруг оси X с учётом pivot-точки по Y.
 */
@Environment(EnvType.CLIENT)
public class PlayerSkinGuiElementRenderer extends SpecialGuiElementRenderer<PlayerSkinGuiElementRenderState> {

	private static final float PLAYER_Y_TRANSLATE = -1.6010001F;

	public PlayerSkinGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
		super(immediate);
	}

	@Override
	public Class<PlayerSkinGuiElementRenderState> getElementClass() {
		return PlayerSkinGuiElementRenderState.class;
	}

	/**
	 * Отрисовывает модель скина игрока.
	 * Поворот по оси X выполняется через {@link Matrix4fStack} модели-вида с учётом
	 * pivot-точки по Y, масштабированной на текущий scale-фактор окна.
	 * Это позволяет вращать модель вокруг нужной точки без искажений.
	 */
	@Override
	protected void render(PlayerSkinGuiElementRenderState state, MatrixStack matrices) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.PLAYER_SKIN);

		int scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();

		float scaledPivotY = state.scale() * scaleFactor;
		modelViewStack.rotateAround(
				RotationAxis.POSITIVE_X.rotationDegrees(state.xRotation()),
				0.0F,
				scaledPivotY * -state.yPivot(),
				0.0F
		);

		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yRotation()));
		matrices.translate(0.0F, PLAYER_Y_TRANSLATE, 0.0F);

		RenderLayer renderLayer = state.playerModel().getLayer(state.texture());
		state.playerModel().render(
				matrices,
				vertexConsumers.getBuffer(renderLayer),
				15728880,
				OverlayTexture.DEFAULT_UV
		);

		vertexConsumers.draw();
		modelViewStack.popMatrix();
	}

	@Override
	protected String getName() {
		return "player skin";
	}
}
