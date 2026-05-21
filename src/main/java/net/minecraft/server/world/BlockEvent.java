package net.minecraft.server.world;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

/**
 * {@code BlockEvent}.
 */
public record BlockEvent(BlockPos pos, Block block, int type, int data) {
}
