package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

@Environment(EnvType.CLIENT)
/**
 * {@code TridentEntityModel}.
 */
public class TridentEntityModel extends Model<Unit> {

	public static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/trident.png");

	public TridentEntityModel(ModelPart root) {
		super(root, RenderLayers::entitySolid);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"pole",
				ModelPartBuilder.create().uv(0, 6).cuboid(-0.5F, 2.0F, -0.5F, 1.0F, 25.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"base",
				ModelPartBuilder.create().uv(4, 0).cuboid(-1.5F, 0.0F, -0.5F, 3.0F, 2.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"left_spike",
				ModelPartBuilder.create().uv(4, 3).cuboid(-2.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"middle_spike",
				ModelPartBuilder.create().uv(0, 0).cuboid(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"right_spike",
				ModelPartBuilder.create().uv(4, 3).mirrored().cuboid(1.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 32, 32);
	}
}
