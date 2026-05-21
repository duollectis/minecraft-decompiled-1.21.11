package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;

@Environment(EnvType.CLIENT)
/**
 * {@code TextCommandRenderer}.
 */
public class TextCommandRenderer {

	public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

		for (OrderedRenderCommandQueueImpl.TextCommand textCommand : queue.getTextCommands()) {
			if (textCommand.outlineColor() == 0) {
				textRenderer.draw(
						textCommand.text(),
						textCommand.x(),
						textCommand.y(),
						textCommand.color(),
						textCommand.dropShadow(),
						textCommand.matricesEntry(),
						vertexConsumers,
						textCommand.layerType(),
						textCommand.backgroundColor(),
						textCommand.lightCoords()
				);
			}
			else {
				textRenderer.drawWithOutline(
						textCommand.text(),
						textCommand.x(),
						textCommand.y(),
						textCommand.color(),
						textCommand.outlineColor(),
						textCommand.matricesEntry(),
						vertexConsumers,
						textCommand.lightCoords()
				);
			}
		}
	}
}
