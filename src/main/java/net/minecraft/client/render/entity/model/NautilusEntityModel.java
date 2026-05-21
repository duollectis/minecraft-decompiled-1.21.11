package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.NautilusAnimations;
import net.minecraft.client.render.entity.state.NautilusEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code NautilusEntityModel}.
 */
public class NautilusEntityModel extends EntityModel<NautilusEntityRenderState> {

	private static final float WALK_ANIMATION_SPEED = 2.0F;
	private static final float WALK_ANIMATION_AMPLITUDE = 3.0F;
	private static final float WALK_AMPLITUDE_OFFSET = 0.2F;
	private static final float AGE_ANIMATION_DIVISOR = 5.0F;
	protected final ModelPart body;
	protected final ModelPart nautilusRoot;
	private final Animation animation;

	public NautilusEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.nautilusRoot = modelPart.getChild("root");
		this.body = this.nautilusRoot.getChild("body");
		this.animation = NautilusAnimations.ANIMATION.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		return TexturedModelData.of(getModelData(), 128, 128);
	}

	public static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 29.0F, -6.0F));
		modelPartData2.addChild(
				"shell",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F, new Dilation(0.0F))
				                .uv(0, 26)
				                .cuboid(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F, new Dilation(0.0F))
				                .uv(48, 26)
				                .cuboid(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -13.0F, 5.0F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(0, 54)
				                .cuboid(-5.0F, -4.51F, -3.0F, 10.0F, 8.0F, 14.0F, new Dilation(0.0F))
				                .uv(0, 76)
				                .cuboid(-5.0F, -4.51F, 7.0F, 10.0F, 8.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -8.5F, 12.3F)
		);
		modelPartData3.addChild(
				"upper_mouth",
				ModelPartBuilder
						.create()
						.uv(54, 54)
						.cuboid(-5.0F, -2.0F, 0.0F, 10.0F, 4.0F, 4.0F, new Dilation(-0.001F)),
				ModelTransform.origin(0.0F, -2.51F, 7.0F)
		);
		modelPartData3.addChild(
				"inner_mouth",
				ModelPartBuilder.create().uv(54, 70).cuboid(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -0.51F, 7.5F)
		);
		modelPartData3.addChild(
				"lower_mouth",
				ModelPartBuilder
						.create()
						.uv(54, 62)
						.cuboid(-5.0F, -1.98F, 0.0F, 10.0F, 4.0F, 4.0F, new Dilation(-0.001F)),
				ModelTransform.origin(0.0F, 1.49F, 7.0F)
		);
		return modelData;
	}

	public static TexturedModelData getBabyTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(-0.5F, 28.0F, -0.5F));
		modelPartData2.addChild(
				"shell",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-6.0F, -4.0F, -1.0F, 7.0F, 4.0F, 7.0F, new Dilation(0.0F))
				                .uv(0, 11)
				                .cuboid(-6.0F, 0.0F, -1.0F, 7.0F, 4.0F, 9.0F, new Dilation(0.0F))
				                .uv(23, 11)
				                .cuboid(-6.0F, 0.0F, 5.0F, 7.0F, 4.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.origin(3.0F, -8.0F, -2.0F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(0, 24)
				                .cuboid(-2.5F, -3.01F, -1.0F, 5.0F, 4.0F, 7.0F, new Dilation(0.0F))
				                .uv(0, 35)
				                .cuboid(-2.5F, -3.01F, 4.1F, 5.0F, 4.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.5F, -5.0F, 3.0F)
		);
		modelPartData3.addChild(
				"upper_mouth",
				ModelPartBuilder
						.create()
						.uv(24, 24)
						.cuboid(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, new Dilation(-0.001F)),
				ModelTransform.origin(0.0F, -2.01F, 3.9F)
		);
		modelPartData3.addChild(
				"inner_mouth",
				ModelPartBuilder.create().uv(24, 32).cuboid(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -1.01F, 4.9F)
		);
		modelPartData3.addChild(
				"lower_mouth",
				ModelPartBuilder
						.create()
						.uv(24, 28)
						.cuboid(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, new Dilation(-0.001F)),
				ModelTransform.origin(0.0F, -0.01F, 3.9F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(NautilusEntityRenderState nautilusEntityRenderState) {
		super.setAngles(nautilusEntityRenderState);
		this.setHeadAngles(nautilusEntityRenderState.relativeHeadYaw, nautilusEntityRenderState.pitch);
		this.animation
				.applyWalking(
						nautilusEntityRenderState.limbSwingAnimationProgress + nautilusEntityRenderState.age / 5.0F,
						nautilusEntityRenderState.limbSwingAmplitude + 0.2F,
						2.0F,
						3.0F
				);
	}

	private void setHeadAngles(float yaw, float pitch) {
		yaw = MathHelper.clamp(yaw, -10.0F, 10.0F);
		pitch = MathHelper.clamp(pitch, -10.0F, 10.0F);
		this.body.yaw = yaw * (float) (Math.PI / 180.0);
		this.body.pitch = pitch * (float) (Math.PI / 180.0);
	}
}
