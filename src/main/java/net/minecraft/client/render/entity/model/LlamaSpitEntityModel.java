package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code LlamaSpitEntityModel}.
 */
public class LlamaSpitEntityModel extends EntityModel<EntityRenderState> {

	private static final String MAIN = "main";

	public LlamaSpitEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		int i = 2;
		modelPartData.addChild(
				"main",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(0.0F, -4.0F, 0.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(0.0F, 2.0F, 0.0F, 2.0F, 2.0F, 2.0F)
				                .cuboid(0.0F, 0.0F, 2.0F, 2.0F, 2.0F, 2.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 32);
	}
}
