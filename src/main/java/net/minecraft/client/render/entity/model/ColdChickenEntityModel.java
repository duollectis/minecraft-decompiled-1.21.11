package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ColdChickenEntityModel}.
 */
public class ColdChickenEntityModel extends ChickenEntityModel {

	public ColdChickenEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		modelData.getRoot()
		         .addChild(
				         "body",
				         ModelPartBuilder
						         .create()
						         .uv(0, 9)
						         .cuboid(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F)
						         .uv(38, 9)
						         .cuboid(0.0F, 3.0F, -1.0F, 0.0F, 3.0F, 5.0F),
				         ModelTransform.of(0.0F, 16.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		         );
		modelData.getRoot()
		         .addChild(
				         "head",
				         ModelPartBuilder
						         .create()
						         .uv(0, 0)
						         .cuboid(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 3.0F)
						         .uv(44, 0)
						         .cuboid(-3.0F, -7.0F, -2.015F, 6.0F, 3.0F, 4.0F),
				         ModelTransform.origin(0.0F, 15.0F, -4.0F)
		         );
		return TexturedModelData.of(modelData, 64, 32);
	}
}
