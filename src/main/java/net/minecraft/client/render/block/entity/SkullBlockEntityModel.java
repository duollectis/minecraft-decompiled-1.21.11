package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayers;

@Environment(EnvType.CLIENT)
/**
 * {@code SkullBlockEntityModel}.
 */
public abstract class SkullBlockEntityModel extends Model<SkullBlockEntityModel.SkullModelState> {

	public SkullBlockEntityModel(ModelPart root) {
		super(root, RenderLayers::entityTranslucent);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SkullModelState}.
	 */
	public static class SkullModelState {

		public float poweredTicks;
		public float yaw;
		public float pitch;
	}
}
