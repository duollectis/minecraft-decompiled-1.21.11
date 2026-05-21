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
		this.layout.addHeader(TITLE, this.textRenderer);
		this.layout.addBody(this.backButton);
		this.backButton.visible = false;
		this.backButton.active = false;
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		this.layout.refreshPositions();
		SimplePositioningWidget.setPos(this.layout, this.getNavigationFocus());
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.backButton.active) {
			int i = this.inactiveTicks++ / 20;
			this.backButton.visible = i >= 1;
			this.backButton.setMessage(BUTTON_TEXTS[i]);
			if (i == 5) {
				this.backButton.active = true;
				this.narrateScreenIfNarrationEnabled(true);
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return this.backButton.active;
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	public @Nullable Screen getParentScreen() {
		return this.parent;
	}
}
