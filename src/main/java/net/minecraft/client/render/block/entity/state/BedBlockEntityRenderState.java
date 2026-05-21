package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
/**
 * {@code BedBlockEntityRenderState}.
 */
public class BedBlockEntityRenderState extends BlockEntityRenderState {

	public DyeColor dyeColor = DyeColor.WHITE;
	public Direction facing = Direction.NORTH;
	public boolean headPart;
}
