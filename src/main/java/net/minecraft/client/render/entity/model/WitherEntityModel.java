package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.WitherEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code WitherEntityModel}.
 */
public class WitherEntityModel extends EntityModel<WitherEntityRenderState> {

	private static final String RIBCAGE = "ribcage";
	private static final String CENTER_HEAD = "center_head";
	private static final String RIGHT_HEAD = "right_head";
	private static final String LEFT_HEAD = "left_head";
	private static final float RIBCAGE_PITCH_OFFSET = 0.065F;
	private static final float TAIL_PITCH_OFFSET = 0.265F;
	private final ModelPart centerHead;
	private final ModelPart rightHead;
	private final ModelPart leftHead;
	private final ModelPart ribcage;
	private final ModelPart tail;

	public WitherEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.ribcage = modelPart.getChild("ribcage");
		this.tail = modelPart.getChild("tail");
		this.centerHead = modelPart.getChild("center_head");
		this.rightHead = modelPart.getChild("right_head");
		this.leftHead = modelPart.getChild("left_head");
	}

	public static TexturedModelData getTexturedModelData(Dilation dilation) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"shoulders",
				ModelPartBuilder.create().uv(0, 16).cuboid(-10.0F, 3.9F, -0.5F, 20.0F, 3.0F, 3.0F, dilation),
				ModelTransform.NONE
		);
		float f = 0.20420352F;
		modelPartData.addChild(
				"ribcage",
				ModelPartBuilder.create()
				                .uv(0, 22)
				                .cuboid(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F, dilation)
				                .uv(24, 22)
				                .cuboid(-4.0F, 1.5F, 0.5F, 11.0F, 2.0F, 2.0F, dilation)
				                .uv(24, 22)
				                .cuboid(-4.0F, 4.0F, 0.5F, 11.0F, 2.0F, 2.0F, dilation)
				                .uv(24, 22)
				                .cuboid(-4.0F, 6.5F, 0.5F, 11.0F, 2.0F, 2.0F, dilation),
				ModelTransform.of(-2.0F, 6.9F, -0.5F, 0.20420352F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"tail",
				ModelPartBuilder.create().uv(12, 22).cuboid(0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F, dilation),
				ModelTransform.of(
						-2.0F,
						6.9F + MathHelper.cos(0.20420352F) * 10.0F,
						-0.5F + MathHelper.sin(0.20420352F) * 10.0F,
						0.83252203F,
						0.0F,
						0.0F
				)
		);
		modelPartData.addChild(
				"center_head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation),
				ModelTransform.NONE
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -4.0F, -4.0F, 6.0F, 6.0F, 6.0F, dilation);
		modelPartData.addChild("right_head", modelPartBuilder, ModelTransform.origin(-8.0F, 4.0F, 0.0F));
		modelPartData.addChild("left_head", modelPartBuilder, ModelTransform.origin(10.0F, 4.0F, 0.0F));
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(WitherEntityRenderState witherEntityRenderState) {
		super.setAngles(witherEntityRenderState);
		rotateHead(witherEntityRenderState, this.rightHead, 0);
		rotateHead(witherEntityRenderState, this.leftHead, 1);
		float f = MathHelper.cos(witherEntityRenderState.age * 0.1F);
		this.ribcage.pitch = (0.065F + 0.05F * f) * (float) Math.PI;
		this.tail.setOrigin(
				-2.0F,
				6.9F + MathHelper.cos(this.ribcage.pitch) * 10.0F,
				-0.5F + MathHelper.sin(this.ribcage.pitch) * 10.0F
		);
		this.tail.pitch = (0.265F + 0.1F * f) * (float) Math.PI;
		this.centerHead.yaw = witherEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.centerHead.pitch = witherEntityRenderState.pitch * (float) (Math.PI / 180.0);
	}

	private static void rotateHead(WitherEntityRenderState witherEntityRenderState, ModelPart head, int sigma) {
		head.yaw =
				(witherEntityRenderState.sideHeadYaws[sigma] - witherEntityRenderState.bodyYaw) * (float) (Math.PI
						/ 180.0
				);
		head.pitch = witherEntityRenderState.sideHeadPitches[sigma] * (float) (Math.PI / 180.0);
	}
}
