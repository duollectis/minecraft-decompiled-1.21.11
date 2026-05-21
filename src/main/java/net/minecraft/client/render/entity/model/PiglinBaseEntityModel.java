package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code PiglinBaseEntityModel}.
 */
public class PiglinBaseEntityModel<S extends BipedEntityRenderState> extends BipedEntityModel<S> {

	private static final String LEFT_SLEEVE = "left_sleeve";
	private static final String RIGHT_SLEEVE = "right_sleeve";
	private static final String LEFT_PANTS = "left_pants";
	private static final String RIGHT_PANTS = "right_pants";
	public final ModelPart leftSleeve = this.leftArm.getChild("left_sleeve");
	public final ModelPart rightSleeve = this.rightArm.getChild("right_sleeve");
	public final ModelPart leftPants = this.leftLeg.getChild("left_pants");
	public final ModelPart rightPants = this.rightLeg.getChild("right_pants");
	public final ModelPart jacket = this.body.getChild("jacket");
	public final ModelPart rightEar = this.head.getChild("right_ear");
	public final ModelPart leftEar = this.head.getChild("left_ear");

	public PiglinBaseEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityTranslucent);
	}

	public static ModelData getModelData(Dilation dilation) {
		ModelData modelData = PlayerEntityModel.getTexturedModelData(dilation, false);
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation),
				ModelTransform.NONE
		);
		ModelPartData modelPartData2 = getModelPartData(dilation, modelData);
		modelPartData2.resetChildrenParts("hat");
		return modelData;
	}

	/**
	 * Создаёт equipment model data.
	 *
	 * @param hatDilation hat dilation
	 * @param armorDilation armor dilation
	 *
	 * @return EquipmentModelData — результат операции
	 */
	public static EquipmentModelData<ModelData> createEquipmentModelData(Dilation hatDilation, Dilation armorDilation) {
		return PlayerEntityModel.createEquipmentModelData(hatDilation, armorDilation).map(modelData -> {
			ModelPartData modelPartData = modelData.getRoot();
			ModelPartData modelPartData2 = modelPartData.getChild("head");
			modelPartData2.addChild("left_ear", ModelPartBuilder.create(), ModelTransform.NONE);
			modelPartData2.addChild("right_ear", ModelPartBuilder.create(), ModelTransform.NONE);
			return (ModelData) modelData;
		});
	}

	public static ModelPartData getModelPartData(Dilation dilation, ModelData playerModelData) {
		ModelPartData modelPartData = playerModelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, dilation)
				                .uv(31, 1)
				                .cuboid(-2.0F, -4.0F, -5.0F, 4.0F, 4.0F, 1.0F, dilation)
				                .uv(2, 4)
				                .cuboid(2.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F, dilation)
				                .uv(2, 0)
				                .cuboid(-3.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F, dilation),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(51, 6).cuboid(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, dilation),
				ModelTransform.of(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 6))
		);
		modelPartData2.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(39, 6).cuboid(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, dilation),
				ModelTransform.of(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 6))
		);
		return modelPartData2;
	}

	@Override
	public void setAngles(S bipedEntityRenderState) {
		super.setAngles(bipedEntityRenderState);
		float f = bipedEntityRenderState.limbSwingAnimationProgress;
		float g = bipedEntityRenderState.limbSwingAmplitude;
		float h = (float) (Math.PI / 6);
		float i = bipedEntityRenderState.age * 0.1F + f * 0.5F;
		float j = 0.08F + g * 0.4F;
		this.leftEar.roll = (float) (-Math.PI / 6) - MathHelper.cos(i * 1.2F) * j;
		this.rightEar.roll = (float) (Math.PI / 6) + MathHelper.cos(i) * j;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		this.leftSleeve.visible = visible;
		this.rightSleeve.visible = visible;
		this.leftPants.visible = visible;
		this.rightPants.visible = visible;
		this.jacket.visible = visible;
	}
}
