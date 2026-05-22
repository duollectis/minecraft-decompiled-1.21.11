package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Позиционер тултипа, привязанный к прямоугольнику виджета.
 * Размещает тултип под виджетом (или над ним при нехватке места),
 * с плавным вертикальным смещением, зависящим от расстояния до края виджета.
 * Горизонтально смещается вправо от курсора, не выходя за края экрана.
 */
@Environment(EnvType.CLIENT)
public class WidgetTooltipPositioner implements TooltipPositioner {

	/** Минимальный отступ от краёв экрана. */
	private static final int SCREEN_EDGE_MARGIN = 5;
	/** Горизонтальное смещение тултипа от исходной X-координаты. */
	private static final int HORIZONTAL_OFFSET = 12;
	/** Минимальный отступ от левого края при переполнении. */
	private static final int MIN_LEFT_MARGIN = 9;
	public static final int VERTICAL_OFFSET = 3;
	public static final int TOOLTIP_EDGE_MARGIN = 5;
	/** Минимальный вертикальный отступ при интерполяции (нижняя граница lerp). */
	private static final float LERP_MIN_OFFSET = 5.0F;

	private final ScreenRect focus;

	public WidgetTooltipPositioner(ScreenRect focus) {
		this.focus = focus;
	}

	/**
	 * Вычисляет позицию тултипа относительно виджета.
	 * Вертикальное смещение интерполируется между высотой виджета и {@value #LERP_MIN_OFFSET}
	 * в зависимости от расстояния тултипа до края виджета — создаёт эффект «прилипания».
	 */
	@Override
	public Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height) {
		Vector2i pos = new Vector2i(x + HORIZONTAL_OFFSET, y);

		if (pos.x + width > screenWidth - SCREEN_EDGE_MARGIN) {
			pos.x = Math.max(x - HORIZONTAL_OFFSET - width, MIN_LEFT_MARGIN);
		}

		pos.y += VERTICAL_OFFSET;
		int totalHeight = height + VERTICAL_OFFSET * 2;
		int belowFocus = focus.getBottom() + VERTICAL_OFFSET + getOffsetY(0, 0, focus.height());
		int screenBottom = screenHeight - SCREEN_EDGE_MARGIN;

		if (belowFocus + totalHeight <= screenBottom) {
			pos.y = pos.y + getOffsetY(pos.y, focus.getTop(), focus.height());
		} else {
			pos.y = pos.y - (totalHeight + getOffsetY(pos.y, focus.getBottom(), focus.height()));
		}

		return pos;
	}

	/**
	 * Вычисляет вертикальное смещение через линейную интерполяцию.
	 * Чем ближе тултип к краю виджета, тем меньше смещение (стремится к {@value #LERP_MIN_OFFSET}).
	 */
	private static int getOffsetY(int tooltipY, int widgetY, int widgetHeight) {
		int distance = Math.min(Math.abs(tooltipY - widgetY), widgetHeight);
		return Math.round(MathHelper.lerp((float) distance / widgetHeight, (float) (widgetHeight - 3), LERP_MIN_OFFSET));
	}
}
