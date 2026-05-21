package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code FallingBlockCommandRenderer}.
 */
public class FallingBlockCommandRenderer {

	private final MatrixStack matrices = new MatrixStack();

	public void render(
			BatchingRenderCommandQueue queue,
			VertexConsumerProvider.Immediate vertexConsumers,
			BlockRenderManager blockRenderManager,
			OutlineVertexConsumerProvider outlineVertexConsumers
	) {
		for (OrderedRenderCommandQueueImpl.MovingBlockCommand movingBlockCommand : queue.getMovingBlockCommands()) {
			MovingBlockRenderState movingBlockRenderState = movingBlockCommand.movingBlockRenderState();
			BlockState blockState = movingBlockRenderState.blockState;
			List<BlockModelPart> list = blockRenderManager.getModel(blockState)
			                                              .getParts(Random.create(blockState.getRenderingSeed(
					                                              movingBlockRenderState.fallingBlockPos)));
			MatrixStack matrixStack = new MatrixStack();
			matrixStack.multiplyPositionMatrix(movingBlockCommand.matricesEntry());
			blockRenderManager.getModelRenderer()
			                  .render(
					                  movingBlockRenderState,
					                  list,
					                  blockState,
					                  movingBlockRenderState.entityBlockPos,
					                  matrixStack,
					                  vertexConsumers.getBuffer(BlockRenderLayers.getMovingBlockLayer(blockState)),
					                  false,
					                  OverlayTexture.DEFAULT_UV
			                  );
		}

		for (OrderedRenderCommandQueueImpl.BlockCommand blockCommand : queue.getBlockCommands()) {
			this.matrices.push();
			this.matrices.peek().copy(blockCommand.matricesEntry());
			blockRenderManager.renderBlockAsEntity(
					blockCommand.state(),
					this.matrices,
					vertexConsumers,
					blockCommand.lightCoords(),
					blockCommand.overlayCoords()
			);
			if (blockCommand.outlineColor() != 0) {
				outlineVertexConsumers.setColor(blockCommand.outlineColor());
				blockRenderManager.renderBlockAsEntity(
						blockCommand.state(),
						this.matrices,
						outlineVertexConsumers,
						blockCommand.lightCoords(),
						blockCommand.overlayCoords()
				);
			}

			this.matrices.pop();
		}

		for (OrderedRenderCommandQueueImpl.BlockStateModelCommand blockStateModelCommand : queue.getBlockStateModelCommands()) {
			BlockModelRenderer.render(
					blockStateModelCommand.matricesEntry(),
					vertexConsumers.getBuffer(blockStateModelCommand.renderLayer()),
					blockStateModelCommand.model(),
					blockStateModelCommand.r(),
					blockStateModelCommand.g(),
					blockStateModelCommand.b(),
					blockStateModelCommand.lightCoords(),
					blockStateModelCommand.overlayCoords()
			);
			if (blockStateModelCommand.outlineColor() != 0) {
				outlineVertexConsumers.setColor(blockStateModelCommand.outlineColor());
				BlockModelRenderer.render(
						blockStateModelCommand.matricesEntry(),
						outlineVertexConsumers.getBuffer(blockStateModelCommand.renderLayer()),
						blockStateModelCommand.model(),
						blockStateModelCommand.r(),
						blockStateModelCommand.g(),
						blockStateModelCommand.b(),
						blockStateModelCommand.lightCoords(),
						blockStateModelCommand.overlayCoords()
				);
			}
		}
	}
}
