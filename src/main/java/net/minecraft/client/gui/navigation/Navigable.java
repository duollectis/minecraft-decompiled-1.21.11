package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Маркерный интерфейс для GUI-элементов, участвующих в клавиатурной навигации.
 */
@Environment(EnvType.CLIENT)
public interface Navigable {

	default int getNavigationOrder() {
		return 0;
	}
}
