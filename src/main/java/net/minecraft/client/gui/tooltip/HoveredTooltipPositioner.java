package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Позиционер тултипа при наведении курсора мыши.
 * Размещает тултип правее и выше курсора, предотвращая выход за края экрана.
 * Синглтон — используется через {@link #INSTANCE}.
 */
@Environment(EnvType.CLIENT)
public class HoveredTooltipPositioner implements TooltipPositioner {

	public static final TooltipPositioner INSTANCE = new HoveredTooltipPositioner();

	/** Горизонтальное смещение тултипа от курсора вправо. */
	private static final int CURSOR_OFFSET_X = 12;
	/** Вертикальное смещение тултипа от курсора вверх. */
	private static final int CURSOR_OFFSET_Y = -12;
	/** Смещение влево при переполнении правого края (курсор + смещение * 2). */
	private static final int OVERFLOW_SHIFT_X = 24;
	/** Минимальный отступ от левого края экрана. */
	private static final int MIN_LEFT_MARGIN = 4;
	/** Нижний зазор между тултипом и нижним краем экрана. */
	private static final int BOTTOM_GAP = 3;

	private HoveredTooltipPositioner() {
	}

	@Override
	public Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height) {
		Vector2i pos = new Vector2i(x, y).add(CURSOR_OFFSET_X, CURSOR_OFFSET_Y);
		preventOverflow(screenWidth, screenHeight, pos, width, height);
		return pos;
	}

	private void preventOverflow(int screenWidth, int screenHeight, Vector2i pos, int width, int height) {
		if (pos.x + width > screenWidth) {
			pos.x = Math.max(pos.x - OVERFLOW_SHIFT_X - width, MIN_LEFT_MARGIN);
		}

		int totalHeight = height + BOTTOM_GAP;
		if (pos.y + totalHeight > screenHeight) {
			pos.y = screenHeight - totalHeight;
		}
	}
}
