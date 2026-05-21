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

@Environment(EnvType.CLIENT)
/**
 * {@code ModelPartCommandRenderer}.
 */
public class ModelPartCommandRenderer {

	private final MatrixStack matrices = new MatrixStack();

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			OutlineVertexConsumerProvider outlineVertexConsumerProvider,
			VertexConsumerProvider.Immediate immediate
	) {
		ModelPartCommandRenderer.Commands commands = queue.getModelPartCommands();

		for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>> entry : commands.modelPartCommands.entrySet()) {
			RenderLayer renderLayer = entry.getKey();
			List<OrderedRenderCommandQueueImpl.ModelPartCommand> list = entry.getValue();
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

			for (OrderedRenderCommandQueueImpl.ModelPartCommand modelPartCommand : list) {
				VertexConsumer vertexConsumer2;
				if (modelPartCommand.sprite() != null) {
					if (modelPartCommand.hasGlint()) {
						vertexConsumer2 = modelPartCommand.sprite()
						                                  .getTextureSpecificVertexConsumer(ItemRenderer.getItemGlintConsumer(
								                                  vertexConsumers,
								                                  renderLayer,
								                                  modelPartCommand.sheeted(),
								                                  true
						                                  ));
					}
					else {
						vertexConsumer2 = modelPartCommand.sprite().getTextureSpecificVertexConsumer(vertexConsumer);
					}
				}
				else if (modelPartCommand.hasGlint()) {
					vertexConsumer2 =
							ItemRenderer.getItemGlintConsumer(
									vertexConsumers,
									renderLayer,
									modelPartCommand.sheeted(),
									true
							);
				}
				else {
					vertexConsumer2 = vertexConsumer;
				}

				this.matrices.peek().copy(modelPartCommand.matricesEntry());
				modelPartCommand.modelPart()
				                .render(
						                this.matrices,
						                vertexConsumer2,
						                modelPartCommand.lightCoords(),
						                modelPartCommand.overlayCoords(),
						                modelPartCommand.tintedColor()
				                );
				if (modelPartCommand.outlineColor() != 0 && (renderLayer.getAffectedOutline().isPresent()
						|| renderLayer.isOutline()
				)) {
					outlineVertexConsumerProvider.setColor(modelPartCommand.outlineColor());
					VertexConsumer vertexConsumer3 = outlineVertexConsumerProvider.getBuffer(renderLayer);
					modelPartCommand.modelPart()
					                .render(
							                this.matrices,
							                modelPartCommand.sprite() == null ? vertexConsumer3 : modelPartCommand
							                                                                      .sprite()
							                                                                      .getTextureSpecificVertexConsumer(
									                                                                      vertexConsumer3),
							                modelPartCommand.lightCoords(),
							                modelPartCommand.overlayCoords(),
							                modelPartCommand.tintedColor()
					                );
				}

				if (modelPartCommand.crumblingOverlay() != null) {
					VertexConsumer vertexConsumer3 = new OverlayVertexConsumer(
							immediate.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(modelPartCommand
									.crumblingOverlay()
									.progress())),
							modelPartCommand.crumblingOverlay().cameraMatricesEntry(),
							1.0F
					);
					modelPartCommand.modelPart()
					                .render(
							                this.matrices,
							                vertexConsumer3,
							                modelPartCommand.lightCoords(),
							                modelPartCommand.overlayCoords(),
							                modelPartCommand.tintedColor()
					                );
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Commands}.
	 */
	public static class Commands {

		final Map<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>>
				modelPartCommands =
				new HashMap<>();
		private final Set<RenderLayer> modelPartLayers = new ObjectOpenHashSet();

		public void add(RenderLayer renderLayer, OrderedRenderCommandQueueImpl.ModelPartCommand command) {
			this.modelPartCommands.computeIfAbsent(renderLayer, renderLayerx -> new ArrayList<>()).add(command);
		}

		public void clear() {
			for (Entry<RenderLayer, List<OrderedRenderCommandQueueImpl.ModelPartCommand>> entry : this.modelPartCommands.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					this.modelPartLayers.add(entry.getKey());
					entry.getValue().clear();
				}
			}
		}

		public void nextFrame() {
			this.modelPartCommands.keySet().removeIf(renderLayer -> !this.modelPartLayers.contains(renderLayer));
			this.modelPartLayers.clear();
		}
	}
}
