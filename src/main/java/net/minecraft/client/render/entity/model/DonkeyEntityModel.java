package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.DonkeyEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code DonkeyEntityModel}.
 */
public class DonkeyEntityModel extends AbstractHorseEntityModel<DonkeyEntityRenderState> {

	public static final float CHEST_SCALE_X = 0.87F;
	public static final float CHEST_SCALE_Y = 0.92F;
	private static final ModelTransformer DONKEY_PARTS_ADDER = data -> {
		addDonkeyParts(data.getRoot());
		return data;
	};
	private final ModelPart leftChest = this.body.getChild("left_chest");
	private final ModelPart rightChest = this.body.getChild("right_chest");

	public DonkeyEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData(float scale) {
		return TexturedModelData.of(AbstractHorseEntityModel.getModelData(Dilation.NONE), 64, 64)
		                        .transform(DONKEY_PARTS_ADDER)
		                        .transform(ModelTransformer.scaling(scale));
	}

	public static TexturedModelData getBabyTexturedModelData(float scale) {
		return TexturedModelData.of(AbstractHorseEntityModel.getBabyModelData(Dilation.NONE), 64, 64)
		                        .transform(DONKEY_PARTS_ADDER)
		                        .transform(BABY_TRANSFORMER)
		                        .transform(ModelTransformer.scaling(scale));
	}

	public static TexturedModelData getSaddleTexturedModelData(float scale, boolean baby) {
		return HorseSaddleEntityModel.getUntransformedTexturedModelData(baby)
		                             .transform(DONKEY_PARTS_ADDER)
		                             .transform(
				                             baby ? AbstractHorseEntityModel.BABY_TRANSFORMER : ModelTransformer.NO_OP)
		                             .transform(ModelTransformer.scaling(scale));
	}

	private static void addDonkeyParts(ModelPartData root) {
		ModelPartData modelPartData = root.getChild("body");
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(26, 21).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F);
		modelPartData.addChild(
				"left_chest",
				modelPartBuilder,
				ModelTransform.of(6.0F, -8.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
		);
		modelPartData.addChild(
				"right_chest",
				modelPartBuilder,
				ModelTransform.of(-6.0F, -8.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
		);
		ModelPartData modelPartData2 = root.getChild("head_parts").getChild("head");
		ModelPartBuilder
				modelPartBuilder2 =
				ModelPartBuilder.create().uv(0, 12).cuboid(-1.0F, -7.0F, 0.0F, 2.0F, 7.0F, 1.0F);
		modelPartData2.addChild(
				"left_ear",
				modelPartBuilder2,
				ModelTransform.of(1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12))
		);
		modelPartData2.addChild(
				"right_ear",
				modelPartBuilder2,
				ModelTransform.of(-1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12))
		);
	}

	public void setAngles(DonkeyEntityRenderState donkeyEntityRenderState) {
		super.setAngles(donkeyEntityRenderState);
		this.leftChest.visible = donkeyEntityRenderState.hasChest;
		this.rightChest.visible = donkeyEntityRenderState.hasChest;
	}
}
