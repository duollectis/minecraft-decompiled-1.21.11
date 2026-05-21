package net.minecraft.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
/**
 * {@code BreakingBlockRenderState}.
 */
public class BreakingBlockRenderState extends MovingBlockRenderState {

	public int breakProgress;

	public BreakingBlockRenderState(ClientWorld world, BlockPos entityBlockPos, int breakProgress) {
		this.world = world;
		this.entityBlockPos = entityBlockPos;
		this.blockState = world.getBlockState(entityBlockPos);
		this.breakProgress = breakProgress;
		this.biome = world.getBiome(entityBlockPos);
	}
}
