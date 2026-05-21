package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;

@Environment(EnvType.CLIENT)
/**
 * {@code WarmCowEntityModel}.
 */
public class WarmCowEntityModel extends CowEntityModel {

	public WarmCowEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		modelData.getRoot()
		         .addChild(
				         "head",
				         ModelPartBuilder.create()
				                         .uv(0, 0)
				                         .cuboid(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
				                         .uv(1, 33)
				                         .cuboid(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F)
				                         .uv(27, 0)
				                         .cuboid(-8.0F, -3.0F, -5.0F, 4.0F, 2.0F, 2.0F)
				                         .uv(39, 0)
				                         .cuboid(-8.0F, -5.0F, -5.0F, 2.0F, 2.0F, 2.0F)
				                         .uv(27, 0)
				                         .mirrored()
				                         .cuboid(4.0F, -3.0F, -5.0F, 4.0F, 2.0F, 2.0F)
				                         .mirrored(false)
				                         .uv(39, 0)
				                         .mirrored()
				                         .cuboid(6.0F, -5.0F, -5.0F, 2.0F, 2.0F, 2.0F)
				                         .mirrored(false),
				         ModelTransform.origin(0.0F, 4.0F, -8.0F)
		         );
		return TexturedModelData.of(modelData, 64, 64);
	}
}
