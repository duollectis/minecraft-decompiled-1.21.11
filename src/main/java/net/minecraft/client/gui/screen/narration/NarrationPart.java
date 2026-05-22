package net.minecraft.client.gui.screen.narration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Перечисление частей нарративного сообщения, определяющих порядок озвучивания элементов экрана.
 */
@Environment(EnvType.CLIENT)
public enum NarrationPart {
	TITLE,
	POSITION,
	HINT,
	USAGE;
}
