package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Unit;

@Environment(EnvType.CLIENT)
/**
 * {@code ShieldEntityModel}.
 */
public class ShieldEntityModel extends Model<Unit> {

	private static final String PLATE = "plate";
	private static final String HANDLE = "handle";
	private static final int HANDLE_WIDTH = 10;
	private static final int HANDLE_HEIGHT = 20;
	private final ModelPart plate;
	private final ModelPart handle;

	public ShieldEntityModel(ModelPart root) {
		super(root, RenderLayers::entitySolid);
		this.plate = root.getChild("plate");
		this.handle = root.getChild("handle");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"plate",
				ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"handle",
				ModelPartBuilder.create().uv(26, 0).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public ModelPart getPlate() {
		return this.plate;
	}

	public ModelPart getHandle() {
		return this.handle;
	}
}
