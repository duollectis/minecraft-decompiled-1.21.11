package net.minecraft.client.gui.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Экран подтверждения с кнопками «Да» и «Нет».
 * Поддерживает временную блокировку кнопок через {@link #disableButtons(int)}.
 */
@Environment(EnvType.CLIENT)
public class ConfirmScreen extends Screen {

	private static final int KEY_ESCAPE = 256;

	private final Text message;
	protected DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(8);
	protected Text yesText;
	protected Text noText;
	protected @Nullable ButtonWidget yesButton;
	protected @Nullable ButtonWidget noButton;
	private int buttonEnableTimer;
	protected final BooleanConsumer callback;

	public ConfirmScreen(BooleanConsumer callback, Text title, Text message) {
		this(callback, title, message, ScreenTexts.YES, ScreenTexts.NO);
	}

	public ConfirmScreen(BooleanConsumer callback, Text title, Text message, Text yesText, Text noText) {
		super(title);
		this.callback = callback;
		this.message = message;
		this.yesText = yesText;
		this.noText = noText;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), message);
	}

	@Override
	protected void init() {
		super.init();
		layout.getMainPositioner().alignHorizontalCenter();
		layout.add(new TextWidget(title, textRenderer));
		layout.add(
				new MultilineTextWidget(message, textRenderer)
						.setMaxWidth(width - 50)
						.setMaxRows(15)
						.setCentered(true)
		);
		initExtras();

		DirectionalLayoutWidget buttonRow = layout.add(DirectionalLayoutWidget.horizontal().spacing(4));
		buttonRow.getMainPositioner().marginTop(16);
		addButtons(buttonRow);
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	protected void initExtras() {
	}

	protected void addButtons(DirectionalLayoutWidget buttonLayout) {
		yesButton = buttonLayout.add(ButtonWidget.builder(yesText, button -> callback.accept(true)).build());
		noButton = buttonLayout.add(ButtonWidget.builder(noText, button -> callback.accept(false)).build());
	}

	public void disableButtons(int ticks) {
		buttonEnableTimer = ticks;
		yesButton.active = false;
		noButton.active = false;
	}

	@Override
	public void tick() {
		super.tick();
		if (--buttonEnableTimer == 0) {
			yesButton.active = true;
			noButton.active = true;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (buttonEnableTimer <= 0 && input.key() == KEY_ESCAPE) {
			callback.accept(false);
			return true;
		}

		return super.keyPressed(input);
	}
}
