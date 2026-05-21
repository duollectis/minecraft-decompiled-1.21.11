package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.MinecartEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code MinecartEntityModel}.
 */
public class MinecartEntityModel extends EntityModel<MinecartEntityRenderState> {

	public MinecartEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		int i = 20;
		int j = 8;
		int k = 16;
		int l = 4;
		modelPartData.addChild(
				"bottom",
				ModelPartBuilder.create().uv(0, 10).cuboid(-10.0F, -8.0F, -1.0F, 20.0F, 16.0F, 2.0F),
				ModelTransform.of(0.0F, 4.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"front",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
				ModelTransform.of(-9.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI * 3.0 / 2.0), 0.0F)
		);
		modelPartData.addChild(
				"back",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
				ModelTransform.of(9.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
		);
		modelPartData.addChild(
				"left",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
				ModelTransform.of(0.0F, 4.0F, -7.0F, 0.0F, (float) Math.PI, 0.0F)
		);
		modelPartData.addChild(
				"right",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -9.0F, -1.0F, 16.0F, 8.0F, 2.0F),
				ModelTransform.origin(0.0F, 4.0F, 7.0F)
		);
		return TexturedModelData.of(modelData, 64, 32);
	}
}
