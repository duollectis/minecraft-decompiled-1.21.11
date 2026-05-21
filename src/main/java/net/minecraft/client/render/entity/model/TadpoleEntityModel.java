package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code TadpoleEntityModel}.
 */
public class TadpoleEntityModel extends EntityModel<LivingEntityRenderState> {

	private final ModelPart tail;

	public TadpoleEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityCutoutNoCull);
		this.tail = modelPart.getChild("tail");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = 0.0F;
		float g = 22.0F;
		float h = -3.0F;
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 3.0F),
				ModelTransform.origin(0.0F, 22.0F, -3.0F)
		);
		modelPartData.addChild(
				"tail",
				ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, -1.0F, 0.0F, 0.0F, 2.0F, 7.0F),
				ModelTransform.origin(0.0F, 22.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 16, 16);
	}

	public void setAngles(LivingEntityRenderState livingEntityRenderState) {
		super.setAngles(livingEntityRenderState);
		float f = livingEntityRenderState.touchingWater ? 1.0F : 1.5F;
		this.tail.yaw = -f * 0.25F * MathHelper.sin(0.3F * livingEntityRenderState.age);
	}
}
