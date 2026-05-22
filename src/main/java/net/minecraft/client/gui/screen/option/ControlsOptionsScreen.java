package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

/**
 * Экран настроек управления — содержит кнопки перехода к настройкам мыши и клавиш,
 * а также набор переключаемых опций (прыжок, спринт, атака и т.д.).
 */
@Environment(EnvType.CLIENT)
public class ControlsOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("controls.title");

	private static SimpleOption<?>[] getOptions(GameOptions gameOptions) {
		return new SimpleOption[]{
				gameOptions.getSneakToggled(),
				gameOptions.getSprintToggled(),
				gameOptions.getAttackToggled(),
				gameOptions.getUseToggled(),
				gameOptions.getAutoJump(),
				gameOptions.getSprintWindow(),
				gameOptions.getOperatorItemsTab()
		};
	}

	public ControlsOptionsScreen(Screen parent, GameOptions options) {
		super(parent, options, TITLE_TEXT);
	}

	@Override
	protected void addOptions() {
		body.addWidgetEntry(
				ButtonWidget
						.builder(
								Text.translatable("options.mouse_settings"),
								button -> client.setScreen(new MouseOptionsScreen(this, gameOptions))
						)
						.build(),
				ButtonWidget
						.builder(
								Text.translatable("controls.keybinds"),
								button -> client.setScreen(new KeybindsScreen(this, gameOptions))
						)
						.build()
		);
		body.addAll(getOptions(gameOptions));
	}
}
