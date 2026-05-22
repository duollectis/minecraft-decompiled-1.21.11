package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Обработчик событий окна GLFW.
 * Реализуется {@link MinecraftClient} для реакции на изменения состояния окна.
 */
@Environment(EnvType.CLIENT)
public interface WindowEventHandler {

	void onWindowFocusChanged(boolean focused);

	void onResolutionChanged();

	void onCursorEnterChanged();
}
