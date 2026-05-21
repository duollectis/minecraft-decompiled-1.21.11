package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.RavagerEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code RavagerEntityModel}.
 */
public class RavagerEntityModel extends EntityModel<RavagerEntityRenderState> {

	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart neck;

	public RavagerEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.neck = modelPart.getChild("neck");
		this.head = this.neck.getChild("head");
		this.jaw = this.head.getChild("mouth");
		this.rightHindLeg = modelPart.getChild("right_hind_leg");
		this.leftHindLeg = modelPart.getChild("left_hind_leg");
		this.rightFrontLeg = modelPart.getChild("right_front_leg");
		this.leftFrontLeg = modelPart.getChild("left_front_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		int i = 16;
		ModelPartData modelPartData2 = modelPartData.addChild(
				"neck",
				ModelPartBuilder.create().uv(68, 73).cuboid(-5.0F, -1.0F, -18.0F, 10.0F, 10.0F, 18.0F),
				ModelTransform.origin(0.0F, -7.0F, 5.5F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"head",
				ModelPartBuilder
						.create()
						.uv(0, 0)
						.cuboid(-8.0F, -20.0F, -14.0F, 16.0F, 20.0F, 16.0F)
						.uv(0, 0)
						.cuboid(-2.0F, -6.0F, -18.0F, 4.0F, 8.0F, 4.0F),
				ModelTransform.origin(0.0F, 16.0F, -17.0F)
		);
		modelPartData3.addChild(
				"right_horn",
				ModelPartBuilder.create().uv(74, 55).cuboid(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
				ModelTransform.of(-10.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"left_horn",
				ModelPartBuilder.create().uv(74, 55).mirrored().cuboid(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
				ModelTransform.of(8.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"mouth",
				ModelPartBuilder.create().uv(0, 36).cuboid(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F),
				ModelTransform.origin(0.0F, -2.0F, 2.0F)
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder
						.create()
						.uv(0, 55)
						.cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 16.0F, 20.0F)
						.uv(0, 91)
						.cuboid(-6.0F, 6.0F, -7.0F, 12.0F, 13.0F, 18.0F),
				ModelTransform.of(0.0F, 1.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_hind_leg",
				ModelPartBuilder.create().uv(96, 0).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F),
				ModelTransform.origin(-8.0F, -13.0F, 18.0F)
		);
		modelPartData.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().uv(96, 0).mirrored().cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F),
				ModelTransform.origin(8.0F, -13.0F, 18.0F)
		);
		modelPartData.addChild(
				"right_front_leg",
				ModelPartBuilder.create().uv(64, 0).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F),
				ModelTransform.origin(-8.0F, -13.0F, -5.0F)
		);
		modelPartData.addChild(
				"left_front_leg",
				ModelPartBuilder.create().uv(64, 0).mirrored().cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F),
				ModelTransform.origin(8.0F, -13.0F, -5.0F)
		);
		return TexturedModelData.of(modelData, 128, 128);
	}

	public void setAngles(RavagerEntityRenderState ravagerEntityRenderState) {
		super.setAngles(ravagerEntityRenderState);
		float f = ravagerEntityRenderState.stunTick;
		float g = ravagerEntityRenderState.attackTick;
		int i = 10;
		if (g > 0.0F) {
			float h = MathHelper.wrap(g, 10.0F);
			float j = (1.0F + h) * 0.5F;
			float k = j * j * j * 12.0F;
			float l = k * MathHelper.sin(this.neck.pitch);
			this.neck.originZ = -6.5F + k;
			this.neck.originY = -7.0F - l;
			if (g > 5.0F) {
				this.jaw.pitch = MathHelper.sin((-4.0F + g) / 4.0F) * (float) Math.PI * 0.4F;
			}
			else {
				this.jaw.pitch = (float) (Math.PI / 20) * MathHelper.sin((float) Math.PI * g / 10.0F);
			}
		}
		else {
			float h = -1.0F;
			float j = -1.0F * MathHelper.sin(this.neck.pitch);
			this.neck.originX = 0.0F;
			this.neck.originY = -7.0F - j;
			this.neck.originZ = 5.5F;
			boolean bl = f > 0.0F;
			this.neck.pitch = bl ? 0.21991149F : 0.0F;
			this.jaw.pitch = (float) Math.PI * (bl ? 0.05F : 0.01F);
			if (bl) {
				double d = f / 40.0;
				this.neck.originX = (float) Math.sin(d * 10.0) * 3.0F;
			}
			else if (ravagerEntityRenderState.roarTick > 0.0) {
				float l = MathHelper.sin(ravagerEntityRenderState.roarTick * (float) Math.PI * 0.25F);
				this.jaw.pitch = (float) (Math.PI / 2) * l;
			}
		}

		this.head.pitch = ravagerEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = ravagerEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		float hx = ravagerEntityRenderState.limbSwingAnimationProgress;
		float jx = 0.4F * ravagerEntityRenderState.limbSwingAmplitude;
		this.rightHindLeg.pitch = MathHelper.cos(hx * 0.6662F) * jx;
		this.leftHindLeg.pitch = MathHelper.cos(hx * 0.6662F + (float) Math.PI) * jx;
		this.rightFrontLeg.pitch = MathHelper.cos(hx * 0.6662F + (float) Math.PI) * jx;
		this.leftFrontLeg.pitch = MathHelper.cos(hx * 0.6662F) * jx;
	}
}
