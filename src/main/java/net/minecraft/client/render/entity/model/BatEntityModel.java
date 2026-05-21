package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.BatAnimations;
import net.minecraft.client.render.entity.state.BatEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code BatEntityModel}.
 */
public class BatEntityModel extends EntityModel<BatEntityRenderState> {

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightWingTip;
	private final ModelPart leftWingTip;
	private final ModelPart feet;
	private final Animation flyingAnimation;
	private final Animation roostingAnimation;

	public BatEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityCutout);
		this.body = modelPart.getChild("body");
		this.head = modelPart.getChild("head");
		this.rightWing = this.body.getChild("right_wing");
		this.rightWingTip = this.rightWing.getChild("right_wing_tip");
		this.leftWing = this.body.getChild("left_wing");
		this.leftWingTip = this.leftWing.getChild("left_wing_tip");
		this.feet = this.body.getChild("feet");
		this.flyingAnimation = BatAnimations.FLYING.createAnimation(modelPart);
		this.roostingAnimation = BatAnimations.ROOSTING.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F),
				ModelTransform.origin(0.0F, 17.0F, 0.0F)
		);
		ModelPartData modelPartData3 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 7).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 2.0F),
				ModelTransform.origin(0.0F, 17.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(1, 15).cuboid(-2.5F, -4.0F, 0.0F, 3.0F, 5.0F, 0.0F),
				ModelTransform.origin(-1.5F, -2.0F, 0.0F)
		);
		modelPartData3.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(8, 15).cuboid(-0.1F, -3.0F, 0.0F, 3.0F, 5.0F, 0.0F),
				ModelTransform.origin(1.1F, -3.0F, 0.0F)
		);
		ModelPartData modelPartData4 = modelPartData2.addChild(
				"right_wing",
				ModelPartBuilder.create().uv(12, 0).cuboid(-2.0F, -2.0F, 0.0F, 2.0F, 7.0F, 0.0F),
				ModelTransform.origin(-1.5F, 0.0F, 0.0F)
		);
		modelPartData4.addChild(
				"right_wing_tip",
				ModelPartBuilder.create().uv(16, 0).cuboid(-6.0F, -2.0F, 0.0F, 6.0F, 8.0F, 0.0F),
				ModelTransform.origin(-2.0F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData5 = modelPartData2.addChild(
				"left_wing",
				ModelPartBuilder.create().uv(12, 7).cuboid(0.0F, -2.0F, 0.0F, 2.0F, 7.0F, 0.0F),
				ModelTransform.origin(1.5F, 0.0F, 0.0F)
		);
		modelPartData5.addChild(
				"left_wing_tip",
				ModelPartBuilder.create().uv(16, 8).cuboid(0.0F, -2.0F, 0.0F, 6.0F, 8.0F, 0.0F),
				ModelTransform.origin(2.0F, 0.0F, 0.0F)
		);
		modelPartData2.addChild(
				"feet",
				ModelPartBuilder.create().uv(16, 16).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 2.0F, 0.0F),
				ModelTransform.origin(0.0F, 5.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(BatEntityRenderState batEntityRenderState) {
		super.setAngles(batEntityRenderState);
		if (batEntityRenderState.roosting) {
			this.setRoostingHeadAngles(batEntityRenderState.relativeHeadYaw);
		}

		this.flyingAnimation.apply(batEntityRenderState.flyingAnimationState, batEntityRenderState.age);
		this.roostingAnimation.apply(batEntityRenderState.roostingAnimationState, batEntityRenderState.age);
	}

	private void setRoostingHeadAngles(float yaw) {
		this.head.yaw = yaw * (float) (Math.PI / 180.0);
	}
}
