package net.minecraft.client.render.command;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

import java.util.*;
import java.util.Map.Entry;

/**
 * Рендерит команды рисования моделей сущностей ({@link OrderedRenderCommandQueueImpl.ModelCommand}).
 * Непрозрачные модели рендерятся сгруппированно по слою, полупрозрачные — сортируются
 * по убыванию расстояния от камеры для корректного альфа-блендинга.
 */
@Environment(EnvType.CLIENT)
public class ModelCommandRenderer {

	private final MatrixStack matrices = new MatrixStack();

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		ModelCommandRenderer.Commands commands = queue.getModelCommands();
		renderAll(vertexConsumers, outlineVertexConsumers, commands.opaqueModelCommands, crumblingOverlayVertexConsumers);
		commands.blendedModelCommands.sort(Comparator.comparingDouble(cmd -> -cmd.position().lengthSquared()));
		renderAllBlended(vertexConsumers, outlineVertexConsumers, commands.blendedModelCommands, crumblingOverlayVertexConsumers);
	}

	private void renderAllBlended(
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			List<OrderedRenderCommandQueueImpl.BlendedModelCommand<?>> blendedModelCommands,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		for (OrderedRenderCommandQueueImpl.BlendedModelCommand<?> cmd : blendedModelCommands) {
			render(cmd.model(), cmd.renderType(), vertexConsumers.getBuffer(cmd.renderType()), outlineVertexConsumers, crumblingOverlayVertexConsumers);
		}
	}

	private void renderAll(
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> modelCommands,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		Iterable<Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>>> entries;

		if (SharedConstants.SHUFFLE_MODELS) {
			List<Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>>> shuffled = new ArrayList<>(modelCommands.entrySet());
			Collections.shuffle(shuffled);
			entries = shuffled;
		}
		else {
			entries = modelCommands.entrySet();
		}

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> entry : entries) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(entry.getKey());

			for (OrderedRenderCommandQueueImpl.ModelCommand<?> cmd : entry.getValue()) {
				render(cmd, entry.getKey(), vertexConsumer, outlineVertexConsumers, crumblingOverlayVertexConsumers);
			}
		}
	}

	private <S> void render(
			OrderedRenderCommandQueueImpl.ModelCommand<S> cmd,
			RenderLayer renderLayer,
			VertexConsumer vertexConsumer,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		matrices.push();
		matrices.peek().copy(cmd.matricesEntry());
		Model<? super S> model = cmd.model();
		VertexConsumer mainConsumer = cmd.sprite() == null
				? vertexConsumer
				: cmd.sprite().getTextureSpecificVertexConsumer(vertexConsumer);
		model.setAngles(cmd.state());
		model.render(matrices, mainConsumer, cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor());

		if (cmd.outlineColor() != 0 && (renderLayer.getAffectedOutline().isPresent() || renderLayer.isOutline())) {
			outlineVertexConsumers.setColor(cmd.outlineColor());
			VertexConsumer outlineConsumer = outlineVertexConsumers.getBuffer(renderLayer);
			model.render(
					matrices,
					cmd.sprite() == null ? outlineConsumer : cmd.sprite().getTextureSpecificVertexConsumer(outlineConsumer),
					cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor()
			);
		}

		if (cmd.crumblingOverlay() != null && renderLayer.hasCrumbling()) {
			VertexConsumer crumblingConsumer = new OverlayVertexConsumer(
					crumblingOverlayVertexConsumers.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(cmd.crumblingOverlay().progress())),
					cmd.crumblingOverlay().cameraMatricesEntry(),
					1.0F
			);
			model.render(
					matrices,
					cmd.sprite() == null ? crumblingConsumer : cmd.sprite().getTextureSpecificVertexConsumer(crumblingConsumer),
					cmd.lightCoords(), cmd.overlayCoords(), cmd.tintedColor()
			);
		}

		matrices.pop();
	}

	/** Накопитель команд рисования моделей, разделённых на непрозрачные и полупрозрачные. */
	@Environment(EnvType.CLIENT)
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> opaqueModelCommands = new HashMap<>();
		final List<OrderedRenderCommandQueueImpl.BlendedModelCommand<?>> blendedModelCommands = new ArrayList<>();
		private final Set<RenderLayer> usedModelRenderLayers = new ObjectOpenHashSet();

		public void add(RenderLayer renderLayer, OrderedRenderCommandQueueImpl.ModelCommand<?> modelCommand) {
			if (renderLayer.getRenderPipeline().getBlendFunction().isEmpty()) {
				opaqueModelCommands
						.computeIfAbsent(renderLayer, layer -> new ArrayList<>())
						.add(modelCommand);
			}
			else {
				Vector3f position = modelCommand.matricesEntry().getPositionMatrix().transformPosition(new Vector3f());
				blendedModelCommands.add(new OrderedRenderCommandQueueImpl.BlendedModelCommand<>(modelCommand, renderLayer, position));
			}
		}

		public void clear() {
			blendedModelCommands.clear();

			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> entry : opaqueModelCommands.entrySet()) {
				List<OrderedRenderCommandQueueImpl.ModelCommand<?>> commands = entry.getValue();
				if (!commands.isEmpty()) {
					usedModelRenderLayers.add(entry.getKey());
					commands.clear();
				}
			}
		}

		public void nextFrame() {
			opaqueModelCommands.keySet().removeIf(layer -> !usedModelRenderLayers.contains(layer));
			usedModelRenderLayers.clear();
		}
	}

	/** Данные оверлея разрушения блока, применяемого поверх модели сущности. */
	@Environment(EnvType.CLIENT)
	public record CrumblingOverlayCommand(int progress, MatrixStack.Entry cameraMatricesEntry) {
	}
}
