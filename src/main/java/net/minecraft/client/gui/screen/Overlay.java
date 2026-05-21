package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Drawable;

@Environment(EnvType.CLIENT)
/**
 * {@code Overlay}.
 */
public abstract class Overlay implements Drawable {

	/**
	 * Pauses game.
	 *
	 * @return boolean — результат операции
	 */
	public boolean pausesGame() {
		return true;
	}

	/**
	 * Tick.
	 */
	public void tick() {
	}
}
