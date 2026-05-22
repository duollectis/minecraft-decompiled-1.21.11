package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.HappyGhastEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code HappyGhastHarnessEntityModel}.
 */
public class HappyGhastHarnessEntityModel extends EntityModel<HappyGhastEntityRenderState> {

	private static final float HARNESS_Y_OFFSET = 14.0F;
	private final ModelPart goggles;

	public HappyGhastHarnessEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.goggles = modelPart.getChild("goggles");
	}

	public static TexturedModelData getTexturedModelData(boolean baby) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"harness",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
				ModelTransform.origin(0.0F, 24.0F, 0.0F)
		);
		modelPartData.addChild(
				"goggles",
				ModelPartBuilder.create().uv(0, 32).cuboid(-8.0F, -2.5F, -2.5F, 16.0F, 5.0F, 5.0F, new Dilation(0.15F)),
				ModelTransform.origin(0.0F, HARNESS_Y_OFFSET, -5.5F)
		);
		return TexturedModelData.of(modelData, 64, 64)
		                        .transform(ModelTransformer.scaling(4.0F))
		                        .transform(baby ? HappyGhastEntityModel.BABY_TRANSFORMER : ModelTransformer.NO_OP);
	}

	public void setAngles(HappyGhastEntityRenderState happyGhastEntityRenderState) {
		super.setAngles(happyGhastEntityRenderState);
		if (happyGhastEntityRenderState.hasPassengers) {
			this.goggles.pitch = 0.0F;
			this.goggles.originY = HARNESS_Y_OFFSET;
		}
		else {
			this.goggles.pitch = -0.7854F;
			this.goggles.originY = 9.0F;
		}
	}
}
