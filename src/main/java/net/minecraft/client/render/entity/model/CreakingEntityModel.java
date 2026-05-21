package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.CreakingAnimations;
import net.minecraft.client.render.entity.state.CreakingEntityRenderState;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code CreakingEntityModel}.
 */
public class CreakingEntityModel extends EntityModel<CreakingEntityRenderState> {

	private final ModelPart head;
	private final Animation walkingAnimation;
	private final Animation attackingAnimation;
	private final Animation invulnerableAnimation;
	private final Animation crumblingAnimation;

	public CreakingEntityModel(ModelPart modelPart) {
		super(modelPart);
		ModelPart modelPart2 = modelPart.getChild("root");
		ModelPart modelPart3 = modelPart2.getChild("upper_body");
		this.head = modelPart3.getChild("head");
		this.walkingAnimation = CreakingAnimations.WALKING.createAnimation(modelPart2);
		this.attackingAnimation = CreakingAnimations.ATTACKING.createAnimation(modelPart2);
		this.invulnerableAnimation = CreakingAnimations.INVULNERABLE.createAnimation(modelPart2);
		this.crumblingAnimation = CreakingAnimations.CRUMBLING.createAnimation(modelPart2);
	}

	private static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 24.0F, 0.0F));
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild(
						"upper_body",
						ModelPartBuilder.create(),
						ModelTransform.origin(-1.0F, -19.0F, 0.0F)
				);
		modelPartData3.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
				                .uv(28, 31)
				                .cuboid(-3.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F)
				                .uv(12, 40)
				                .cuboid(3.0F, -13.0F, 0.0F, 9.0F, 14.0F, 0.0F)
				                .uv(34, 12)
				                .cuboid(-12.0F, -14.0F, 0.0F, 9.0F, 14.0F, 0.0F),
				ModelTransform.origin(-3.0F, -11.0F, 0.0F)
		);
		modelPartData3.addChild(
				"body",
				ModelPartBuilder
						.create()
						.uv(0, 16)
						.cuboid(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F)
						.uv(24, 0)
						.cuboid(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F),
				ModelTransform.origin(0.0F, -7.0F, 1.0F)
		);
		modelPartData3.addChild(
				"right_arm",
				ModelPartBuilder
						.create()
						.uv(22, 13)
						.cuboid(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F)
						.uv(46, 0)
						.cuboid(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F),
				ModelTransform.origin(-7.0F, -9.5F, 1.5F)
		);
		modelPartData3.addChild(
				"left_arm",
				ModelPartBuilder.create()
				                .uv(30, 40)
				                .cuboid(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
				                .uv(52, 12)
				                .cuboid(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
				                .uv(52, 19)
				                .cuboid(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				ModelTransform.origin(6.0F, -9.0F, 0.5F)
		);
		modelPartData2.addChild(
				"left_leg",
				ModelPartBuilder
						.create()
						.uv(42, 40)
						.cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F)
						.uv(45, 55)
						.cuboid(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F),
				ModelTransform.origin(1.5F, -16.0F, 0.5F)
		);
		modelPartData2.addChild(
				"right_leg",
				ModelPartBuilder.create()
				                .uv(0, 34)
				                .cuboid(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
				                .uv(45, 46)
				                .cuboid(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
				                .uv(12, 34)
				                .cuboid(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F),
				ModelTransform.origin(-1.0F, -17.5F, 0.5F)
		);
		return modelData;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		return TexturedModelData.of(modelData, 64, 64);
	}

	public static TexturedModelData getEyesTexturedModelData() {
		ModelData modelData = getModelData();
		modelData.getRoot().resetChildrenExceptExact(Set.of("head"));
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(CreakingEntityRenderState creakingEntityRenderState) {
		super.setAngles(creakingEntityRenderState);
		this.head.pitch = creakingEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = creakingEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		if (creakingEntityRenderState.unrooted) {
			this.walkingAnimation.applyWalking(
					creakingEntityRenderState.limbSwingAnimationProgress,
					creakingEntityRenderState.limbSwingAmplitude,
					1.0F,
					1.0F
			);
		}

		this.attackingAnimation.apply(creakingEntityRenderState.attackAnimationState, creakingEntityRenderState.age);
		this.invulnerableAnimation.apply(
				creakingEntityRenderState.invulnerableAnimationState,
				creakingEntityRenderState.age
		);
		this.crumblingAnimation.apply(creakingEntityRenderState.crumblingAnimationState, creakingEntityRenderState.age);
	}
}
