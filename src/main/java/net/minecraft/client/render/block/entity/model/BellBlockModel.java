package net.minecraft.client.render.block.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BellBlockModel}.
 */
public class BellBlockModel extends Model<BellBlockModel.BellModelState> {

	private static final String BELL_BODY = "bell_body";
	private final ModelPart bellBody;

	public BellBlockModel(ModelPart root) {
		super(root, RenderLayers::entitySolid);
		this.bellBody = root.getChild("bell_body");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"bell_body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F),
				ModelTransform.origin(8.0F, 12.0F, 8.0F)
		);
		modelPartData2.addChild(
				"bell_base",
				ModelPartBuilder.create().uv(0, 13).cuboid(4.0F, 4.0F, 4.0F, 8.0F, 2.0F, 8.0F),
				ModelTransform.origin(-8.0F, -12.0F, -8.0F)
		);
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(BellBlockModel.BellModelState bellModelState) {
		super.setAngles(bellModelState);
		float f = 0.0F;
		float g = 0.0F;
		if (bellModelState.shakeDirection != null) {
			float h = MathHelper.sin(bellModelState.ticks / (float) Math.PI) / (4.0F + bellModelState.ticks / 3.0F);
			switch (bellModelState.shakeDirection) {
				case NORTH:
					f = -h;
					break;
				case SOUTH:
					f = h;
					break;
				case EAST:
					g = -h;
					break;
				case WEST:
					g = h;
			}
		}

		this.bellBody.pitch = f;
		this.bellBody.roll = g;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BellModelState}.
	 */
	public record BellModelState(float ticks, @Nullable Direction shakeDirection) {
	}
}
