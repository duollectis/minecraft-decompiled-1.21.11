package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code HorseSaddleEntityModel}.
 */
public class HorseSaddleEntityModel extends AbstractHorseEntityModel<LivingHorseEntityRenderState> {

	private static final String SADDLE = "saddle";
	private static final String LEFT_SADDLE_MOUTH = "left_saddle_mouth";
	private static final String LEFT_SADDLE_LINE = "left_saddle_line";
	private static final String RIGHT_SADDLE_MOUTH = "right_saddle_mouth";
	private static final String RIGHT_SADDLE_LINE = "right_saddle_line";
	private static final String HEAD_SADDLE = "head_saddle";
	private static final String MOUTH_SADDLE_WRAP = "mouth_saddle_wrap";
	private final ModelPart[] saddleLines;

	public HorseSaddleEntityModel(ModelPart modelPart) {
		super(modelPart);
		ModelPart modelPart2 = this.head.getChild("left_saddle_line");
		ModelPart modelPart3 = this.head.getChild("right_saddle_line");
		this.saddleLines = new ModelPart[]{modelPart2, modelPart3};
	}

	public static TexturedModelData getTexturedModelData(boolean baby) {
		return getUntransformedTexturedModelData(baby).transform(baby ? BABY_TRANSFORMER : ModelTransformer.NO_OP);
	}

	public static TexturedModelData getUntransformedTexturedModelData(boolean baby) {
		ModelData modelData = baby ? getBabyModelData(Dilation.NONE) : getModelData(Dilation.NONE);
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.getChild("body");
		ModelPartData modelPartData3 = modelPartData.getChild("head_parts");
		modelPartData2.addChild(
				"saddle",
				ModelPartBuilder.create().uv(26, 0).cuboid(-5.0F, -8.0F, -9.0F, 10.0F, 9.0F, 9.0F, new Dilation(0.5F)),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"left_saddle_mouth",
				ModelPartBuilder.create().uv(29, 5).cuboid(2.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"right_saddle_mouth",
				ModelPartBuilder.create().uv(29, 5).cuboid(-3.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"left_saddle_line",
				ModelPartBuilder.create().uv(32, 2).cuboid(3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
				ModelTransform.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_saddle_line",
				ModelPartBuilder.create().uv(32, 2).cuboid(-3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
				ModelTransform.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"head_saddle",
				ModelPartBuilder.create().uv(1, 1).cuboid(-3.0F, -11.0F, -1.9F, 6.0F, 5.0F, 6.0F, new Dilation(0.22F)),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"mouth_saddle_wrap",
				ModelPartBuilder.create().uv(19, 0).cuboid(-2.0F, -11.0F, -4.0F, 4.0F, 5.0F, 2.0F, new Dilation(0.2F)),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(LivingHorseEntityRenderState livingHorseEntityRenderState) {
		super.setAngles(livingHorseEntityRenderState);

		for (ModelPart modelPart : this.saddleLines) {
			modelPart.visible = livingHorseEntityRenderState.hasPassengers;
		}
	}
}
