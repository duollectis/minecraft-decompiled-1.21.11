package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.BreezeAnimations;
import net.minecraft.client.render.entity.state.BreezeEntityRenderState;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code BreezeEntityModel}.
 */
public class BreezeEntityModel extends EntityModel<BreezeEntityRenderState> {

	private static final float WIND_SCALE_MIN = 0.6F;
	private static final float WIND_SCALE_MID = 0.8F;
	private static final float WIND_SCALE_MAX = 1.0F;
	private final ModelPart head;
	private final ModelPart eyes;
	private final ModelPart windBody;
	private final ModelPart windTop;
	private final ModelPart windMid;
	private final ModelPart windBottom;
	private final ModelPart rods;
	private final Animation idlingAnimation;
	private final Animation shootingAnimation;
	private final Animation slidingAnimation;
	private final Animation slidingBackAnimation;
	private final Animation inhalingAnimation;
	private final Animation longJumpingAnimation;

	public BreezeEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityTranslucent);
		this.windBody = modelPart.getChild("wind_body");
		this.windBottom = this.windBody.getChild("wind_bottom");
		this.windMid = this.windBottom.getChild("wind_mid");
		this.windTop = this.windMid.getChild("wind_top");
		this.head = modelPart.getChild("body").getChild("head");
		this.eyes = this.head.getChild("eyes");
		this.rods = modelPart.getChild("body").getChild("rods");
		this.idlingAnimation = BreezeAnimations.IDLING.createAnimation(modelPart);
		this.shootingAnimation = BreezeAnimations.SHOOTING.createAnimation(modelPart);
		this.slidingAnimation = BreezeAnimations.SLIDING.createAnimation(modelPart);
		this.slidingBackAnimation = BreezeAnimations.SLIDING_BACK.createAnimation(modelPart);
		this.inhalingAnimation = BreezeAnimations.INHALING.createAnimation(modelPart);
		this.longJumpingAnimation = BreezeAnimations.LONG_JUMPING.createAnimation(modelPart);
	}

	private static ModelData createModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("body", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 0.0F, 0.0F));
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild("rods", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 8.0F, 0.0F));
		modelPartData3.addChild(
				"rod_1",
				ModelPartBuilder.create().uv(0, 17).cuboid(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.of(2.5981F, -3.0F, 1.5F, -2.7489F, -1.0472F, 3.1416F)
		);
		modelPartData3.addChild(
				"rod_2",
				ModelPartBuilder.create().uv(0, 17).cuboid(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.of(-2.5981F, -3.0F, 1.5F, -2.7489F, 1.0472F, 3.1416F)
		);
		modelPartData3.addChild(
				"rod_3",
				ModelPartBuilder.create().uv(0, 17).cuboid(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, -3.0F, -3.0F, 0.3927F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData4 = modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(4, 24)
				                .cuboid(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, new Dilation(0.0F))
				                .uv(0, 0)
				                .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 4.0F, 0.0F)
		);
		modelPartData4.addChild(
				"eyes",
				ModelPartBuilder.create()
				                .uv(4, 24)
				                .cuboid(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, new Dilation(0.0F))
				                .uv(0, 0)
				                .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData.addChild("wind_body", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 0.0F, 0.0F));
		ModelPartData modelPartData6 = modelPartData5.addChild(
				"wind_bottom",
				ModelPartBuilder.create().uv(1, 83).cuboid(-2.5F, -7.0F, -2.5F, 5.0F, 7.0F, 5.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 24.0F, 0.0F)
		);
		ModelPartData modelPartData7 = modelPartData6.addChild(
				"wind_mid",
				ModelPartBuilder.create()
				                .uv(74, 28)
				                .cuboid(-6.0F, -6.0F, -6.0F, 12.0F, 6.0F, 12.0F, new Dilation(0.0F))
				                .uv(78, 32)
				                .cuboid(-4.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F, new Dilation(0.0F))
				                .uv(49, 71)
				                .cuboid(-2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -7.0F, 0.0F)
		);
		modelPartData7.addChild(
				"wind_top",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-9.0F, -8.0F, -9.0F, 18.0F, 8.0F, 18.0F, new Dilation(0.0F))
				                .uv(6, 6)
				                .cuboid(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F, new Dilation(0.0F))
				                .uv(105, 57)
				                .cuboid(-2.5F, -8.0F, -2.5F, 5.0F, 8.0F, 5.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -6.0F, 0.0F)
		);
		return modelData;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = createModelData();
		modelData.getRoot().resetChildrenExcept(Set.of("head", "rods"));
		return TexturedModelData.of(modelData, 32, 32);
	}

	public static TexturedModelData getWindTexturedModelData() {
		ModelData modelData = createModelData();
		modelData.getRoot().resetChildrenExcept(Set.of("wind_body"));
		return TexturedModelData.of(modelData, 128, 128);
	}

	public static TexturedModelData getEyesTexturedModelData() {
		ModelData modelData = createModelData();
		modelData.getRoot().resetChildrenExcept(Set.of("eyes"));
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(BreezeEntityRenderState breezeEntityRenderState) {
		super.setAngles(breezeEntityRenderState);
		this.idlingAnimation.apply(breezeEntityRenderState.idleAnimationState, breezeEntityRenderState.age);
		this.shootingAnimation.apply(breezeEntityRenderState.shootingAnimationState, breezeEntityRenderState.age);
		this.slidingAnimation.apply(breezeEntityRenderState.slidingAnimationState, breezeEntityRenderState.age);
		this.slidingBackAnimation.apply(breezeEntityRenderState.slidingBackAnimationState, breezeEntityRenderState.age);
		this.inhalingAnimation.apply(breezeEntityRenderState.inhalingAnimationState, breezeEntityRenderState.age);
		this.longJumpingAnimation.apply(breezeEntityRenderState.longJumpingAnimationState, breezeEntityRenderState.age);
	}

	public ModelPart getHead() {
		return this.head;
	}

	public ModelPart getEyes() {
		return this.eyes;
	}

	public ModelPart getRods() {
		return this.rods;
	}

	public ModelPart getWindBody() {
		return this.windBody;
	}
}
