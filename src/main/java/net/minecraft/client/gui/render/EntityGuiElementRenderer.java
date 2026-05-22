package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.state.special.EntityGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Рендерер сущности в GUI (например, превью персонажа на экране инвентаря).
 * Поддерживает переопределение угла камеры для корректного отображения
 * сущности с произвольной ориентацией.
 */
@Environment(EnvType.CLIENT)
public class EntityGuiElementRenderer extends SpecialGuiElementRenderer<EntityGuiElementRenderState> {

	private final EntityRenderManager entityRenderDispatcher;

	public EntityGuiElementRenderer(
			VertexConsumerProvider.Immediate vertexConsumers,
			EntityRenderManager entityRenderDispatcher
	) {
		super(vertexConsumers);
		this.entityRenderDispatcher = entityRenderDispatcher;
	}

	@Override
	public Class<EntityGuiElementRenderState> getElementClass() {
		return EntityGuiElementRenderState.class;
	}

	/**
	 * Отрисовывает сущность в пространстве GUI.
	 * Если задан {@code overrideCameraAngle}, ориентация камеры вычисляется
	 * как сопряжённый кватернион с поворотом на π по оси Y — это компенсирует
	 * стандартный разворот модели сущности «лицом к камере».
	 */
	@Override
	protected void render(EntityGuiElementRenderState state, MatrixStack matrices) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

		Vector3f translation = state.translation();
		matrices.translate(translation.x, translation.y, translation.z);
		matrices.multiply(state.rotation());

		CameraRenderState cameraState = new CameraRenderState();
		Quaternionf overrideAngle = state.overrideCameraAngle();

		if (overrideAngle != null) {
			cameraState.orientation = overrideAngle.conjugate(new Quaternionf()).rotateY((float) Math.PI);
		}

		RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();

		entityRenderDispatcher.render(
				state.renderState(),
				cameraState,
				0.0,
				0.0,
				0.0,
				matrices,
				renderDispatcher.getQueue()
		);

		renderDispatcher.render();
	}

	@Override
	protected float getYOffset(int height, int windowScaleFactor) {
		return height / 2.0F;
	}

	@Override
	protected String getName() {
		return "entity";
	}
}
