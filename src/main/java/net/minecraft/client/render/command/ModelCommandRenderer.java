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

@Environment(EnvType.CLIENT)
/**
 * {@code ModelCommandRenderer}.
 */
public class ModelCommandRenderer {

	private final MatrixStack matrices = new MatrixStack();

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		ModelCommandRenderer.Commands commands = queue.getModelCommands();
		this.renderAll(
				vertexConsumers,
				outlineVertexConsumers,
				commands.opaqueModelCommands,
				crumblingOverlayVertexConsumers
		);
		commands.blendedModelCommands.sort(Comparator.comparingDouble(modelCommand -> -modelCommand
				.position()
				.lengthSquared()));
		this.renderAllBlended(
				vertexConsumers,
				outlineVertexConsumers,
				commands.blendedModelCommands,
				crumblingOverlayVertexConsumers
		);
	}

	private void renderAllBlended(
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			List<OrderedRenderCommandQueueImpl.BlendedModelCommand<?>> blendedModelCommands,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		for (OrderedRenderCommandQueueImpl.BlendedModelCommand<?> blendedModelCommand : blendedModelCommands) {
			this.render(
					blendedModelCommand.model(),
					blendedModelCommand.renderType(),
					vertexConsumers.getBuffer(blendedModelCommand.renderType()),
					outlineVertexConsumers,
					crumblingOverlayVertexConsumers
			);
		}
	}

	private void renderAll(
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> modelCommands,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		Iterable<Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>>> iterable;
		if (SharedConstants.SHUFFLE_MODELS) {
			List<Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>>>
					list =
					new ArrayList<>(modelCommands.entrySet());
			Collections.shuffle(list);
			iterable = list;
		}
		else {
			iterable = modelCommands.entrySet();
		}

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> entry : iterable) {
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(entry.getKey());

			for (OrderedRenderCommandQueueImpl.ModelCommand<?> modelCommand : entry.getValue()) {
				this.render(
						modelCommand,
						entry.getKey(),
						vertexConsumer,
						outlineVertexConsumers,
						crumblingOverlayVertexConsumers
				);
			}
		}
	}

	private <S> void render(
			OrderedRenderCommandQueueImpl.ModelCommand<S> model,
			RenderLayer renderLayer,
			VertexConsumer vertexConsumer,
			OutlineVertexConsumerProvider outlineVertexConsumers,
			VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers
	) {
		this.matrices.push();
		this.matrices.peek().copy(model.matricesEntry());
		Model<? super S> model2 = model.model();
		VertexConsumer
				vertexConsumer2 =
				model.sprite() == null ? vertexConsumer
				                       : model.sprite().getTextureSpecificVertexConsumer(vertexConsumer);
		model2.setAngles(model.state());
		model2.render(this.matrices, vertexConsumer2, model.lightCoords(), model.overlayCoords(), model.tintedColor());
		if (model.outlineColor() != 0 && (renderLayer.getAffectedOutline().isPresent() || renderLayer.isOutline())) {
			outlineVertexConsumers.setColor(model.outlineColor());
			VertexConsumer vertexConsumer3 = outlineVertexConsumers.getBuffer(renderLayer);
			model2.render(
					this.matrices,
					model.sprite() == null ? vertexConsumer3
					                       : model.sprite().getTextureSpecificVertexConsumer(vertexConsumer3),
					model.lightCoords(),
					model.overlayCoords(),
					model.tintedColor()
			);
		}

		if (model.crumblingOverlay() != null && renderLayer.hasCrumbling()) {
			VertexConsumer vertexConsumer3 = new OverlayVertexConsumer(
					crumblingOverlayVertexConsumers.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(model
							.crumblingOverlay()
							.progress())),
					model.crumblingOverlay().cameraMatricesEntry(),
					1.0F
			);
			model2.render(
					this.matrices,
					model.sprite() == null ? vertexConsumer3
					                       : model.sprite().getTextureSpecificVertexConsumer(vertexConsumer3),
					model.lightCoords(),
					model.overlayCoords(),
					model.tintedColor()
			);
		}

		this.matrices.pop();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Commands}.
	 */
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>>
				opaqueModelCommands =
				new HashMap<>();
		final List<OrderedRenderCommandQueueImpl.BlendedModelCommand<?>> blendedModelCommands = new ArrayList<>();
		private final Set<RenderLayer> usedModelRenderLayers = new ObjectOpenHashSet();

		/**
		 * Add.
		 *
		 * @param renderLayer render layer
		 * @param modelCommand model command
		 */
		public void add(RenderLayer renderLayer, OrderedRenderCommandQueueImpl.ModelCommand<?> modelCommand) {
			if (renderLayer.getRenderPipeline().getBlendFunction().isEmpty()) {
				this.opaqueModelCommands
						.computeIfAbsent(renderLayer, renderLayerx -> new ArrayList<>())
						.add(modelCommand);
			}
			else {
				Vector3f vector3f = modelCommand.matricesEntry().getPositionMatrix().transformPosition(new Vector3f());
				this.blendedModelCommands.add(new OrderedRenderCommandQueueImpl.BlendedModelCommand<>(
						modelCommand,
						renderLayer,
						vector3f
				));
			}
		}

		/**
		 * Clear.
		 */
		public void clear() {
			this.blendedModelCommands.clear();

			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelCommand<?>>> entry : this.opaqueModelCommands.entrySet()) {
				List<OrderedRenderCommandQueueImpl.ModelCommand<?>> list = entry.getValue();
				if (!list.isEmpty()) {
					this.usedModelRenderLayers.add(entry.getKey());
					list.clear();
				}
			}
		}

		/**
		 * Next frame.
		 */
		public void nextFrame() {
			this.opaqueModelCommands
					.keySet()
					.removeIf(renderLayer -> !this.usedModelRenderLayers.contains(renderLayer));
			this.usedModelRenderLayers.clear();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CrumblingOverlayCommand}.
	 */
	public record CrumblingOverlayCommand(int progress, MatrixStack.Entry cameraMatricesEntry) {
	}
}
