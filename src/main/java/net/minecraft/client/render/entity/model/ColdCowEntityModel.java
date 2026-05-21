package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ColdCowEntityModel}.
 */
public class ColdCowEntityModel extends CowEntityModel {

	public ColdCowEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		modelData.getRoot()
		         .addChild(
				         "body",
				         ModelPartBuilder.create()
				                         .uv(20, 32)
				                         .cuboid(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F, new Dilation(0.5F))
				                         .uv(18, 4)
				                         .cuboid(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F)
				                         .uv(52, 0)
				                         .cuboid(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F),
				         ModelTransform.of(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		         );
		ModelPartData modelPartData = modelData.getRoot()
		                                       .addChild(
				                                       "head",
				                                       ModelPartBuilder
						                                       .create()
						                                       .uv(0, 0)
						                                       .cuboid(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
						                                       .uv(9, 33)
						                                       .cuboid(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F),
				                                       ModelTransform.origin(0.0F, 4.0F, -8.0F)
		                                       );
		modelPartData.addChild(
				"right_horn",
				ModelPartBuilder.create().uv(0, 40).cuboid(-1.5F, -4.5F, -0.5F, 2.0F, 6.0F, 2.0F),
				ModelTransform.of(-4.5F, -2.5F, -3.5F, 1.5708F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_horn",
				ModelPartBuilder.create().uv(0, 32).cuboid(-1.5F, -3.0F, -0.5F, 2.0F, 6.0F, 2.0F),
				ModelTransform.of(5.5F, -2.5F, -5.0F, 1.5708F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}
}
