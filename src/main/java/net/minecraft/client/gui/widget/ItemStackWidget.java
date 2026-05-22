package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Виджет для отображения предмета из {@link ItemStack} с опциональным оверлеем и тултипом.
 */
@Environment(EnvType.CLIENT)
public class ItemStackWidget extends ClickableWidget {

	private final MinecraftClient client;
	private final int xOffset;
	private final int yOffset;
	private final ItemStack stack;
	private final boolean drawOverlay;
	private final boolean hasTooltip;

	public ItemStackWidget(
			MinecraftClient client,
			int x,
			int y,
			int width,
			int height,
			Text message,
			ItemStack stack,
			boolean drawOverlay,
			boolean hasTooltip
	) {
		super(0, 0, width, height, message);
		this.client = client;
		this.xOffset = x;
		this.yOffset = y;
		this.stack = stack;
		this.drawOverlay = drawOverlay;
		this.hasTooltip = hasTooltip;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.drawItem(stack, getX() + xOffset, getY() + yOffset, 0);

		if (drawOverlay) {
			context.drawStackOverlay(
					client.textRenderer,
					stack,
					getX() + xOffset,
					getY() + yOffset,
					null
			);
		}

		if (isFocused()) {
			context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(), -1);
		}

		if (hasTooltip && isSelected()) {
			renderTooltip(context, mouseX, mouseY);
		}
	}

	protected void renderTooltip(DrawContext context, int mouseX, int mouseY) {
		context.drawItemTooltip(client.textRenderer, stack, mouseX, mouseY);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, Text.translatable("narration.item", stack.getName()));
	}
}
