package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран, отображаемый при нехватке памяти (OutOfMemoryError).
 * Предлагает вернуться на главный экран или выйти из игры.
 */
@Environment(EnvType.CLIENT)
public class OutOfMemoryScreen extends Screen {

	private static final Text TITLE = Text.translatable("outOfMemory.title");
	private static final Text MESSAGE = Text.translatable("outOfMemory.message");
	private static final int MAX_TEXT_WIDTH = 300;

	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

	public OutOfMemoryScreen() {
		super(TITLE);
	}

	@Override
	protected void init() {
		layout.addHeader(TITLE, textRenderer);
		layout.addBody(NarratedMultilineTextWidget.builder(MESSAGE, textRenderer).width(MAX_TEXT_WIDTH).build());

		DirectionalLayoutWidget buttonRow = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		buttonRow.add(ButtonWidget.builder(ScreenTexts.TO_TITLE, button -> client.setScreen(new TitleScreen())).build());
		buttonRow.add(ButtonWidget.builder(Text.translatable("menu.quit"), button -> client.scheduleStop()).build());

		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
