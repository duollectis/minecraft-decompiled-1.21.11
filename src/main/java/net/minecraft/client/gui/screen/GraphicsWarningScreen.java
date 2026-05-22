package net.minecraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.List;

/**
 * Экран предупреждения о настройках графики с набором кнопок выбора.
 * Используется для отображения несовместимых или рискованных параметров рендеринга.
 */
@Environment(EnvType.CLIENT)
public class GraphicsWarningScreen extends Screen {

	private static final int BUTTON_PADDING = 20;
	private static final int BUTTON_MARGIN = 5;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 9;
	private static final int TITLE_Y_OFFSET = 2;

	private final Text narrationMessage;
	private final List<Text> message;
	private final ImmutableList<ChoiceButton> choiceButtons;
	private MultilineText lines = MultilineText.EMPTY;
	private int linesY;
	private int buttonWidth;

	public GraphicsWarningScreen(
			Text title,
			List<Text> messages,
			ImmutableList<ChoiceButton> choiceButtons
	) {
		super(title);
		this.message = messages;
		this.narrationMessage = ScreenTexts.joinSentences(title, Texts.join(messages, ScreenTexts.EMPTY));
		this.choiceButtons = choiceButtons;
	}

	@Override
	public Text getNarratedTitle() {
		return narrationMessage;
	}

	@Override
	public void init() {
		for (ChoiceButton choiceButton : choiceButtons) {
			buttonWidth = Math.max(
				buttonWidth,
				BUTTON_PADDING + textRenderer.getWidth(choiceButton.message) + BUTTON_PADDING
			);
		}

		int buttonStride = BUTTON_MARGIN + buttonWidth + BUTTON_MARGIN;
		int totalButtonsWidth = buttonStride * choiceButtons.size();
		lines = MultilineText.create(textRenderer, totalButtonsWidth, message.toArray(new Text[0]));

		int linesHeight = lines.getLineCount() * LINE_HEIGHT;
		linesY = (int) (height / 2.0 - linesHeight / 2.0);

		int buttonsY = linesY + linesHeight + LINE_HEIGHT * TITLE_Y_OFFSET;
		int buttonX = (int) (width / 2.0 - totalButtonsWidth / 2.0);

		for (ChoiceButton choiceButton : choiceButtons) {
			addDrawableChild(
				ButtonWidget.builder(choiceButton.message, choiceButton.pressAction)
					.dimensions(buttonX, buttonsY, buttonWidth, BUTTON_HEIGHT)
					.build()
			);
			buttonX += buttonStride;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer textConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, linesY - LINE_HEIGHT * TITLE_Y_OFFSET, -1);
		lines.draw(Alignment.CENTER, width / 2, linesY, LINE_HEIGHT, textConsumer);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	/**
	 * Кнопка выбора с текстом и обработчиком нажатия для экрана предупреждения.
	 */
	@Environment(EnvType.CLIENT)
	public static final class ChoiceButton {

		final Text message;
		final ButtonWidget.PressAction pressAction;

		public ChoiceButton(Text message, ButtonWidget.PressAction pressAction) {
			this.message = message;
			this.pressAction = pressAction;
		}
	}
}
