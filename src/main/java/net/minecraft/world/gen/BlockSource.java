package net.minecraft.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.jspecify.annotations.Nullable;

/**
 * Источник блоков для генерации чанков.
 * Используется при заполнении шумового объёма блоками на основе позиции.
 */
public interface BlockSource {

	@Nullable BlockState apply(ChunkNoiseSampler sampler, int x, int y, int z);
}
