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

/**
 * Поставщик вершинных потребителей ({@link VertexConsumer}) для различных слоёв рендеринга.
 * Абстрагирует управление буферами: каждый {@link RenderLayer} получает свой буфер,
 * который автоматически сбрасывается при переключении слоёв.
 */
@Environment(EnvType.CLIENT)
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

	/**
	 * Немедленная реализация: буферизует вершины по слоям и отправляет их на GPU
	 * при переключении слоя или явном вызове {@link #draw()}.
	 * Слои с выделенными буферами ({@link #layerBuffers}) могут накапливаться параллельно.
	 */
	@Environment(EnvType.CLIENT)
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
			BufferBuilder bufferBuilder = pending.get(layer);
			if (bufferBuilder != null && !layer.areVerticesNotShared()) {
				draw(layer, bufferBuilder);
				bufferBuilder = null;
			}

			if (bufferBuilder != null) {
				return bufferBuilder;
			}

			BufferAllocator dedicatedAllocator = layerBuffers.get(layer);
			if (dedicatedAllocator != null) {
				bufferBuilder = new BufferBuilder(dedicatedAllocator, layer.getDrawMode(), layer.getVertexFormat());
			}
			else {
				if (currentLayer != null) {
					draw(currentLayer);
				}

				bufferBuilder = new BufferBuilder(allocator, layer.getDrawMode(), layer.getVertexFormat());
				currentLayer = layer;
			}

			pending.put(layer, bufferBuilder);
			return bufferBuilder;
		}

		public void drawCurrentLayer() {
			if (currentLayer == null) {
				return;
			}

			draw(currentLayer);
			currentLayer = null;
		}

		public void draw() {
			drawCurrentLayer();

			for (RenderLayer renderLayer : layerBuffers.keySet()) {
				draw(renderLayer);
			}
		}

		public void draw(RenderLayer layer) {
			BufferBuilder bufferBuilder = pending.remove(layer);
			if (bufferBuilder != null) {
				draw(layer, bufferBuilder);
			}
		}

		private void draw(RenderLayer layer, BufferBuilder builder) {
			BuiltBuffer builtBuffer = builder.endNullable();
			if (builtBuffer != null) {
				if (layer.isTranslucent()) {
					BufferAllocator sortAllocator = layerBuffers.getOrDefault(layer, allocator);
					builtBuffer.sortQuads(sortAllocator, RenderSystem.getProjectionType().getVertexSorter());
				}

				layer.draw(builtBuffer);
			}

			if (layer.equals(currentLayer)) {
				currentLayer = null;
			}
		}
	}
}
