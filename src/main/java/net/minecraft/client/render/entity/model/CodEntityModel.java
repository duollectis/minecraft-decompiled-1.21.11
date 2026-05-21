package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code CodEntityModel}.
 */
public class CodEntityModel extends EntityModel<LivingEntityRenderState> {

	private final ModelPart tailFin;

	public CodEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.tailFin = modelPart.getChild("tail_fin");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		int i = 22;
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 7.0F),
				ModelTransform.origin(0.0F, 22.0F, 0.0F)
		);
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(11, 0).cuboid(-1.0F, -2.0F, -3.0F, 2.0F, 4.0F, 3.0F),
				ModelTransform.origin(0.0F, 22.0F, 0.0F)
		);
		modelPartData.addChild(
				"nose",
				ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 1.0F),
				ModelTransform.origin(0.0F, 22.0F, -3.0F)
		);
		modelPartData.addChild(
				"right_fin",
				ModelPartBuilder.create().uv(22, 1).cuboid(-2.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
				ModelTransform.of(-1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 4))
		);
		modelPartData.addChild(
				"left_fin",
				ModelPartBuilder.create().uv(22, 4).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F),
				ModelTransform.of(1.0F, 23.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 4))
		);
		modelPartData.addChild(
				"tail_fin",
				ModelPartBuilder.create().uv(22, 3).cuboid(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 4.0F),
				ModelTransform.origin(0.0F, 22.0F, 7.0F)
		);
		modelPartData.addChild(
				"top_fin",
				ModelPartBuilder.create().uv(20, -6).cuboid(0.0F, -1.0F, -1.0F, 0.0F, 1.0F, 6.0F),
				ModelTransform.origin(0.0F, 20.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(LivingEntityRenderState livingEntityRenderState) {
		super.setAngles(livingEntityRenderState);
		float f = livingEntityRenderState.touchingWater ? 1.0F : 1.5F;
		this.tailFin.yaw = -f * 0.45F * MathHelper.sin(0.6F * livingEntityRenderState.age);
	}
}
