package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран настроек скина — управляет видимостью частей модели игрока
 * (плащ, шляпа, рукава и т.д.) и выбором основной руки.
 */
@Environment(EnvType.CLIENT)
public class SkinOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.skinCustomisation.title");

	public SkinOptionsScreen(Screen parent, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE_TEXT);
	}

	@Override
	protected void addOptions() {
		List<ClickableWidget> widgets = new ArrayList<>();

		for (PlayerModelPart part : PlayerModelPart.values()) {
			widgets.add(
					CyclingButtonWidget.onOffBuilder(gameOptions.isPlayerModelPartEnabled(part))
							.build(
									part.getOptionName(),
									(button, enabled) -> gameOptions.setPlayerModelPart(part, enabled)
							)
			);
		}

		widgets.add(gameOptions.getMainArm().createWidget(gameOptions));
		body.addAll(widgets);
	}
}
