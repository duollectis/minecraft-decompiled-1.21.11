package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
/**
 * {@code ItemStackWidget}.
 */
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
		context.drawItem(this.stack, this.getX() + this.xOffset, this.getY() + this.yOffset, 0);
		if (this.drawOverlay) {
			context.drawStackOverlay(
					this.client.textRenderer,
					this.stack,
					this.getX() + this.xOffset,
					this.getY() + this.yOffset,
					null
			);
		}

		if (this.isFocused()) {
			context.drawStrokedRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1);
		}

		if (this.hasTooltip && this.isSelected()) {
			this.renderTooltip(context, mouseX, mouseY);
		}
	}

	/**
	 * Отрисовывает tooltip.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 */
	protected void renderTooltip(DrawContext context, int mouseX, int mouseY) {
		context.drawItemTooltip(this.client.textRenderer, this.stack, mouseX, mouseY);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, Text.translatable("narration.item", this.stack.getName()));
	}
}
