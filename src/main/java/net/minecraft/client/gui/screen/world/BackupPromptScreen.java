package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран предупреждения с предложением создать резервную копию мира перед входом.
 * Отображается при попытке открыть мир, требующий обновления или потенциально несовместимый.
 */
@Environment(EnvType.CLIENT)
public class BackupPromptScreen extends Screen {

	private static final int TITLE_Y = 50;
	private static final int TEXT_Y = 70;
	private static final int LINE_HEIGHT = 9;
	private static final int BUTTON_OFFSET_X = 155;
	private static final int BUTTON_OFFSET_X_CENTER = 80;
	private static final int BUTTON_OFFSET_X_RIGHT = 160;
	private static final Text SKIP_BUTTON_TEXT = Text.translatable("selectWorld.backupJoinSkipButton");
	public static final Text CONFIRM_BUTTON_TEXT = Text.translatable("selectWorld.backupJoinConfirmButton");

	private final Runnable onCancel;
	protected final BackupPromptScreen.Callback callback;
	private final Text subtitle;
	private final boolean showEraseCacheCheckbox;
	private MultilineText wrappedText = MultilineText.EMPTY;
	final Text firstButtonText;
	protected int contentY;
	private CheckboxWidget eraseCacheCheckbox;

	public BackupPromptScreen(
			Runnable onCancel,
			BackupPromptScreen.Callback callback,
			Text title,
			Text subtitle,
			boolean showEraseCacheCheckbox
	) {
		this(onCancel, callback, title, subtitle, CONFIRM_BUTTON_TEXT, showEraseCacheCheckbox);
	}

	public BackupPromptScreen(
			Runnable onCancel,
			BackupPromptScreen.Callback callback,
			Text title,
			Text subtitle,
			Text firstButtonText,
			boolean showEraseCacheCheckbox
	) {
		super(title);
		this.onCancel = onCancel;
		this.callback = callback;
		this.subtitle = subtitle;
		this.showEraseCacheCheckbox = showEraseCacheCheckbox;
		this.firstButtonText = firstButtonText;
	}

	@Override
	protected void init() {
		super.init();
		wrappedText = MultilineText.create(textRenderer, subtitle, width - 50);
		int textHeight = (wrappedText.getLineCount() + 1) * LINE_HEIGHT;

		eraseCacheCheckbox = CheckboxWidget
				.builder(
						Text.translatable("selectWorld.backupEraseCache").withColor(-2039584),
						textRenderer
				)
				.pos(width / 2 - BUTTON_OFFSET_X + BUTTON_OFFSET_X_CENTER, 76 + textHeight)
				.build();

		if (showEraseCacheCheckbox) {
			addDrawableChild(eraseCacheCheckbox);
		}

		addDrawableChild(
				ButtonWidget
						.builder(
								firstButtonText,
								button -> callback.proceed(true, eraseCacheCheckbox.isChecked())
						)
						.dimensions(width / 2 - BUTTON_OFFSET_X, 100 + textHeight, 150, 20)
						.build()
		);
		addDrawableChild(
				ButtonWidget
						.builder(
								SKIP_BUTTON_TEXT,
								button -> callback.proceed(false, eraseCacheCheckbox.isChecked())
						)
						.dimensions(width / 2 - BUTTON_OFFSET_X + BUTTON_OFFSET_X_RIGHT, 100 + textHeight, 150, 20)
						.build()
		);
		addDrawableChild(
				ButtonWidget
						.builder(ScreenTexts.CANCEL, button -> onCancel.run())
						.dimensions(width / 2 - BUTTON_OFFSET_X + BUTTON_OFFSET_X_CENTER, 124 + textHeight, 150, 20)
						.build()
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer textConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
		wrappedText.draw(Alignment.CENTER, width / 2, TEXT_Y, LINE_HEIGHT, textConsumer);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == 256) {
			onCancel.run();
			return true;
		}

		return super.keyPressed(input);
	}

	/**
	 * Колбэк для обработки выбора пользователя на экране резервного копирования.
	 */
	@Environment(EnvType.CLIENT)
	public interface Callback {

		void proceed(boolean backup, boolean eraseCache);
	}
}
