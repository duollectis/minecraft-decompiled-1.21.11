package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
/**
 * {@code BlockEntityTicker}.
 */
public interface BlockEntityTicker<T extends BlockEntity> {

	void tick(World world, BlockPos pos, BlockState state, T blockEntity);
}
