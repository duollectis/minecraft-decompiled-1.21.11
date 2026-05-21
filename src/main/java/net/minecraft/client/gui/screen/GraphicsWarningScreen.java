package net.minecraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
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

@Environment(EnvType.CLIENT)
/**
 * {@code GraphicsWarningScreen}.
 */
public class GraphicsWarningScreen extends Screen {

	private static final int BUTTON_PADDING = 20;
	private static final int BUTTON_MARGIN = 5;
	private static final int BUTTON_HEIGHT = 20;
	private final Text narrationMessage;
	private final List<Text> message;
	private final ImmutableList<GraphicsWarningScreen.ChoiceButton> choiceButtons;
	private MultilineText lines = MultilineText.EMPTY;
	private int linesY;
	private int buttonWidth;

	public GraphicsWarningScreen(
			Text title,
			List<Text> messages,
			ImmutableList<GraphicsWarningScreen.ChoiceButton> choiceButtons
	) {
		super(title);
		this.message = messages;
		this.narrationMessage = ScreenTexts.joinSentences(title, Texts.join(messages, ScreenTexts.EMPTY));
		this.choiceButtons = choiceButtons;
	}

	@Override
	public Text getNarratedTitle() {
		return this.narrationMessage;
	}

	@Override
	public void init() {
		UnmodifiableIterator i = this.choiceButtons.iterator();

		while (i.hasNext()) {
			GraphicsWarningScreen.ChoiceButton choiceButton = (GraphicsWarningScreen.ChoiceButton) i.next();
			this.buttonWidth = Math.max(this.buttonWidth, 20 + this.textRenderer.getWidth(choiceButton.message) + 20);
		}

		int ix = 5 + this.buttonWidth + 5;
		int j = ix * this.choiceButtons.size();
		this.lines = MultilineText.create(this.textRenderer, j, this.message.toArray(new Text[0]));
		int k = this.lines.getLineCount() * 9;
		this.linesY = (int) (this.height / 2.0 - k / 2.0);
		int l = this.linesY + k + 9 * 2;
		int m = (int) (this.width / 2.0 - j / 2.0);

		for (UnmodifiableIterator var6 = this.choiceButtons.iterator(); var6.hasNext(); m += ix) {
			GraphicsWarningScreen.ChoiceButton choiceButton2 = (GraphicsWarningScreen.ChoiceButton) var6.next();
			this.addDrawableChild(ButtonWidget
					.builder(choiceButton2.message, choiceButton2.pressAction)
					.dimensions(m, l, this.buttonWidth, 20)
					.build());
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.linesY - 9 * 2, -1);
		this.lines.draw(Alignment.CENTER, this.width / 2, this.linesY, 9, drawnTextConsumer);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ChoiceButton}.
	 */
	public static final class ChoiceButton {

		final Text message;
		final ButtonWidget.PressAction pressAction;

		public ChoiceButton(Text message, ButtonWidget.PressAction pressAction) {
			this.message = message;
			this.pressAction = pressAction;
		}
	}
}
