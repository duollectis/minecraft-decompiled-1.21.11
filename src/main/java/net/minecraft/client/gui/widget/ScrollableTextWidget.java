package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
/**
 * {@code ScrollableTextWidget}.
 */
public class ScrollableTextWidget extends ScrollableTextFieldWidget {

	private final TextRenderer textRenderer;
	private final MultilineTextWidget wrapped;

	public ScrollableTextWidget(int x, int y, int width, int height, Text message, TextRenderer textRenderer) {
		super(x, y, width, height, message);
		this.textRenderer = textRenderer;
		this.wrapped = new MultilineTextWidget(message, textRenderer).setMaxWidth(this.getWidth() - this.getPadding());
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		this.wrapped.setMaxWidth(this.getWidth() - this.getPadding());
	}

	@Override
	protected int getContentsHeight() {
		return this.wrapped.getHeight();
	}

	/**
	 * Обновляет height.
	 */
	public void updateHeight() {
		if (!this.textOverflows()) {
			this.setHeight(this.getContentsHeight() + this.getPadding());
		}
	}

	@Override
	protected double getDeltaYPerScroll() {
		return 9.0;
	}

	@Override
	protected void drawBox(DrawContext context) {
		super.drawBox(context);
	}

	/**
	 * Text overflows.
	 *
	 * @return boolean — результат операции
	 */
	public boolean textOverflows() {
		return super.overflows();
	}

	@Override
	protected void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(this.getTextX(), this.getTextY());
		this.wrapped.render(context, mouseX, mouseY, deltaTicks);
		context.getMatrices().popMatrix();
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getMessage());
	}

	@Override
	public void setMessage(Text message) {
		super.setMessage(message);
		this.wrapped.setMessage(message);
	}
}
