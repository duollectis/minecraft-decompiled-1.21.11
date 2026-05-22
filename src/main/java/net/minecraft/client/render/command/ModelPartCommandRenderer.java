package net.minecraft.client.render.command;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;

import java.util.*;
import java.util.Map.Entry;

/**
 * Рендерит команды рисования отдельных частей моделей ({@link OrderedRenderCommandQueueImpl.ModelPartCommand}).
 * Поддерживает блеск (glint), контурную подсветку и оверлей разрушения блока.
 */
@Environment(EnvType.CLIENT)
public class ModelPartCommandRenderer {

	private final MatrixStack matrices = new MatrixStack();

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumerProvider,
			VertexConsumerProvider.Immediate crumblingVertexConsumers
	) {
		ModelPartCommandRenderer.Commands commands = queue.getModelPartCommands();

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>> entry : commands.modelPartCommands.entrySet()) {
			RenderLayer renderLayer = entry.getKey();
			VertexConsumer layerConsumer = vertexConsumers.getBuffer(renderLayer);

			for (OrderedRenderCommandQueueImpl.ModelPartCommand cmd : entry.getValue()) {
				VertexConsumer mainConsumer = resolveMainConsumer(cmd, vertexConsumers, renderLayer, layerConsumer);
				matrices.peek().copy(cmd.matricesEntry());
				cmd.modelPart().render(matrices, mainConsumer, cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor());

				if (cmd.outlineColor() != 0 && (renderLayer.getAffectedOutline().isPresent() || renderLayer.isOutline())) {
					outlineVertexConsumerProvider.setColor(cmd.outlineColor());
					VertexConsumer outlineConsumer = outlineVertexConsumerProvider.getBuffer(renderLayer);
					cmd.modelPart().render(
							matrices,
							cmd.sprite() == null ? outlineConsumer : cmd.sprite().getTextureSpecificVertexConsumer(outlineConsumer),
							cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor()
					);
				}

				if (cmd.crumblingOverlay() != null) {
					VertexConsumer crumblingConsumer = new OverlayVertexConsumer(
							crumblingVertexConsumers.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(cmd.crumblingOverlay().progress())),
							cmd.crumblingOverlay().cameraMatricesEntry(),
							1.0F
					);
					cmd.modelPart().render(matrices, crumblingConsumer, cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor());
				}
			}
		}
	}

	private static VertexConsumer resolveMainConsumer(
			OrderedRenderCommandQueueImpl.ModelPartCommand cmd,
			VertexConsumerProvider.Immediate vertexConsumers,
			RenderLayer renderLayer,
			VertexConsumer layerConsumer
	) {
		if (cmd.sprite() != null) {
			return cmd.hasGlint()
					? cmd.sprite().getTextureSpecificVertexConsumer(
							ItemRenderer.getItemGlintConsumer(vertexConsumers, renderLayer, cmd.sheeted(), true))
					: cmd.sprite().getTextureSpecificVertexConsumer(layerConsumer);
		}

		return cmd.hasGlint()
				? ItemRenderer.getItemGlintConsumer(vertexConsumers, renderLayer, cmd.sheeted(), true)
				: layerConsumer;
	}

	/** Накопитель команд рисования частей моделей, сгруппированных по слоям рендеринга. */
	@Environment(EnvType.CLIENT)
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>> modelPartCommands = new HashMap<>();
		private final Set<RenderLayer> modelPartLayers = new ObjectOpenHashSet();

		public void add(RenderLayer renderLayer, OrderedRenderCommandQueueImpl.ModelPartCommand command) {
			modelPartCommands.computeIfAbsent(renderLayer, layer -> new ArrayList<>()).add(command);
		}

		public void clear() {
			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>> entry : modelPartCommands.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					modelPartLayers.add(entry.getKey());
					entry.getValue().clear();
				}
			}
		}

		public void nextFrame() {
			modelPartCommands.keySet().removeIf(layer -> !modelPartLayers.contains(layer));
			modelPartLayers.clear();
		}
	}
}
