package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.SnifferAnimations;
import net.minecraft.client.render.entity.state.SnifferEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code SnifferEntityModel}.
 */
public class SnifferEntityModel extends EntityModel<SnifferEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.5F);
	private static final float LIMB_ANGLE_SCALE = 9.0F;
	private static final float LIMB_DISTANCE_SCALE = 100.0F;
	private final ModelPart head;
	private final Animation searchingAnimation;
	private final Animation walkingAnimation;
	private final Animation diggingAnimation;
	private final Animation sniffingAnimation;
	private final Animation risingAnimation;
	private final Animation feelingHappyAnimation;
	private final Animation scentingAnimation;
	private final Animation babyGrowthAnimation;

	public SnifferEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("bone").getChild("body").getChild("head");
		this.searchingAnimation = SnifferAnimations.SEARCHING.createAnimation(modelPart);
		this.walkingAnimation = SnifferAnimations.WALKING.createAnimation(modelPart);
		this.diggingAnimation = SnifferAnimations.DIGGING.createAnimation(modelPart);
		this.sniffingAnimation = SnifferAnimations.SNIFFING.createAnimation(modelPart);
		this.risingAnimation = SnifferAnimations.RISING.createAnimation(modelPart);
		this.feelingHappyAnimation = SnifferAnimations.FEELING_HAPPY.createAnimation(modelPart);
		this.scentingAnimation = SnifferAnimations.SCENTING.createAnimation(modelPart);
		this.babyGrowthAnimation = SnifferAnimations.BABY_GROWTH.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("bone", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 5.0F, 0.0F));
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(62, 68)
				                .cuboid(-12.5F, -14.0F, -20.0F, 25.0F, 29.0F, 40.0F, new Dilation(0.0F))
				                .uv(62, 0)
				                .cuboid(-12.5F, -14.0F, -20.0F, 25.0F, 24.0F, 40.0F, new Dilation(0.5F))
				                .uv(87, 68)
				                .cuboid(-12.5F, 12.0F, -20.0F, 25.0F, 0.0F, 40.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		modelPartData2.addChild(
				"right_front_leg",
				ModelPartBuilder.create().uv(32, 87).cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(-7.5F, 10.0F, -15.0F)
		);
		modelPartData2.addChild(
				"right_mid_leg",
				ModelPartBuilder
						.create()
						.uv(32, 105)
						.cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(-7.5F, 10.0F, 0.0F)
		);
		modelPartData2.addChild(
				"right_hind_leg",
				ModelPartBuilder
						.create()
						.uv(32, 123)
						.cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(-7.5F, 10.0F, 15.0F)
		);
		modelPartData2.addChild(
				"left_front_leg",
				ModelPartBuilder.create().uv(0, 87).cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(7.5F, 10.0F, -15.0F)
		);
		modelPartData2.addChild(
				"left_mid_leg",
				ModelPartBuilder.create().uv(0, 105).cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(7.5F, 10.0F, 0.0F)
		);
		modelPartData2.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().uv(0, 123).cuboid(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(7.5F, 10.0F, 15.0F)
		);
		ModelPartData modelPartData4 = modelPartData3.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(8, 15)
				                .cuboid(-6.5F, -7.5F, -11.5F, 13.0F, 18.0F, 11.0F, new Dilation(0.0F))
				                .uv(8, 4)
				                .cuboid(-6.5F, 7.5F, -11.5F, 13.0F, 0.0F, 11.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 6.5F, -19.48F)
		);
		modelPartData4.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(2, 0).cuboid(0.0F, 0.0F, -3.0F, 1.0F, 19.0F, 7.0F, new Dilation(0.0F)),
				ModelTransform.origin(6.51F, -7.5F, -4.51F)
		);
		modelPartData4.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(48, 0).cuboid(-1.0F, 0.0F, -3.0F, 1.0F, 19.0F, 7.0F, new Dilation(0.0F)),
				ModelTransform.origin(-6.51F, -7.5F, -4.51F)
		);
		modelPartData4.addChild(
				"nose",
				ModelPartBuilder.create().uv(10, 45).cuboid(-6.5F, -2.0F, -9.0F, 13.0F, 2.0F, 9.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -4.5F, -11.5F)
		);
		modelPartData4.addChild(
				"lower_beak",
				ModelPartBuilder
						.create()
						.uv(10, 57)
						.cuboid(-6.5F, -7.0F, -8.0F, 13.0F, 12.0F, 9.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 2.5F, -12.5F)
		);
		return TexturedModelData.of(modelData, 192, 192);
	}

	public void setAngles(SnifferEntityRenderState snifferEntityRenderState) {
		super.setAngles(snifferEntityRenderState);
		this.head.pitch = snifferEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = snifferEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		if (snifferEntityRenderState.searching) {
			this.searchingAnimation.applyWalking(
					snifferEntityRenderState.limbSwingAnimationProgress,
					snifferEntityRenderState.limbSwingAmplitude,
					9.0F,
					LIMB_DISTANCE_SCALE
			);
		}
		else {
			this.walkingAnimation.applyWalking(
					snifferEntityRenderState.limbSwingAnimationProgress,
					snifferEntityRenderState.limbSwingAmplitude,
					9.0F,
					LIMB_DISTANCE_SCALE
			);
		}

		this.diggingAnimation.apply(snifferEntityRenderState.diggingAnimationState, snifferEntityRenderState.age);
		this.sniffingAnimation.apply(snifferEntityRenderState.sniffingAnimationState, snifferEntityRenderState.age);
		this.risingAnimation.apply(snifferEntityRenderState.risingAnimationState, snifferEntityRenderState.age);
		this.feelingHappyAnimation.apply(
				snifferEntityRenderState.feelingHappyAnimationState,
				snifferEntityRenderState.age
		);
		this.scentingAnimation.apply(snifferEntityRenderState.scentingAnimationState, snifferEntityRenderState.age);
		if (snifferEntityRenderState.baby) {
			this.babyGrowthAnimation.applyStatic();
		}
	}
}
