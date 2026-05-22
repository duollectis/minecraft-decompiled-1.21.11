package net.minecraft.client.gui.screen.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.LayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Предупреждение о безопасности мультиплеера — показывается при первом входе
 * в раздел мультиплеера. Позволяет запомнить выбор через чекбокс.
 */
@Environment(EnvType.CLIENT)
public class MultiplayerWarningScreen extends WarningScreen {

	private static final Text HEADER = Text.translatable("multiplayerWarning.header").formatted(Formatting.BOLD);
	private static final Text MESSAGE = Text.translatable("multiplayerWarning.message");
	private static final Text CHECK_MESSAGE = Text.translatable("multiplayerWarning.check").withColor(-2039584);
	private static final Text NARRATED_TEXT = HEADER.copy().append("\n").append(MESSAGE);

	private final Screen parent;

	public MultiplayerWarningScreen(Screen parent) {
		super(HEADER, MESSAGE, CHECK_MESSAGE, NARRATED_TEXT);
		this.parent = parent;
	}

	@Override
	protected LayoutWidget getLayout() {
		DirectionalLayoutWidget layout = DirectionalLayoutWidget.horizontal().spacing(8);
		layout.add(ButtonWidget.builder(
			ScreenTexts.PROCEED, button -> {
				if (checkbox.isChecked()) {
					client.options.skipMultiplayerWarning = true;
					client.options.write();
				}

				client.setScreen(new MultiplayerScreen(parent));
			}
		).build());
		layout.add(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).build());
		return layout;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
