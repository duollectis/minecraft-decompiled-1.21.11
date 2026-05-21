package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
/**
 * {@code BlazeEntityModel}.
 */
public class BlazeEntityModel extends EntityModel<LivingEntityRenderState> {

	private final ModelPart[] rods;
	private final ModelPart head;

	public BlazeEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.rods = new ModelPart[12];
		Arrays.setAll(this.rods, i -> modelPart.getChild(getRodName(i)));
	}

	private static String getRodName(int index) {
		return "part" + index;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				ModelTransform.NONE
		);
		float f = 0.0F;
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(0, 16).cuboid(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

		for (int i = 0; i < 4; i++) {
			float g = MathHelper.cos(f) * 9.0F;
			float h = -2.0F + MathHelper.cos(i * 2 * 0.25F);
			float j = MathHelper.sin(f) * 9.0F;
			modelPartData.addChild(getRodName(i), modelPartBuilder, ModelTransform.origin(g, h, j));
			f++;
		}

		f = (float) (Math.PI / 4);

		for (int i = 4; i < 8; i++) {
			float g = MathHelper.cos(f) * 7.0F;
			float h = 2.0F + MathHelper.cos(i * 2 * 0.25F);
			float j = MathHelper.sin(f) * 7.0F;
			modelPartData.addChild(getRodName(i), modelPartBuilder, ModelTransform.origin(g, h, j));
			f++;
		}

		f = 0.47123894F;

		for (int i = 8; i < 12; i++) {
			float g = MathHelper.cos(f) * 5.0F;
			float h = 11.0F + MathHelper.cos(i * 1.5F * 0.5F);
			float j = MathHelper.sin(f) * 5.0F;
			modelPartData.addChild(getRodName(i), modelPartBuilder, ModelTransform.origin(g, h, j));
			f++;
		}

		return TexturedModelData.of(modelData, 64, 32);
	}

	public void setAngles(LivingEntityRenderState livingEntityRenderState) {
		super.setAngles(livingEntityRenderState);
		float f = livingEntityRenderState.age * (float) Math.PI * -0.1F;

		for (int i = 0; i < 4; i++) {
			this.rods[i].originY = -2.0F + MathHelper.cos((i * 2 + livingEntityRenderState.age) * 0.25F);
			this.rods[i].originX = MathHelper.cos(f) * 9.0F;
			this.rods[i].originZ = MathHelper.sin(f) * 9.0F;
			f++;
		}

		f = (float) (Math.PI / 4) + livingEntityRenderState.age * (float) Math.PI * 0.03F;

		for (int i = 4; i < 8; i++) {
			this.rods[i].originY = 2.0F + MathHelper.cos((i * 2 + livingEntityRenderState.age) * 0.25F);
			this.rods[i].originX = MathHelper.cos(f) * 7.0F;
			this.rods[i].originZ = MathHelper.sin(f) * 7.0F;
			f++;
		}

		f = 0.47123894F + livingEntityRenderState.age * (float) Math.PI * -0.05F;

		for (int i = 8; i < 12; i++) {
			this.rods[i].originY = 11.0F + MathHelper.cos((i * 1.5F + livingEntityRenderState.age) * 0.5F);
			this.rods[i].originX = MathHelper.cos(f) * 5.0F;
			this.rods[i].originZ = MathHelper.sin(f) * 5.0F;
			f++;
		}

		this.head.yaw = livingEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.head.pitch = livingEntityRenderState.pitch * (float) (Math.PI / 180.0);
	}
}
