package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2ic;

/**
 * Стратегия позиционирования тултипа на экране.
 * Принимает размеры экрана, исходную позицию курсора/виджета и размеры тултипа,
 * возвращает итоговые координаты верхнего левого угла тултипа.
 *
 * <p>Стандартные реализации:
 * <ul>
 *   <li>{@link HoveredTooltipPositioner} — при наведении мыши</li>
 *   <li>{@link FocusedTooltipPositioner} — при фокусе клавиатуры</li>
 *   <li>{@link WidgetTooltipPositioner} — привязка к виджету</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public interface TooltipPositioner {

	/**
	 * Вычисляет позицию тултипа.
	 *
	 * @param screenWidth  ширина экрана в пикселях
	 * @param screenHeight высота экрана в пикселях
	 * @param x            исходная X-координата (курсор или виджет)
	 * @param y            исходная Y-координата (курсор или виджет)
	 * @param width        ширина тултипа
	 * @param height       высота тултипа
	 * @return неизменяемый вектор с итоговыми координатами тултипа
	 */
	Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height);
}
