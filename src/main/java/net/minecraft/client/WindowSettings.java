package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
/**
 * {@code WindowSettings}.
 */
public record WindowSettings(
		int width,
		int height,
		OptionalInt fullscreenWidth,
		OptionalInt fullscreenHeight,
		boolean fullscreen
) {

	/**
	 * With dimensions.
	 *
	 * @param width width
	 * @param height height
	 *
	 * @return WindowSettings — результат операции
	 */
	public WindowSettings withDimensions(int width, int height) {
		return new WindowSettings(width, height, this.fullscreenWidth, this.fullscreenHeight, this.fullscreen);
	}

	/**
	 * With fullscreen.
	 *
	 * @param fullscreen fullscreen
	 *
	 * @return WindowSettings — результат операции
	 */
	public WindowSettings withFullscreen(boolean fullscreen) {
		return new WindowSettings(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, fullscreen);
	}
}
