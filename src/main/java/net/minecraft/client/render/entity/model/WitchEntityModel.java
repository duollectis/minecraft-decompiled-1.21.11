package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.WitchEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code WitchEntityModel}.
 */
public class WitchEntityModel extends EntityModel<WitchEntityRenderState> implements ModelWithHead, ModelWithHat<WitchEntityRenderState> {

	protected final ModelPart nose;
	private final ModelPart head;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart arms;

	public WitchEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.nose = this.head.getChild("nose");
		this.rightLeg = modelPart.getChild("right_leg");
		this.leftLeg = modelPart.getChild("left_leg");
		this.arms = modelPart.getChild("arms");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = VillagerResemblingModel.getModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
				ModelTransform.NONE
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"hat",
				ModelPartBuilder.create().uv(0, 64).cuboid(0.0F, 0.0F, 0.0F, 10.0F, 2.0F, 10.0F),
				ModelTransform.origin(-5.0F, -10.03125F, -5.0F)
		);
		ModelPartData modelPartData4 = modelPartData3.addChild(
				"hat2",
				ModelPartBuilder.create().uv(0, 76).cuboid(0.0F, 0.0F, 0.0F, 7.0F, 4.0F, 7.0F),
				ModelTransform.of(1.75F, -4.0F, 2.0F, -0.05235988F, 0.0F, 0.02617994F)
		);
		ModelPartData modelPartData5 = modelPartData4.addChild(
				"hat3",
				ModelPartBuilder.create().uv(0, 87).cuboid(0.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F),
				ModelTransform.of(1.75F, -4.0F, 2.0F, -0.10471976F, 0.0F, 0.05235988F)
		);
		modelPartData5.addChild(
				"hat4",
				ModelPartBuilder.create().uv(0, 95).cuboid(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new Dilation(0.25F)),
				ModelTransform.of(1.75F, -2.0F, 2.0F, (float) (-Math.PI / 15), 0.0F, 0.10471976F)
		);
		ModelPartData modelPartData6 = modelPartData2.getChild("nose");
		modelPartData6.addChild(
				"mole",
				ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 3.0F, -6.75F, 1.0F, 1.0F, 1.0F, new Dilation(-0.25F)),
				ModelTransform.origin(0.0F, -2.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 128);
	}

	public void setAngles(WitchEntityRenderState witchEntityRenderState) {
		super.setAngles(witchEntityRenderState);
		this.head.yaw = witchEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.head.pitch = witchEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.rightLeg.pitch = MathHelper.cos(witchEntityRenderState.limbSwingAnimationProgress * 0.6662F)
				* 1.4F
				* witchEntityRenderState.limbSwingAmplitude
				* 0.5F;
		this.leftLeg.pitch =
				MathHelper.cos(witchEntityRenderState.limbSwingAnimationProgress * 0.6662F + (float) Math.PI)
						* 1.4F
						* witchEntityRenderState.limbSwingAmplitude
						* 0.5F;
		float f = 0.01F * (witchEntityRenderState.id % 10);
		this.nose.pitch = MathHelper.sin(witchEntityRenderState.age * f) * 4.5F * (float) (Math.PI / 180.0);
		this.nose.roll = MathHelper.cos(witchEntityRenderState.age * f) * 2.5F * (float) (Math.PI / 180.0);
		if (witchEntityRenderState.holdingItem) {
			this.nose.setOrigin(0.0F, 1.0F, -1.5F);
			this.nose.pitch = -0.9F;
		}
	}

	public ModelPart getNose() {
		return this.nose;
	}

	@Override
	public ModelPart getHead() {
		return this.head;
	}

	public void rotateArms(WitchEntityRenderState witchEntityRenderState, MatrixStack matrixStack) {
		this.root.applyTransform(matrixStack);
		this.arms.applyTransform(matrixStack);
	}
}
