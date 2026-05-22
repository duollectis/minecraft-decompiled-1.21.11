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
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Экран отображения выполняемой задачи или её результата.
 * В режиме «выполнение» показывает анимированный индикатор загрузки,
 * в режиме «результат» — многострочный текст описания.
 */
@Environment(EnvType.CLIENT)
public class TaskScreen extends Screen {

	private static final int TITLE_TEXT_Y = 80;
	private static final int DESCRIPTION_TEXT_Y = 120;
	private static final int DESCRIPTION_TEXT_WIDTH = 360;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_BOTTOM_MARGIN = 40;
	private static final int MIN_DESCRIPTION_LINES = 5;
	private static final int LINE_HEIGHT = 9;
	private static final int RESULT_BUTTON_COOLDOWN = 20;
	private static final int LOADING_TEXT_COLOR = -6250336;

	private final @Nullable Text descriptionText;
	private final Text closeButtonText;
	private final Runnable closeCallback;
	private @Nullable MultilineText description;
	private ButtonWidget button;
	private int buttonCooldown;

	public static TaskScreen createRunningScreen(Text title, Text closeButtonText, Runnable closeCallback) {
		return new TaskScreen(title, null, closeButtonText, closeCallback, 0);
	}

	public static TaskScreen createResultScreen(
		Text title,
		Text descriptionText,
		Text closeButtonText,
		Runnable closeCallback
	) {
		return new TaskScreen(title, descriptionText, closeButtonText, closeCallback, RESULT_BUTTON_COOLDOWN);
	}

	protected TaskScreen(
		Text title,
		@Nullable Text descriptionText,
		Text closeButtonText,
		Runnable closeCallback,
		int buttonCooldown
	) {
		super(title);
		this.descriptionText = descriptionText;
		this.closeButtonText = closeButtonText;
		this.closeCallback = closeCallback;
		this.buttonCooldown = buttonCooldown;
	}

	@Override
	protected void init() {
		super.init();

		if (descriptionText != null) {
			description = MultilineText.create(textRenderer, descriptionText, DESCRIPTION_TEXT_WIDTH);
		}

		int lineCount = description != null ? description.getLineCount() : 1;
		int descriptionHeight = Math.max(lineCount, MIN_DESCRIPTION_LINES) * LINE_HEIGHT;
		int buttonY = Math.min(DESCRIPTION_TEXT_Y + descriptionHeight, height - BUTTON_BOTTOM_MARGIN);

		button = addDrawableChild(
			ButtonWidget.builder(closeButtonText, btn -> close())
				.dimensions((width - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build()
		);
	}

	@Override
	public void tick() {
		if (buttonCooldown > 0) {
			buttonCooldown--;
		}

		button.active = buttonCooldown == 0;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer textConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_TEXT_Y, -1);

		if (description == null) {
			String loadingText = LoadingDisplay.get(Util.getMeasuringTimeMs());
			context.drawCenteredTextWithShadow(textRenderer, loadingText, width / 2, DESCRIPTION_TEXT_Y, LOADING_TEXT_COLOR);
		} else {
			description.draw(Alignment.CENTER, width / 2, DESCRIPTION_TEXT_Y, LINE_HEIGHT, textConsumer);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return description != null && button.active;
	}

	@Override
	public void close() {
		closeCallback.run();
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(
			title,
			descriptionText != null ? descriptionText : ScreenTexts.EMPTY
		);
	}
}
