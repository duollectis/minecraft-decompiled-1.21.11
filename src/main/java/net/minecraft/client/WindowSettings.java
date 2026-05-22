package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.OptionalInt;

/**
 * Иммутабельные настройки окна: размеры, режим полноэкранного отображения.
 * Методы {@code with*} возвращают новый экземпляр с изменённым полем.
 */
@Environment(EnvType.CLIENT)
public record WindowSettings(
	int width,
	int height,
	OptionalInt fullscreenWidth,
	OptionalInt fullscreenHeight,
	boolean fullscreen
) {

	public WindowSettings withDimensions(int width, int height) {
		return new WindowSettings(width, height, fullscreenWidth, fullscreenHeight, fullscreen);
	}

	public WindowSettings withFullscreen(boolean fullscreen) {
		return new WindowSettings(width, height, fullscreenWidth, fullscreenHeight, fullscreen);
	}
}
