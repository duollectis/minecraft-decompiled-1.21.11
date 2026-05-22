package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code WaitingForResponseScreen}.
 */
public class WaitingForResponseScreen extends Screen {

	private static final Text TITLE = Text.translatable("gui.waitingForResponse.title");
	private static final Text[] BUTTON_TEXTS = new Text[]{
			Text.empty(),
			Text.translatable("gui.waitingForResponse.button.inactive", 4),
			Text.translatable("gui.waitingForResponse.button.inactive", 3),
			Text.translatable("gui.waitingForResponse.button.inactive", 2),
			Text.translatable("gui.waitingForResponse.button.inactive", 1),
			ScreenTexts.BACK
	};
	private static final int SECONDS_BEFORE_BACK_BUTTON_APPEARS = 1;
	private static final int SECONDS_BEFORE_BACK_BUTTON_ACTIVATES = 5;
	private final @Nullable Screen parent;
	private final ThreePartsLayoutWidget layout;
	private final ButtonWidget backButton;
	private int inactiveTicks;

	public WaitingForResponseScreen(@Nullable Screen parent) {
		super(TITLE);
		this.parent = parent;
		this.layout = new ThreePartsLayoutWidget(this, 33, 0);
		this.backButton = ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).width(200).build();
	}

	@Override
	protected void init() {
		super.init();
		layout.addHeader(TITLE, textRenderer);
		layout.addBody(backButton);
		backButton.visible = false;
		backButton.active = false;
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public void tick() {
		super.tick();
		if (backButton.active) {
			return;
		}

		int elapsedSeconds = inactiveTicks++ / 20;
		backButton.visible = elapsedSeconds >= SECONDS_BEFORE_BACK_BUTTON_APPEARS;
		backButton.setMessage(BUTTON_TEXTS[elapsedSeconds]);

		if (elapsedSeconds == SECONDS_BEFORE_BACK_BUTTON_ACTIVATES) {
			backButton.active = true;
			narrateScreenIfNarrationEnabled(true);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return backButton.active;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	public @Nullable Screen getParentScreen() {
		return parent;
	}
}
