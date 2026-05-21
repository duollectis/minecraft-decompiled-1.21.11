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

@Environment(EnvType.CLIENT)
/**
 * {@code EntityGuiElementRenderer}.
 */
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

	protected void render(EntityGuiElementRenderState entityGuiElementRenderState, MatrixStack matrixStack) {
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
		Vector3f vector3f = entityGuiElementRenderState.translation();
		matrixStack.translate(vector3f.x, vector3f.y, vector3f.z);
		matrixStack.multiply(entityGuiElementRenderState.rotation());
		Quaternionf quaternionf = entityGuiElementRenderState.overrideCameraAngle();
		RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
		CameraRenderState cameraRenderState = new CameraRenderState();
		if (quaternionf != null) {
			cameraRenderState.orientation = quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI);
		}

		this.entityRenderDispatcher.render(
				entityGuiElementRenderState.renderState(),
				cameraRenderState,
				0.0,
				0.0,
				0.0,
				matrixStack,
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
