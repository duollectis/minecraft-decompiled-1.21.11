package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.NautilusEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombieNautilusCoralEntityModel}.
 */
public class ZombieNautilusCoralEntityModel extends NautilusEntityModel {

	private final ModelPart corals;

	public ZombieNautilusCoralEntityModel(ModelPart modelPart) {
		super(modelPart);
		ModelPart modelPart2 = this.nautilusRoot.getChild("shell");
		this.corals = modelPart2.getChild("corals");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		ModelPartData modelPartData = modelData.getRoot()
		                                       .getChild("root")
		                                       .getChild("shell")
		                                       .addChild(
				                                       "corals",
				                                       ModelPartBuilder.create(),
				                                       ModelTransform.origin(8.0F, 4.5F, -8.0F)
		                                       );
		ModelPartData
				modelPartData2 =
				modelPartData.addChild(
						"yellow_coral",
						ModelPartBuilder.create(),
						ModelTransform.origin(0.0F, -11.0F, 11.0F)
				);
		modelPartData2.addChild(
				"yellow_coral_second",
				ModelPartBuilder.create().uv(0, 85).cuboid(-4.5F, -3.5F, 0.0F, 6.0F, 8.0F, 0.0F),
				ModelTransform.of(0.0F, 0.0F, 2.0F, 0.0F, -0.7854F, 0.0F)
		);
		modelPartData2.addChild(
				"yellow_coral_first",
				ModelPartBuilder.create().uv(0, 85).cuboid(-4.5F, -3.5F, 0.0F, 6.0F, 8.0F, 0.0F),
				ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
		);
		ModelPartData modelPartData3 = modelPartData.addChild(
				"pink_coral",
				ModelPartBuilder.create().uv(-8, 94).cuboid(-4.5F, 4.5F, 0.0F, 6.0F, 0.0F, 8.0F),
				ModelTransform.origin(-12.5F, -18.0F, 11.0F)
		);
		modelPartData3.addChild(
				"pink_coral_second",
				ModelPartBuilder.create().uv(-8, 94).cuboid(-3.0F, 0.0F, -4.0F, 6.0F, 0.0F, 8.0F),
				ModelTransform.of(-1.5F, 4.5F, 4.0F, 0.0F, 0.0F, 1.5708F)
		);
		ModelPartData
				modelPartData4 =
				modelPartData.addChild(
						"blue_coral",
						ModelPartBuilder.create(),
						ModelTransform.origin(-14.0F, 0.0F, 5.5F)
				);
		modelPartData4.addChild(
				"blue_second",
				ModelPartBuilder.create().uv(0, 102).cuboid(-3.5F, -5.5F, 0.0F, 5.0F, 10.0F, 0.0F),
				ModelTransform.of(0.0F, 0.0F, -2.0F, 0.0F, 0.7854F, 0.0F)
		);
		modelPartData4.addChild(
				"blue_first",
				ModelPartBuilder.create().uv(0, 102).cuboid(-3.5F, -5.5F, 0.0F, 5.0F, 10.0F, 0.0F),
				ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData.addChild("red_coral", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 0.0F, 0.0F));
		modelPartData5.addChild(
				"red_coral_second",
				ModelPartBuilder.create().uv(0, 112).cuboid(-2.5F, -5.5F, 0.0F, 4.0F, 10.0F, 0.0F),
				ModelTransform.of(-0.5F, -1.0F, 1.5F, 0.0F, -0.829F, 0.0F)
		);
		modelPartData5.addChild(
				"red_coral_first",
				ModelPartBuilder.create().uv(0, 112).cuboid(-4.5F, -5.5F, 0.0F, 6.0F, 10.0F, 0.0F),
				ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
		);
		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(NautilusEntityRenderState nautilusEntityRenderState) {
		super.setAngles(nautilusEntityRenderState);
		this.corals.visible = nautilusEntityRenderState.armorStack.isEmpty();
	}
}
