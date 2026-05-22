package net.minecraft.world.chunk.light;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.function.BiConsumer;

/**
 * Расширение {@link BlockView} для чанков, которые являются источниками света.
 * Предоставляет доступ к итерации по всем источникам света и данным о небесном свете.
 */
public interface LightSourceView extends BlockView {

	void forEachLightSource(BiConsumer<BlockPos, BlockState> callback);

	ChunkSkyLight getChunkSkyLight();
}
