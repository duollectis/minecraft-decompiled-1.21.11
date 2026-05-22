package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Экран уведомления с текстом и одной кнопкой действия.
 */
@Environment(EnvType.CLIENT)
public class NoticeScreen extends Screen {

	private static final int NOTICE_TEXT_Y = 90;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LINE_HEIGHT = 9;

	private final Text notice;
	private MultilineText noticeLines = MultilineText.EMPTY;
	private final Runnable actionHandler;
	private final Text buttonText;
	private final boolean shouldCloseOnEsc;

	public NoticeScreen(Runnable actionHandler, Text title, Text notice) {
		this(actionHandler, title, notice, ScreenTexts.BACK, true);
	}

	public NoticeScreen(Runnable actionHandler, Text title, Text notice, Text buttonText, boolean shouldCloseOnEsc) {
		super(title);
		this.actionHandler = actionHandler;
		this.notice = notice;
		this.buttonText = buttonText;
		this.shouldCloseOnEsc = shouldCloseOnEsc;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), notice);
	}

	@Override
	protected void init() {
		super.init();
		noticeLines = MultilineText.create(textRenderer, notice, width - 50);
		int textHeight = noticeLines.getLineCount() * LINE_HEIGHT;
		int buttonY = MathHelper.clamp(NOTICE_TEXT_Y + textHeight + 12, height / 6 + 96, height - 24);

		addDrawableChild(
				ButtonWidget.builder(buttonText, button -> actionHandler.run())
						.dimensions((width - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
						.build()
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer drawnTextConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 70, -1);
		noticeLines.draw(Alignment.CENTER, width / 2, NOTICE_TEXT_Y, LINE_HEIGHT, drawnTextConsumer);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return shouldCloseOnEsc;
	}
}
