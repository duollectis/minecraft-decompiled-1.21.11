package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Позиционер тултипа для элемента, получившего фокус клавиатурной навигации.
 * Размещает тултип под фокусированным виджетом с небольшим отступом,
 * а при нехватке места снизу — над ним. Горизонтально выравнивается
 * по левому краю виджета, не выходя за правый край экрана.
 */
@Environment(EnvType.CLIENT)
public class FocusedTooltipPositioner implements TooltipPositioner {

	/** Отступ тултипа от границ фокусированного виджета. */
	private static final int MARGIN = 3;
	/** Минимальный отступ от правого края экрана. */
	private static final int EDGE_PADDING = 4;
	/** Дополнительный пиксель смещения для визуального разделения. */
	private static final int EXTRA_PIXEL = 1;

	private final ScreenRect focus;

	public FocusedTooltipPositioner(ScreenRect focus) {
		this.focus = focus;
	}

	@Override
	public Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height) {
		Vector2i pos = new Vector2i();
		pos.x = focus.getLeft() + MARGIN;
		pos.y = focus.getBottom() + MARGIN + EXTRA_PIXEL;

		if (pos.y + height + MARGIN > screenHeight) {
			pos.y = focus.getTop() - height - MARGIN - EXTRA_PIXEL;
		}

		if (pos.x + width > screenWidth) {
			pos.x = Math.max(focus.getRight() - width - MARGIN, EDGE_PADDING);
		}

		return pos;
	}
}
