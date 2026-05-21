package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code SnowGolemEntityModel}.
 */
public class SnowGolemEntityModel extends EntityModel<LivingEntityRenderState> {

	private static final String UPPER_BODY = "upper_body";
	private final ModelPart upperBody;
	private final ModelPart head;
	private final ModelPart leftArm;
	private final ModelPart rightArm;

	public SnowGolemEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.leftArm = modelPart.getChild("left_arm");
		this.rightArm = modelPart.getChild("right_arm");
		this.upperBody = modelPart.getChild("upper_body");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = 4.0F;
		Dilation dilation = new Dilation(-0.5F);
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation),
				ModelTransform.origin(0.0F, 4.0F, 0.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(32, 0).cuboid(-1.0F, 0.0F, -1.0F, 12.0F, 2.0F, 2.0F, dilation);
		modelPartData.addChild("left_arm", modelPartBuilder, ModelTransform.of(5.0F, 6.0F, 1.0F, 0.0F, 0.0F, 1.0F));
		modelPartData.addChild(
				"right_arm",
				modelPartBuilder,
				ModelTransform.of(-5.0F, 6.0F, -1.0F, 0.0F, (float) Math.PI, -1.0F)
		);
		modelPartData.addChild(
				"upper_body",
				ModelPartBuilder.create().uv(0, 16).cuboid(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, dilation),
				ModelTransform.origin(0.0F, 13.0F, 0.0F)
		);
		modelPartData.addChild(
				"lower_body",
				ModelPartBuilder.create().uv(0, 36).cuboid(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, dilation),
				ModelTransform.origin(0.0F, 24.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(LivingEntityRenderState livingEntityRenderState) {
		super.setAngles(livingEntityRenderState);
		this.head.yaw = livingEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.head.pitch = livingEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.upperBody.yaw = livingEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0) * 0.25F;
		float f = MathHelper.sin(this.upperBody.yaw);
		float g = MathHelper.cos(this.upperBody.yaw);
		this.leftArm.yaw = this.upperBody.yaw;
		this.rightArm.yaw = this.upperBody.yaw + (float) Math.PI;
		this.leftArm.originX = g * 5.0F;
		this.leftArm.originZ = -f * 5.0F;
		this.rightArm.originX = -g * 5.0F;
		this.rightArm.originZ = f * 5.0F;
	}

	public ModelPart getHead() {
		return this.head;
	}
}
