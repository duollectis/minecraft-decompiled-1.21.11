package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code Navigable}.
 */
public interface Navigable {

	default int getNavigationOrder() {
		return 0;
	}
}
