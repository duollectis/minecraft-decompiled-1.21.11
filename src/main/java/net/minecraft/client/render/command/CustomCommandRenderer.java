package net.minecraft.client.render.command;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.*;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code CustomCommandRenderer}.
 */
public class CustomCommandRenderer {

	/**
	 * Render.
	 *
	 * @param queue queue
	 * @param vertexConsumers vertex consumers
	 */
	public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers) {
		CustomCommandRenderer.Commands commands = queue.getCustomCommands();

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> entry : commands.customCommands.entrySet()) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(entry.getKey());

			for (OrderedRenderCommandQueueImpl.CustomCommand customCommand : entry.getValue()) {
				customCommand.customRenderer().render(customCommand.matricesEntry(), vertexConsumer);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Commands}.
	 */
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> customCommands = new HashMap<>();
		private final Set<RenderLayer> customRenderLayers = new ObjectOpenHashSet();

		/**
		 * Add.
		 *
		 * @param matrices matrices
		 * @param renderLayer render layer
		 * @param custom custom
		 */
		public void add(MatrixStack matrices, RenderLayer renderLayer, OrderedRenderCommandQueue.Custom custom) {
			List<OrderedRenderCommandQueueImpl.CustomCommand>
					list =
					this.customCommands.computeIfAbsent(renderLayer, renderLayerx -> new ArrayList<>());
			list.add(new OrderedRenderCommandQueueImpl.CustomCommand(matrices.peek().copy(), custom));
		}

		/**
		 * Clear.
		 */
		public void clear() {
			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> entry : this.customCommands.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					this.customRenderLayers.add(entry.getKey());
					entry.getValue().clear();
				}
			}
		}

		/**
		 * Next frame.
		 */
		public void nextFrame() {
			this.customCommands.keySet().removeIf(renderLayer -> !this.customRenderLayers.contains(renderLayer));
			this.customRenderLayers.clear();
		}
	}
}
