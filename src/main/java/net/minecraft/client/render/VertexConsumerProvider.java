package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.BufferAllocator;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;

@Environment(EnvType.CLIENT)
/**
 * {@code VertexConsumerProvider}.
 */
public interface VertexConsumerProvider {

	static VertexConsumerProvider.Immediate immediate(BufferAllocator buffer) {
		return immediate(Object2ObjectSortedMaps.emptyMap(), buffer);
	}

	static VertexConsumerProvider.Immediate immediate(
			SequencedMap<RenderLayer, BufferAllocator> layerBuffers,
			BufferAllocator fallbackBuffer
	) {
		return new VertexConsumerProvider.Immediate(fallbackBuffer, layerBuffers);
	}

	VertexConsumer getBuffer(RenderLayer layer);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Immediate}.
	 */
	public static class Immediate implements VertexConsumerProvider {

		protected final BufferAllocator allocator;
		protected final SequencedMap<RenderLayer, BufferAllocator> layerBuffers;
		protected final Map<RenderLayer, BufferBuilder> pending = new HashMap<>();
		protected @Nullable RenderLayer currentLayer;

		protected Immediate(BufferAllocator allocator, SequencedMap<RenderLayer, BufferAllocator> layerBuffers) {
			this.allocator = allocator;
			this.layerBuffers = layerBuffers;
		}

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			BufferBuilder bufferBuilder = this.pending.get(layer);
			if (bufferBuilder != null && !layer.areVerticesNotShared()) {
				this.draw(layer, bufferBuilder);
				bufferBuilder = null;
			}

			if (bufferBuilder != null) {
				return bufferBuilder;
			}
			else {
				BufferAllocator bufferAllocator = this.layerBuffers.get(layer);
				if (bufferAllocator != null) {
					bufferBuilder = new BufferBuilder(bufferAllocator, layer.getDrawMode(), layer.getVertexFormat());
				}
				else {
					if (this.currentLayer != null) {
						this.draw(this.currentLayer);
					}

					bufferBuilder = new BufferBuilder(this.allocator, layer.getDrawMode(), layer.getVertexFormat());
					this.currentLayer = layer;
				}

				this.pending.put(layer, bufferBuilder);
				return bufferBuilder;
			}
		}

		public void drawCurrentLayer() {
			if (this.currentLayer != null) {
				this.draw(this.currentLayer);
				this.currentLayer = null;
			}
		}

		public void draw() {
			this.drawCurrentLayer();

			for (RenderLayer renderLayer : this.layerBuffers.keySet()) {
				this.draw(renderLayer);
			}
		}

		public void draw(RenderLayer layer) {
			BufferBuilder bufferBuilder = this.pending.remove(layer);
			if (bufferBuilder != null) {
				this.draw(layer, bufferBuilder);
			}
		}

		private void draw(RenderLayer layer, BufferBuilder builder) {
			BuiltBuffer builtBuffer = builder.endNullable();
			if (builtBuffer != null) {
				if (layer.isTranslucent()) {
					BufferAllocator bufferAllocator = this.layerBuffers.getOrDefault(layer, this.allocator);
					builtBuffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
				}

				layer.draw(builtBuffer);
			}

			if (layer.equals(this.currentLayer)) {
				this.currentLayer = null;
			}
		}
	}
}
