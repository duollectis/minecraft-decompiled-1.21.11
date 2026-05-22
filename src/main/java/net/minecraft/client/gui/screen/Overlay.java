package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Drawable;

/**
 * Базовый класс для оверлеев поверх игрового экрана (например, экран загрузки).
 */
@Environment(EnvType.CLIENT)
public abstract class Overlay implements Drawable {

	public boolean pausesGame() {
		return true;
	}

	public void tick() {
	}
}
