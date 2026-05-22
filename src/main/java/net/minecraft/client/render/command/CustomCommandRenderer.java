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

/**
 * Рендерит пользовательские команды рисования ({@link OrderedRenderCommandQueue.Custom}),
 * сгруппированные по слоям рендеринга. Используется для произвольной геометрии,
 * которую нельзя выразить стандартными командами модели или метки.
 */
@Environment(EnvType.CLIENT)
public class CustomCommandRenderer {

	public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers) {
		CustomCommandRenderer.Commands commands = queue.getCustomCommands();

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> entry : commands.customCommands.entrySet()) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(entry.getKey());

			for (OrderedRenderCommandQueueImpl.CustomCommand customCommand : entry.getValue()) {
				customCommand.customRenderer().render(customCommand.matricesEntry(), vertexConsumer);
			}
		}
	}

	/** Накопитель пользовательских команд рисования, сгруппированных по слоям рендеринга. */
	@Environment(EnvType.CLIENT)
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> customCommands = new HashMap<>();
		private final Set<RenderLayer> customRenderLayers = new ObjectOpenHashSet();

		public void add(MatrixStack matrices, RenderLayer renderLayer, OrderedRenderCommandQueue.Custom custom) {
			customCommands
					.computeIfAbsent(renderLayer, layer -> new ArrayList<>())
					.add(new OrderedRenderCommandQueueImpl.CustomCommand(matrices.peek().copy(), custom));
		}

		public void clear() {
			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.CustomCommand>> entry : customCommands.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					customRenderLayers.add(entry.getKey());
					entry.getValue().clear();
				}
			}
		}

		public void nextFrame() {
			customCommands.keySet().removeIf(layer -> !customRenderLayers.contains(layer));
			customRenderLayers.clear();
		}
	}
}
