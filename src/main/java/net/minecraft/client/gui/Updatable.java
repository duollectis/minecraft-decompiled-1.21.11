package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Интерфейс для элементов GUI, требующих периодического обновления состояния.
 */
@Environment(EnvType.CLIENT)
public interface Updatable {

	void update();
}
