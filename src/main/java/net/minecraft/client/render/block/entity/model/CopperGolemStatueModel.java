package net.minecraft.client.render.block.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
/**
 * {@code CopperGolemStatueModel}.
 */
public class CopperGolemStatueModel extends Model<Direction> {

	public CopperGolemStatueModel(ModelPart root) {
		super(root, RenderLayers::entityCutoutNoCull);
	}

	public void setAngles(Direction direction) {
		this.root.originY = 0.0F;
		this.root.yaw = direction.getOpposite().getPositiveHorizontalDegrees() * (float) (Math.PI / 180.0);
		this.root.roll = (float) Math.PI;
	}
}
