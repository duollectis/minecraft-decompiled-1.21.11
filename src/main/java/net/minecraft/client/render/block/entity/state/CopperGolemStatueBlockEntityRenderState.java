package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
/**
 * {@code CopperGolemStatueBlockEntityRenderState}.
 */
public class CopperGolemStatueBlockEntityRenderState extends BlockEntityRenderState {

	public CopperGolemStatueBlock.Pose pose = CopperGolemStatueBlock.Pose.STANDING;
	public Direction facing = Direction.NORTH;
}
