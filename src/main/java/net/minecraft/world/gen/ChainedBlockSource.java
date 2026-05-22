package net.minecraft.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.jspecify.annotations.Nullable;

/**
 * Цепочка сэмплеров состояний блоков, опрашиваемых последовательно.
 * Возвращает первый ненулевой результат из цепочки, либо {@code null} если ни один не сработал.
 */
public record ChainedBlockSource(ChunkNoiseSampler.BlockStateSampler[] samplers)
		implements ChunkNoiseSampler.BlockStateSampler {

	@Override
	public @Nullable BlockState sample(DensityFunction.NoisePos pos) {
		for (ChunkNoiseSampler.BlockStateSampler sampler : samplers) {
			BlockState state = sampler.sample(pos);

			if (state != null) {
				return state;
			}
		}

		return null;
	}
}
