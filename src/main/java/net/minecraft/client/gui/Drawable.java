package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Маркерный интерфейс для всех элементов GUI, которые умеют отрисовывать себя на экране.
 */
@Environment(EnvType.CLIENT)
public interface Drawable {

	void render(DrawContext context, int mouseX, int mouseY, float deltaTicks);
}
