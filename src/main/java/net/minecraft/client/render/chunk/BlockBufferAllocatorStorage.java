package net.minecraft.client.render.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockBufferAllocatorStorage}.
 */
public class BlockBufferAllocatorStorage implements AutoCloseable {

	public static final int
			EXPECTED_TOTAL_SIZE =
			Arrays.stream(BlockRenderLayer.values()).mapToInt(BlockRenderLayer::getBufferSize).sum();
	private final Map<BlockRenderLayer, BufferAllocator> allocators = Util.mapEnum(
			BlockRenderLayer.class, blockRenderLayer -> new BufferAllocator(blockRenderLayer.getBufferSize())
	);

	public BufferAllocator get(BlockRenderLayer layer) {
		return this.allocators.get(layer);
	}

	public void clear() {
		this.allocators.values().forEach(BufferAllocator::clear);
	}

	public void reset() {
		this.allocators.values().forEach(BufferAllocator::reset);
	}

	@Override
	public void close() {
		this.allocators.values().forEach(BufferAllocator::close);
	}
}
