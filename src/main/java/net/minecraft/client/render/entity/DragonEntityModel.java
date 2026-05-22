package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EnderDragonEntityRenderState;
import net.minecraft.entity.boss.dragon.EnderDragonFrameTracker;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code DragonEntityModel}.
 */
public class DragonEntityModel extends EntityModel<EnderDragonEntityRenderState> {

	private static final int NUM_NECK_PARTS = 5;
	private static final int NUM_TAIL_PARTS = 12;
	private final ModelPart head;
	private final ModelPart[] neckParts = new ModelPart[5];
	private final ModelPart[] tailParts = new ModelPart[NUM_TAIL_PARTS];
	private final ModelPart jaw;
	private final ModelPart body;
	private final ModelPart leftWing;
	private final ModelPart leftWingTip;
	private final ModelPart leftFrontLeg;
	private final ModelPart leftFrontLegTip;
	private final ModelPart leftFrontFoot;
	private final ModelPart leftHindLeg;
	private final ModelPart leftHindLegTip;
	private final ModelPart leftHindFoot;
	private final ModelPart rightWing;
	private final ModelPart rightWingTip;
	private final ModelPart rightFrontLeg;
	private final ModelPart rightFrontLegTip;
	private final ModelPart rightFrontFoot;
	private final ModelPart rightHindLeg;
	private final ModelPart rightHindLegTip;
	private final ModelPart rightHindFoot;

	private static String neck(int id) {
		return "neck" + id;
	}

	private static String tail(int id) {
		return "tail" + id;
	}

	public DragonEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.jaw = this.head.getChild("jaw");

		for (int i = 0; i < this.neckParts.length; i++) {
			this.neckParts[i] = modelPart.getChild(neck(i));
		}

		for (int i = 0; i < this.tailParts.length; i++) {
			this.tailParts[i] = modelPart.getChild(tail(i));
		}

		this.body = modelPart.getChild("body");
		this.leftWing = this.body.getChild("left_wing");
		this.leftWingTip = this.leftWing.getChild("left_wing_tip");
		this.leftFrontLeg = this.body.getChild("left_front_leg");
		this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
		this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
		this.leftHindLeg = this.body.getChild("left_hind_leg");
		this.leftHindLegTip = this.leftHindLeg.getChild("left_hind_leg_tip");
		this.leftHindFoot = this.leftHindLegTip.getChild("left_hind_foot");
		this.rightWing = this.body.getChild("right_wing");
		this.rightWingTip = this.rightWing.getChild("right_wing_tip");
		this.rightFrontLeg = this.body.getChild("right_front_leg");
		this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
		this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
		this.rightHindLeg = this.body.getChild("right_hind_leg");
		this.rightHindLegTip = this.rightHindLeg.getChild("right_hind_leg_tip");
		this.rightHindFoot = this.rightHindLegTip.getChild("right_hind_foot");
	}

	/**
	 * Создаёт textured model data.
	 *
	 * @return TexturedModelData — результат операции
	 */
	public static TexturedModelData createTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = -16.0F;
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create()
				                .cuboid("upperlip", -6.0F, -1.0F, -24.0F, NUM_TAIL_PARTS, 5, 16, 176, 44)
				                .cuboid("upperhead", -8.0F, -8.0F, -10.0F, 16, 16, 16, 112, 30)
				                .mirrored()
				                .cuboid("scale", -5.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
				                .cuboid("nostril", -5.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0)
				                .mirrored()
				                .cuboid("scale", 3.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
				                .cuboid("nostril", 3.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0),
				ModelTransform.origin(0.0F, 20.0F, -62.0F)
		);
		modelPartData2.addChild(
				"jaw",
				ModelPartBuilder.create().cuboid("jaw", -6.0F, 0.0F, -16.0F, NUM_TAIL_PARTS, 4, 16, 176, 65),
				ModelTransform.origin(0.0F, 4.0F, -8.0F)
		);
		ModelPartBuilder modelPartBuilder = ModelPartBuilder.create()
		                                                    .cuboid("box", -5.0F, -5.0F, -5.0F, 10, 10, 10, 192, 104)
		                                                    .cuboid("scale", -1.0F, -9.0F, -3.0F, 2, 4, 6, 48, 0);

		for (int i = 0; i < 5; i++) {
			modelPartData.addChild(neck(i), modelPartBuilder, ModelTransform.origin(0.0F, 20.0F, -12.0F - i * 10.0F));
		}

		for (int i = 0; i < NUM_TAIL_PARTS; i++) {
			modelPartData.addChild(tail(i), modelPartBuilder, ModelTransform.origin(0.0F, 10.0F, 60.0F + i * 10.0F));
		}

		ModelPartData modelPartData3 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create()
				                .cuboid("body", -12.0F, 1.0F, -16.0F, 24, 24, 64, 0, 0)
				                .cuboid("scale", -1.0F, -5.0F, -10.0F, 2, 6, NUM_TAIL_PARTS, 220, 53)
				                .cuboid("scale", -1.0F, -5.0F, 10.0F, 2, 6, NUM_TAIL_PARTS, 220, 53)
				                .cuboid("scale", -1.0F, -5.0F, 30.0F, 2, 6, NUM_TAIL_PARTS, 220, 53),
				ModelTransform.origin(0.0F, 3.0F, 8.0F)
		);
		ModelPartData modelPartData4 = modelPartData3.addChild(
				"left_wing",
				ModelPartBuilder
						.create()
						.mirrored()
						.cuboid("bone", 0.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88)
						.cuboid("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
				ModelTransform.origin(12.0F, 2.0F, -6.0F)
		);
		modelPartData4.addChild(
				"left_wing_tip",
				ModelPartBuilder
						.create()
						.mirrored()
						.cuboid("bone", 0.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136)
						.cuboid("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
				ModelTransform.origin(56.0F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData5 = modelPartData3.addChild(
				"left_front_leg",
				ModelPartBuilder.create().cuboid("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104),
				ModelTransform.of(12.0F, 17.0F, -6.0F, 1.3F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData6 = modelPartData5.addChild(
				"left_front_leg_tip",
				ModelPartBuilder.create().cuboid("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138),
				ModelTransform.of(0.0F, 20.0F, -1.0F, -0.5F, 0.0F, 0.0F)
		);
		modelPartData6.addChild(
				"left_front_foot",
				ModelPartBuilder.create().cuboid("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104),
				ModelTransform.of(0.0F, 23.0F, 0.0F, 0.75F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData7 = modelPartData3.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().cuboid("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0),
				ModelTransform.of(16.0F, 13.0F, 34.0F, 1.0F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData8 = modelPartData7.addChild(
				"left_hind_leg_tip",
				ModelPartBuilder.create().cuboid("main", -6.0F, -2.0F, 0.0F, NUM_TAIL_PARTS, 32, NUM_TAIL_PARTS, 196, 0),
				ModelTransform.of(0.0F, 32.0F, -4.0F, 0.5F, 0.0F, 0.0F)
		);
		modelPartData8.addChild(
				"left_hind_foot",
				ModelPartBuilder.create().cuboid("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0),
				ModelTransform.of(0.0F, 31.0F, 4.0F, 0.75F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData9 = modelPartData3.addChild(
				"right_wing",
				ModelPartBuilder
						.create()
						.cuboid("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88)
						.cuboid("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
				ModelTransform.origin(-12.0F, 2.0F, -6.0F)
		);
		modelPartData9.addChild(
				"right_wing_tip",
				ModelPartBuilder
						.create()
						.cuboid("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136)
						.cuboid("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
				ModelTransform.origin(-56.0F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData10 = modelPartData3.addChild(
				"right_front_leg",
				ModelPartBuilder.create().cuboid("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104),
				ModelTransform.of(-12.0F, 17.0F, -6.0F, 1.3F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData11 = modelPartData10.addChild(
				"right_front_leg_tip",
				ModelPartBuilder.create().cuboid("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138),
				ModelTransform.of(0.0F, 20.0F, -1.0F, -0.5F, 0.0F, 0.0F)
		);
		modelPartData11.addChild(
				"right_front_foot",
				ModelPartBuilder.create().cuboid("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104),
				ModelTransform.of(0.0F, 23.0F, 0.0F, 0.75F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData12 = modelPartData3.addChild(
				"right_hind_leg",
				ModelPartBuilder.create().cuboid("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0),
				ModelTransform.of(-16.0F, 13.0F, 34.0F, 1.0F, 0.0F, 0.0F)
		);
		ModelPartData modelPartData13 = modelPartData12.addChild(
				"right_hind_leg_tip",
				ModelPartBuilder.create().cuboid("main", -6.0F, -2.0F, 0.0F, NUM_TAIL_PARTS, 32, NUM_TAIL_PARTS, 196, 0),
				ModelTransform.of(0.0F, 32.0F, -4.0F, 0.5F, 0.0F, 0.0F)
		);
		modelPartData13.addChild(
				"right_hind_foot",
				ModelPartBuilder.create().cuboid("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0),
				ModelTransform.of(0.0F, 31.0F, 4.0F, 0.75F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 256, 256);
	}

	public void setAngles(EnderDragonEntityRenderState enderDragonEntityRenderState) {
		super.setAngles(enderDragonEntityRenderState);
		float f = enderDragonEntityRenderState.wingPosition * (float) (Math.PI * 2);
		this.jaw.pitch = (MathHelper.sin(f) + 1.0F) * 0.2F;
		float g = MathHelper.sin(f - 1.0F) + 1.0F;
		g = (g * g + g * 2.0F) * 0.05F;
		this.root.originY = (g - 2.0F) * 16.0F;
		this.root.originZ = -48.0F;
		this.root.pitch = g * 2.0F * (float) (Math.PI / 180.0);
		float h = this.neckParts[0].originX;
		float i = this.neckParts[0].originY;
		float j = this.neckParts[0].originZ;
		float k = 1.5F;
		EnderDragonFrameTracker.Frame frame = enderDragonEntityRenderState.getLerpedFrame(6);
		float
				l =
				MathHelper.wrapDegrees(
						enderDragonEntityRenderState.getLerpedFrame(5).yRot() - enderDragonEntityRenderState
								.getLerpedFrame(10)
								.yRot());
		float m = MathHelper.wrapDegrees(enderDragonEntityRenderState.getLerpedFrame(5).yRot() + l / 2.0F);

		for (int n = 0; n < 5; n++) {
			ModelPart modelPart = this.neckParts[n];
			EnderDragonFrameTracker.Frame frame2 = enderDragonEntityRenderState.getLerpedFrame(5 - n);
			float o = MathHelper.cos(n * 0.45F + f) * 0.15F;
			modelPart.yaw = MathHelper.wrapDegrees(frame2.yRot() - frame.yRot()) * (float) (Math.PI / 180.0) * 1.5F;
			modelPart.pitch =
					o + enderDragonEntityRenderState.getNeckPartPitchOffset(n, frame, frame2) * (float) (Math.PI
							/ 180.0
					) * 1.5F * 5.0F;
			modelPart.roll = -MathHelper.wrapDegrees(frame2.yRot() - m) * (float) (Math.PI / 180.0) * 1.5F;
			modelPart.originY = i;
			modelPart.originZ = j;
			modelPart.originX = h;
			h -= MathHelper.sin(modelPart.yaw) * MathHelper.cos(modelPart.pitch) * 10.0F;
			i += MathHelper.sin(modelPart.pitch) * 10.0F;
			j -= MathHelper.cos(modelPart.yaw) * MathHelper.cos(modelPart.pitch) * 10.0F;
		}

		this.head.originY = i;
		this.head.originZ = j;
		this.head.originX = h;
		EnderDragonFrameTracker.Frame frame3 = enderDragonEntityRenderState.getLerpedFrame(0);
		this.head.yaw = MathHelper.wrapDegrees(frame3.yRot() - frame.yRot()) * (float) (Math.PI / 180.0);
		this.head.pitch =
				MathHelper.wrapDegrees(enderDragonEntityRenderState.getNeckPartPitchOffset(6, frame, frame3))
						* (float) (Math.PI / 180.0) * 1.5F * 5.0F;
		this.head.roll = -MathHelper.wrapDegrees(frame3.yRot() - m) * (float) (Math.PI / 180.0);
		this.body.roll = -l * 1.5F * (float) (Math.PI / 180.0);
		this.leftWing.pitch = 0.125F - MathHelper.cos(f) * 0.2F;
		this.leftWing.yaw = -0.25F;
		this.leftWing.roll = -(MathHelper.sin(f) + 0.125F) * 0.8F;
		this.leftWingTip.roll = (MathHelper.sin(f + 2.0F) + 0.5F) * 0.75F;
		this.rightWing.pitch = this.leftWing.pitch;
		this.rightWing.yaw = -this.leftWing.yaw;
		this.rightWing.roll = -this.leftWing.roll;
		this.rightWingTip.roll = -this.leftWingTip.roll;
		this.setLegAngles(
				g,
				this.leftFrontLeg,
				this.leftFrontLegTip,
				this.leftFrontFoot,
				this.leftHindLeg,
				this.leftHindLegTip,
				this.leftHindFoot
		);
		this.setLegAngles(
				g,
				this.rightFrontLeg,
				this.rightFrontLegTip,
				this.rightFrontFoot,
				this.rightHindLeg,
				this.rightHindLegTip,
				this.rightHindFoot
		);
		float p = 0.0F;
		i = this.tailParts[0].originY;
		j = this.tailParts[0].originZ;
		h = this.tailParts[0].originX;
		frame = enderDragonEntityRenderState.getLerpedFrame(11);

		for (int q = 0; q < NUM_TAIL_PARTS; q++) {
			EnderDragonFrameTracker.Frame frame4 = enderDragonEntityRenderState.getLerpedFrame(NUM_TAIL_PARTS + q);
			p += MathHelper.sin(q * 0.45F + f) * 0.05F;
			ModelPart modelPart2 = this.tailParts[q];
			modelPart2.yaw =
					(MathHelper.wrapDegrees(frame4.yRot() - frame.yRot()) * 1.5F + 180.0F) * (float) (Math.PI / 180.0);
			modelPart2.pitch = p + (float) (frame4.y() - frame.y()) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
			modelPart2.roll = MathHelper.wrapDegrees(frame4.yRot() - m) * (float) (Math.PI / 180.0) * 1.5F;
			modelPart2.originY = i;
			modelPart2.originZ = j;
			modelPart2.originX = h;
			i += MathHelper.sin(modelPart2.pitch) * 10.0F;
			j -= MathHelper.cos(modelPart2.yaw) * MathHelper.cos(modelPart2.pitch) * 10.0F;
			h -= MathHelper.sin(modelPart2.yaw) * MathHelper.cos(modelPart2.pitch) * 10.0F;
		}
	}

	private void setLegAngles(
			float offset,
			ModelPart frontLeg,
			ModelPart frontLegTip,
			ModelPart frontFoot,
			ModelPart hindLeg,
			ModelPart hindLegTip,
			ModelPart hindFoot
	) {
		hindLeg.pitch = 1.0F + offset * 0.1F;
		hindLegTip.pitch = 0.5F + offset * 0.1F;
		hindFoot.pitch = 0.75F + offset * 0.1F;
		frontLeg.pitch = 1.3F + offset * 0.1F;
		frontLegTip.pitch = -0.5F - offset * 0.1F;
		frontFoot.pitch = 0.75F + offset * 0.1F;
	}
}
