package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Экран настроек мыши — управляет чувствительностью, инверсией осей,
 * сенсорным экраном и другими параметрами мыши.
 */
@Environment(EnvType.CLIENT)
public class MouseOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE = Text.translatable("options.mouse_settings.title");

	private static SimpleOption<?>[] getOptions(GameOptions gameOptions) {
		return new SimpleOption[]{
				gameOptions.getMouseSensitivity(),
				gameOptions.getTouchscreen(),
				gameOptions.getMouseWheelSensitivity(),
				gameOptions.getDiscreteMouseScroll(),
				gameOptions.getInvertMouseX(),
				gameOptions.getInvertMouseY(),
				gameOptions.getAllowCursorChanges()
		};
	}

	public MouseOptionsScreen(Screen parent, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE);
	}

	@Override
	protected void addOptions() {
		if (InputUtil.isRawMouseMotionSupported()) {
			body.addAll(Stream
					.concat(
							Arrays.stream(getOptions(gameOptions)),
							Stream.of(gameOptions.getRawMouseInput())
					)
					.toArray(SimpleOption[]::new));
		} else {
			body.addAll(getOptions(gameOptions));
		}
	}
}
