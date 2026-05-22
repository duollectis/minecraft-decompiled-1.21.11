package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Виджет индикатора загрузки: отображает сообщение и анимированную строку прогресса из {@link LoadingDisplay}.
 */
@Environment(EnvType.CLIENT)
public class LoadingWidget extends ClickableWidget {

	private static final int TEXT_COLOR = -1;
	private static final int LOADING_TEXT_COLOR = -8355712;
	private static final int LINE_HEIGHT = 9;

	private final TextRenderer textRenderer;

	public LoadingWidget(TextRenderer textRenderer, Text message) {
		super(0, 0, textRenderer.getWidth(message), LINE_HEIGHT * 3, message);
		this.textRenderer = textRenderer;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int centerX = getX() + getWidth() / 2;
		int centerY = getY() + getHeight() / 2;
		Text message = getMessage();
		context.drawTextWithShadow(textRenderer, message, centerX - textRenderer.getWidth(message) / 2, centerY - LINE_HEIGHT, TEXT_COLOR);

		String loadingText = LoadingDisplay.get(Util.getMeasuringTimeMs());
		context.drawTextWithShadow(
				textRenderer,
				loadingText,
				centerX - textRenderer.getWidth(loadingText) / 2,
				centerY + LINE_HEIGHT,
				LOADING_TEXT_COLOR
		);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public boolean isInteractable() {
		return false;
	}

	@Override
	public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		return null;
	}
}
