package net.minecraft.client.render.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractChunkRenderData}.
 */
public interface AbstractChunkRenderData extends AutoCloseable {

	default boolean hasPosition(NormalizedRelativePos pos) {
		return false;
	}

	default boolean hasData() {
		return false;
	}

	default boolean hasTranslucentLayers() {
		return false;
	}

	default boolean containsLayer(BlockRenderLayer layer) {
		return true;
	}

	default List<BlockEntity> getBlockEntities() {
		return Collections.emptyList();
	}

	boolean isVisibleThrough(Direction from, Direction to);

	default @Nullable Buffers getBuffersForLayer(BlockRenderLayer layer) {
		return null;
	}

	@Override
	default void close() {
	}
}
