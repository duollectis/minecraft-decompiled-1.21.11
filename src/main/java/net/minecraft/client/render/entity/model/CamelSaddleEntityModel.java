package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.CamelEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code CamelSaddleEntityModel}.
 */
public class CamelSaddleEntityModel extends CamelEntityModel {

	private static final String SADDLE = "saddle";
	private static final String BRIDLE = "bridle";
	private static final String REINS = "reins";
	private final ModelPart reins = this.head.getChild("reins");

	public CamelSaddleEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.getChild("body");
		ModelPartData modelPartData3 = modelPartData2.getChild("head");
		Dilation dilation = new Dilation(0.05F);
		modelPartData2.addChild(
				"saddle",
				ModelPartBuilder.create()
				                .uv(74, 64)
				                .cuboid(-4.5F, -17.0F, -15.5F, 9.0F, 5.0F, 11.0F, dilation)
				                .uv(92, 114)
				                .cuboid(-3.5F, -20.0F, -15.5F, 7.0F, 3.0F, 11.0F, dilation)
				                .uv(0, 89)
				                .cuboid(-7.5F, -12.0F, -23.5F, 15.0F, 12.0F, 27.0F, dilation),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"reins",
				ModelPartBuilder.create()
				                .uv(98, 42)
				                .cuboid(3.51F, -18.0F, -17.0F, 0.0F, 7.0F, 15.0F)
				                .uv(84, 57)
				                .cuboid(-3.5F, -18.0F, -2.0F, 7.0F, 7.0F, 0.0F)
				                .uv(98, 42)
				                .cuboid(-3.51F, -18.0F, -17.0F, 0.0F, 7.0F, 15.0F),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"bridle",
				ModelPartBuilder.create()
				                .uv(60, 87)
				                .cuboid(-3.5F, -7.0F, -15.0F, 7.0F, 8.0F, 19.0F, dilation)
				                .uv(21, 64)
				                .cuboid(-3.5F, -21.0F, -15.0F, 7.0F, 14.0F, 7.0F, dilation)
				                .uv(50, 64)
				                .cuboid(-2.5F, -21.0F, -21.0F, 5.0F, 5.0F, 6.0F, dilation)
				                .uv(74, 70)
				                .cuboid(2.5F, -19.0F, -18.0F, 1.0F, 2.0F, 2.0F)
				                .uv(74, 70)
				                .mirrored()
				                .cuboid(-3.5F, -19.0F, -18.0F, 1.0F, 2.0F, 2.0F),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(CamelEntityRenderState camelEntityRenderState) {
		super.setAngles(camelEntityRenderState);
		this.reins.visible = camelEntityRenderState.hasPassengers;
	}
}
