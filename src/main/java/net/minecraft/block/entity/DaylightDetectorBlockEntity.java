package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * {@code DaylightDetectorBlockEntity}.
 */
public class DaylightDetectorBlockEntity extends BlockEntity {

	public DaylightDetectorBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.DAYLIGHT_DETECTOR, pos, state);
	}
}
