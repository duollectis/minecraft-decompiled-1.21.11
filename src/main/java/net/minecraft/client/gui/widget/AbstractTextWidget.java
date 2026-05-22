package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Базовый абстрактный виджет текста с поддержкой кликабельных стилей (ссылок).
 * Подклассы реализуют метод {@link #draw} для конкретного способа отрисовки текста.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractTextWidget extends ClickableWidget {

	private @Nullable Consumer<Style> clickedStyleConsumer = null;
	private final TextRenderer textRenderer;

	public AbstractTextWidget(int x, int y, int width, int height, Text message, TextRenderer textRenderer) {
		super(x, y, width, height, message);
		this.textRenderer = textRenderer;
	}

	public abstract void draw(DrawnTextConsumer textConsumer);

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		DrawContext.HoverType hoverType;
		if (isHovered()) {
			hoverType = clickedStyleConsumer != null
					? DrawContext.HoverType.TOOLTIP_AND_CURSOR
					: DrawContext.HoverType.TOOLTIP_ONLY;
		}
		else {
			hoverType = DrawContext.HoverType.NONE;
		}

		draw(context.getHoverListener(this, hoverType));
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (clickedStyleConsumer == null) {
			super.onClick(click, doubled);
			return;
		}

		DrawnTextConsumer.ClickHandler clickHandler = new DrawnTextConsumer.ClickHandler(
				getTextRenderer(),
				(int) click.x(),
				(int) click.y()
		);
		draw(clickHandler);

		Style style = clickHandler.getStyle();
		if (style != null) {
			clickedStyleConsumer.accept(style);
			return;
		}

		super.onClick(click, doubled);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}

	protected final TextRenderer getTextRenderer() {
		return textRenderer;
	}

	@Override
	public void setMessage(Text message) {
		super.setMessage(message);
		setWidth(getTextRenderer().getWidth(message.asOrderedText()));
	}

	public AbstractTextWidget onClick(@Nullable Consumer<Style> clickedStyleConsumer) {
		this.clickedStyleConsumer = clickedStyleConsumer;
		return this;
	}
}
