package net.minecraft.client.gui.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.text.OrderedText;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * Состояние текстового элемента GUI до его растеризации в глифы.
 * Хранит все параметры, необходимые для подготовки текста через {@link TextRenderer}.
 *
 * <p>Подготовка ({@link #prepare()}) выполняется лениво при первом обращении к {@link #bounds()}.
 * После подготовки вычисляется ограничивающий прямоугольник с учётом матрицы трансформации
 * и области отсечения, который используется системой слоёв GUI.
 */
@Environment(EnvType.CLIENT)
public final class TextGuiElementRenderState implements GuiElementRenderState {

	public final TextRenderer textRenderer;
	public final OrderedText orderedText;
	public final Matrix3x2fc matrix;
	public final int x;
	public final int y;
	public final int color;
	public final int backgroundColor;
	public final boolean shadow;
	final boolean trackEmpty;
	public final @Nullable ScreenRect clipBounds;

	private TextRenderer.@Nullable GlyphDrawable preparation;
	private @Nullable ScreenRect bounds;

	public TextGuiElementRenderState(
			TextRenderer textRenderer,
			OrderedText orderedText,
			Matrix3x2fc matrix,
			int x,
			int y,
			int color,
			int backgroundColor,
			boolean shadow,
			boolean trackEmpty,
			@Nullable ScreenRect clipBounds
	) {
		this.textRenderer = textRenderer;
		this.orderedText = orderedText;
		this.matrix = matrix;
		this.x = x;
		this.y = y;
		this.color = color;
		this.backgroundColor = backgroundColor;
		this.shadow = shadow;
		this.trackEmpty = trackEmpty;
		this.clipBounds = clipBounds;
	}

	/**
	 * Выполняет растеризацию текста в набор глифов.
	 * Вычисляет и кэширует ограничивающий прямоугольник с учётом матрицы и области отсечения.
	 * Повторные вызовы возвращают кэшированный результат.
	 */
	public TextRenderer.GlyphDrawable prepare() {
		if (preparation != null) {
			return preparation;
		}

		preparation = textRenderer.prepare(
				orderedText,
				x,
				y,
				color,
				shadow,
				trackEmpty,
				backgroundColor
		);

		ScreenRect textRect = preparation.getScreenRect();
		if (textRect != null) {
			textRect = textRect.transformEachVertex(matrix);
			bounds = clipBounds != null ? clipBounds.intersection(textRect) : textRect;
		}

		return preparation;
	}

	@Override
	public @Nullable ScreenRect bounds() {
		prepare();
		return bounds;
	}
}
