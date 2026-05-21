package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code VillagerResemblingModel}.
 */
public class VillagerResemblingModel extends EntityModel<VillagerEntityRenderState> implements ModelWithHead, ModelWithHat<VillagerEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.5F);
	private final ModelPart head;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart arms;

	public VillagerResemblingModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.rightLeg = modelPart.getChild("right_leg");
		this.leftLeg = modelPart.getChild("left_leg");
		this.arms = modelPart.getChild("arms");
	}

	public static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = 0.5F;
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
				ModelTransform.NONE
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"hat",
				ModelPartBuilder
						.create()
						.uv(32, 0)
						.cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new Dilation(0.51F)),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"hat_rim",
				ModelPartBuilder.create().uv(30, 47).cuboid(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
				ModelTransform.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
		);
		modelPartData2.addChild(
				"nose",
				ModelPartBuilder.create().uv(24, 0).cuboid(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
				ModelTransform.origin(0.0F, -2.0F, 0.0F)
		);
		ModelPartData modelPartData4 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(16, 20).cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F),
				ModelTransform.NONE
		);
		modelPartData4.addChild(
				"jacket",
				ModelPartBuilder.create().uv(0, 38).cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new Dilation(0.5F)),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"arms",
				ModelPartBuilder.create()
				                .uv(44, 22)
				                .cuboid(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				                .uv(44, 22)
				                .cuboid(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
				                .uv(40, 38)
				                .cuboid(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
				ModelTransform.of(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				ModelTransform.origin(-2.0F, 12.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				ModelTransform.origin(2.0F, 12.0F, 0.0F)
		);
		return modelData;
	}

	public static ModelData getNoHatModelData() {
		ModelData modelData = getModelData();
		modelData.getRoot().resetChildrenParts("head").resetChildrenParts();
		return modelData;
	}

	public void setAngles(VillagerEntityRenderState villagerEntityRenderState) {
		super.setAngles(villagerEntityRenderState);
		this.head.yaw = villagerEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.head.pitch = villagerEntityRenderState.pitch * (float) (Math.PI / 180.0);
		if (villagerEntityRenderState.headRolling) {
			this.head.roll = 0.3F * MathHelper.sin(0.45F * villagerEntityRenderState.age);
			this.head.pitch = 0.4F;
		}
		else {
			this.head.roll = 0.0F;
		}

		this.rightLeg.pitch = MathHelper.cos(villagerEntityRenderState.limbSwingAnimationProgress * 0.6662F)
				* 1.4F
				* villagerEntityRenderState.limbSwingAmplitude
				* 0.5F;
		this.leftLeg.pitch =
				MathHelper.cos(villagerEntityRenderState.limbSwingAnimationProgress * 0.6662F + (float) Math.PI)
						* 1.4F
						* villagerEntityRenderState.limbSwingAmplitude
						* 0.5F;
		this.rightLeg.yaw = 0.0F;
		this.leftLeg.yaw = 0.0F;
	}

	@Override
	public ModelPart getHead() {
		return this.head;
	}

	public void rotateArms(VillagerEntityRenderState villagerEntityRenderState, MatrixStack matrixStack) {
		this.root.applyTransform(matrixStack);
		this.arms.applyTransform(matrixStack);
	}
}
