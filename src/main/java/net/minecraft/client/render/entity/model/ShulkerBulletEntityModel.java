package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.ShulkerBulletEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code ShulkerBulletEntityModel}.
 */
public class ShulkerBulletEntityModel extends EntityModel<ShulkerBulletEntityRenderState> {

	private static final String MAIN = "main";
	private final ModelPart bullet;

	public ShulkerBulletEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.bullet = modelPart.getChild("main");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"main",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -4.0F, -1.0F, 8.0F, 8.0F, 2.0F)
				                .uv(0, 10)
				                .cuboid(-1.0F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
				                .uv(20, 0)
				                .cuboid(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 32);
	}

	public void setAngles(ShulkerBulletEntityRenderState shulkerBulletEntityRenderState) {
		super.setAngles(shulkerBulletEntityRenderState);
		this.bullet.yaw = shulkerBulletEntityRenderState.yaw * (float) (Math.PI / 180.0);
		this.bullet.pitch = shulkerBulletEntityRenderState.pitch * (float) (Math.PI / 180.0);
	}
}
