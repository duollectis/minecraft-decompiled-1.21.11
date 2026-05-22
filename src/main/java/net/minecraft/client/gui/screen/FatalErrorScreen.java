package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран отображения фатальной ошибки с кнопкой закрытия.
 */
@Environment(EnvType.CLIENT)
public class FatalErrorScreen extends Screen {

	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_Y = 140;
	private static final int TITLE_Y = 90;
	private static final int MESSAGE_Y = 110;
	private static final int GRADIENT_TOP_COLOR = -12574688;
	private static final int GRADIENT_BOTTOM_COLOR = -11530224;

	private final Text message;

	public FatalErrorScreen(Text title, Text message) {
		super(title);
		this.message = message;
	}

	@Override
	protected void init() {
		super.init();
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> client.setScreen(null))
				.dimensions(width / 2 - BUTTON_WIDTH / 2, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build()
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
		context.drawCenteredTextWithShadow(textRenderer, message, width / 2, MESSAGE_Y, -1);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.fillGradient(0, 0, width, height, GRADIENT_TOP_COLOR, GRADIENT_BOTTOM_COLOR);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
