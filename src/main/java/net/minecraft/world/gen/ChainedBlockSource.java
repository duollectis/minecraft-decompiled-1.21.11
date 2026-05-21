package net.minecraft.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.jspecify.annotations.Nullable;

/**
 * {@code ChainedBlockSource}.
 */
public record ChainedBlockSource(ChunkNoiseSampler.BlockStateSampler[] samplers) implements ChunkNoiseSampler.BlockStateSampler {

	@Override
	public @Nullable BlockState sample(DensityFunction.NoisePos pos) {
		for (ChunkNoiseSampler.BlockStateSampler blockStateSampler : this.samplers) {
			BlockState blockState = blockStateSampler.sample(pos);
			if (blockState != null) {
				return blockState;
			}
		}

		return null;
	}
}
