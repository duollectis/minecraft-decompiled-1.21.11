package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.SheepEntityRenderState;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code SheepEntityModel}.
 */
public class SheepEntityModel extends QuadrupedEntityModel<SheepEntityRenderState> {

	public static final ModelTransformer
			BABY_TRANSFORMER =
			new BabyModelTransformer(false, 8.0F, 4.0F, 2.0F, 2.0F, 24.0F, Set.of("head"));

	public SheepEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = QuadrupedEntityModel.getModelData(12, false, true, Dilation.NONE);
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -4.0F, -6.0F, 6.0F, 6.0F, 8.0F),
				ModelTransform.origin(0.0F, 6.0F, -8.0F)
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(28, 8).cuboid(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F),
				ModelTransform.of(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 32);
	}

	public void setAngles(SheepEntityRenderState sheepEntityRenderState) {
		super.setAngles(sheepEntityRenderState);
		this.head.originY =
				this.head.originY + sheepEntityRenderState.neckAngle * 9.0F * sheepEntityRenderState.ageScale;
		this.head.pitch = sheepEntityRenderState.headAngle;
	}
}
