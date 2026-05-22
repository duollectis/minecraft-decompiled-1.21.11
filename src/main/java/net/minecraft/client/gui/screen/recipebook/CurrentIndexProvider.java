package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Поставщик текущего индекса для циклической анимации иконок рецептов.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface CurrentIndexProvider {

	int currentIndex();
}
