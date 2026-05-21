package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code PigEntityModel}.
 */
public class PigEntityModel extends QuadrupedEntityModel<LivingEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(false, 4.0F, 4.0F, Set.of("head"));

	public PigEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData(Dilation dilation) {
		return TexturedModelData.of(getModelData(dilation), 64, 64);
	}

	protected static ModelData getModelData(Dilation dilation) {
		ModelData modelData = QuadrupedEntityModel.getModelData(6, true, false, dilation);
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, dilation)
				                .uv(16, 16)
				                .cuboid(-2.0F, 0.0F, -9.0F, 4.0F, 3.0F, 1.0F, dilation),
				ModelTransform.origin(0.0F, 12.0F, -6.0F)
		);
		return modelData;
	}
}
