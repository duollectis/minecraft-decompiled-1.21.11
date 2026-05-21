package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code CowEntityModel}.
 */
public class CowEntityModel extends QuadrupedEntityModel<LivingEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(false, 8.0F, 6.0F, Set.of("head"));
	private static final int UDDER_Y_OFFSET = 12;

	public CowEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		return TexturedModelData.of(modelData, 64, 64);
	}

	static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
				                .uv(1, 33)
				                .cuboid(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F)
				                .uv(22, 0)
				                .cuboid("right_horn", -5.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F)
				                .uv(22, 0)
				                .cuboid("left_horn", 4.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F),
				ModelTransform.origin(0.0F, 4.0F, -8.0F)
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder
						.create()
						.uv(18, 4)
						.cuboid(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F)
						.uv(52, 0)
						.cuboid(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F),
				ModelTransform.of(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().mirrored().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		ModelPartBuilder
				modelPartBuilder2 =
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		modelPartData.addChild("right_hind_leg", modelPartBuilder2, ModelTransform.origin(-4.0F, 12.0F, 7.0F));
		modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.origin(4.0F, 12.0F, 7.0F));
		modelPartData.addChild("right_front_leg", modelPartBuilder2, ModelTransform.origin(-4.0F, 12.0F, -5.0F));
		modelPartData.addChild("left_front_leg", modelPartBuilder, ModelTransform.origin(4.0F, 12.0F, -5.0F));
		return modelData;
	}

	public ModelPart getHead() {
		return this.head;
	}
}
