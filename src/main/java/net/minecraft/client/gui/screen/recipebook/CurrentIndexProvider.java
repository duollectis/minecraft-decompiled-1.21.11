package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code CurrentIndexProvider}.
 */
public interface CurrentIndexProvider {

	int currentIndex();
}
