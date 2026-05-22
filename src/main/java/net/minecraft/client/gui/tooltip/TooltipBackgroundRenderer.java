package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Утилитарный рендерер фона и рамки тултипа.
 * Поддерживает кастомные текстуры через {@link Identifier} стиля тултипа
 * (компонент {@code minecraft:tooltip_style}). При {@code null} используются
 * стандартные текстуры {@code tooltip/background} и {@code tooltip/frame}.
 */
@Environment(EnvType.CLIENT)
public class TooltipBackgroundRenderer {

	private static final Identifier DEFAULT_BACKGROUND_TEXTURE = Identifier.ofVanilla("tooltip/background");
	private static final Identifier DEFAULT_FRAME_TEXTURE = Identifier.ofVanilla("tooltip/frame");

	public static final int BACKGROUND_PADDING = 12;
	public static final int TOP_PADDING = 3;
	public static final int BOTTOM_PADDING = 3;
	public static final int LEFT_PADDING = 3;
	public static final int RIGHT_PADDING = 3;

	/** Внутренний отступ содержимого от рамки. */
	private static final int INNER_PADDING = 3;
	/** Размер угловых элементов текстуры рамки. */
	private static final int CORNER_SIZE = 9;

	/**
	 * Отрисовывает фон и рамку тултипа вокруг области ({@code x}, {@code y}, {@code width}, {@code height}).
	 * Фактические координаты расширяются на {@link #INNER_PADDING} и {@link #CORNER_SIZE} со всех сторон.
	 *
	 * @param texture идентификатор стиля тултипа или {@code null} для стандартного вида
	 */
	public static void render(DrawContext context, int x, int y, int width, int height, @Nullable Identifier texture) {
		int bgX = x - INNER_PADDING - CORNER_SIZE;
		int bgY = y - INNER_PADDING - CORNER_SIZE;
		int bgWidth = width + INNER_PADDING * 2 + CORNER_SIZE * 2;
		int bgHeight = height + INNER_PADDING * 2 + CORNER_SIZE * 2;
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getBackgroundTexture(texture), bgX, bgY, bgWidth, bgHeight);
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getFrameTexture(texture), bgX, bgY, bgWidth, bgHeight);
	}

	private static Identifier getBackgroundTexture(@Nullable Identifier texture) {
		return texture == null
			? DEFAULT_BACKGROUND_TEXTURE
			: texture.withPath(name -> "tooltip/" + name + "_background");
	}

	private static Identifier getFrameTexture(@Nullable Identifier texture) {
		return texture == null
			? DEFAULT_FRAME_TEXTURE
			: texture.withPath(name -> "tooltip/" + name + "_frame");
	}
}
