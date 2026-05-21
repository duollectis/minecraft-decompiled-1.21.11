package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.AllayEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code AllayEntityModel}.
 */
public class AllayEntityModel extends EntityModel<AllayEntityRenderState> implements ModelWithArms<AllayEntityRenderState> {

	private final ModelPart head = this.root.getChild("head");
	private final ModelPart body = this.root.getChild("body");
	private final ModelPart rightArm = this.body.getChild("right_arm");
	private final ModelPart leftArm = this.body.getChild("left_arm");
	private final ModelPart rightWing = this.body.getChild("right_wing");
	private final ModelPart leftWing = this.body.getChild("left_wing");
	private static final float WING_ANGLE = (float) (Math.PI / 4);
	private static final float WING_FOLD_ANGLE = -1.134464F;
	private static final float ARM_ANGLE = (float) (-Math.PI / 3);

	public AllayEntityModel(ModelPart modelPart) {
		super(modelPart.getChild("root"), RenderLayers::entityTranslucent);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 23.5F, 0.0F));
		modelPartData2.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -3.99F, 0.0F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(0, 10)
				                .cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new Dilation(0.0F))
				                .uv(0, 16)
				                .cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F, new Dilation(-0.2F)),
				ModelTransform.origin(0.0F, -4.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_arm",
				ModelPartBuilder
						.create()
						.uv(23, 0)
						.cuboid(-0.75F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new Dilation(-0.01F)),
				ModelTransform.origin(-1.75F, 0.5F, 0.0F)
		);
		modelPartData3.addChild(
				"left_arm",
				ModelPartBuilder
						.create()
						.uv(23, 6)
						.cuboid(-0.25F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new Dilation(-0.01F)),
				ModelTransform.origin(1.75F, 0.5F, 0.0F)
		);
		modelPartData3.addChild(
				"right_wing",
				ModelPartBuilder.create().uv(16, 14).cuboid(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(-0.5F, 0.0F, 0.6F)
		);
		modelPartData3.addChild(
				"left_wing",
				ModelPartBuilder.create().uv(16, 14).cuboid(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.5F, 0.0F, 0.6F)
		);
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(AllayEntityRenderState allayEntityRenderState) {
		super.setAngles(allayEntityRenderState);
		float f = allayEntityRenderState.limbSwingAmplitude;
		float g = allayEntityRenderState.limbSwingAnimationProgress;
		float h = allayEntityRenderState.age * 20.0F * (float) (Math.PI / 180.0) + g;
		float i = MathHelper.cos(h) * (float) Math.PI * 0.15F + f;
		float j = allayEntityRenderState.age * 9.0F * (float) (Math.PI / 180.0);
		float k = Math.min(f / 0.3F, 1.0F);
		float l = 1.0F - k;
		float m = allayEntityRenderState.itemHoldAnimationTicks;
		if (allayEntityRenderState.dancing) {
			float n = allayEntityRenderState.age * 8.0F * (float) (Math.PI / 180.0) + f;
			float o = MathHelper.cos(n) * 16.0F * (float) (Math.PI / 180.0);
			float p = allayEntityRenderState.spinningAnimationTicks;
			float q = MathHelper.cos(n) * 14.0F * (float) (Math.PI / 180.0);
			float r = MathHelper.cos(n) * 30.0F * (float) (Math.PI / 180.0);
			this.root.yaw = allayEntityRenderState.spinning ? (float) (Math.PI * 4) * p : this.root.yaw;
			this.root.roll = o * (1.0F - p);
			this.head.yaw = r * (1.0F - p);
			this.head.roll = q * (1.0F - p);
		}
		else {
			this.head.pitch = allayEntityRenderState.pitch * (float) (Math.PI / 180.0);
			this.head.yaw = allayEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		}

		this.rightWing.pitch = 0.43633232F * (1.0F - k);
		this.rightWing.yaw = (float) (-Math.PI / 4) + i;
		this.leftWing.pitch = 0.43633232F * (1.0F - k);
		this.leftWing.yaw = (float) (Math.PI / 4) - i;
		this.body.pitch = k * (float) (Math.PI / 4);
		float n = m * MathHelper.lerp(k, (float) (-Math.PI / 3), -1.134464F);
		this.root.originY = this.root.originY + (float) Math.cos(j) * 0.25F * l;
		this.rightArm.pitch = n;
		this.leftArm.pitch = n;
		float o = l * (1.0F - m);
		float p = 0.43633232F - MathHelper.cos(j + (float) (Math.PI * 3.0 / 2.0)) * (float) Math.PI * 0.075F * o;
		this.leftArm.roll = -p;
		this.rightArm.roll = p;
		this.rightArm.yaw = 0.27925268F * m;
		this.leftArm.yaw = -0.27925268F * m;
	}

	public void setArmAngle(AllayEntityRenderState allayEntityRenderState, Arm arm, MatrixStack matrixStack) {
		float f = 1.0F;
		float g = 3.0F;
		this.root.applyTransform(matrixStack);
		this.body.applyTransform(matrixStack);
		matrixStack.translate(0.0F, 0.0625F, 0.1875F);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotation(this.rightArm.pitch));
		matrixStack.scale(0.7F, 0.7F, 0.7F);
		matrixStack.translate(0.0625F, 0.0F, 0.0F);
	}
}
