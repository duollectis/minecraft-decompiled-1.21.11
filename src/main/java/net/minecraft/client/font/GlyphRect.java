package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

/**
 * Прямоугольная область глифа в экранных координатах.
 * Используется для позиционирования эффектов (подчёркивание, зачёркивание, тень).
 */
@Environment(EnvType.CLIENT)
public interface GlyphRect {

	Style style();

	float getLeft();

	float getTop();

	float getRight();

	float getBottom();
}
