package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Потребитель отрисованного текста — абстракция над конкретным способом
 * вывода текста (в буфер вершин, в обработчик кликов и т.д.).
 * Содержит вспомогательные методы для бегущей строки (marquee) и
 * статический хелпер для обработки наведения мыши.
 */
@Environment(EnvType.CLIENT)
public interface DrawnTextConsumer {

	/** Период бегущей строки на единицу избыточной ширины (секунды/пиксель). */
	double MARQUEE_PERIOD_PER_EXCESS_WIDTH = 0.5;

	/** Минимальный период бегущей строки в секундах. */
	double MARQUEE_MIN_PERIOD = 3.0;

	DrawnTextConsumer.Transformation getTransformation();

	void setTransformation(DrawnTextConsumer.Transformation transformation);

	default void text(int x, int y, OrderedText text) {
		text(Alignment.LEFT, x, y, getTransformation(), text);
	}

	default void text(int x, int y, Text text) {
		text(Alignment.LEFT, x, y, getTransformation(), text.asOrderedText());
	}

	default void text(Alignment alignment, int x, int y, DrawnTextConsumer.Transformation transformation, Text text) {
		text(alignment, x, y, transformation, text.asOrderedText());
	}

	void text(Alignment alignment, int x, int y, DrawnTextConsumer.Transformation transformation, OrderedText text);

	default void text(Alignment alignment, int x, int y, Text text) {
		text(alignment, x, y, text.asOrderedText());
	}

	default void text(Alignment alignment, int x, int y, OrderedText text) {
		text(alignment, x, y, getTransformation(), text);
	}

	void marqueedText(
			Text text,
			int x,
			int left,
			int right,
			int top,
			int bottom,
			DrawnTextConsumer.Transformation transformation
	);

	default void marqueedText(Text text, int x, int left, int right, int top, int bottom) {
		marqueedText(text, x, left, right, top, bottom, getTransformation());
	}

	default void text(Text text, int left, int right, int top, int bottom) {
		marqueedText(text, (left + right) / 2, left, right, top, bottom);
	}

	/**
	 * Отрисовывает текст в виде бегущей строки, если он не помещается в отведённую ширину.
	 * Использует синусоидальную интерполяцию для плавного движения.
	 *
	 * @param text           текст для отображения
	 * @param x              X-координата центра для центрированного текста
	 * @param left           левая граница области
	 * @param right          правая граница области
	 * @param top            верхняя граница области
	 * @param bottom         нижняя граница области
	 * @param width          измеренная ширина текста в пикселях
	 * @param lineHeight     высота строки в пикселях
	 * @param transformation трансформация (матрица, прозрачность, ножницы)
	 */
	default void marqueedText(
			Text text,
			int x,
			int left,
			int right,
			int top,
			int bottom,
			int width,
			int lineHeight,
			DrawnTextConsumer.Transformation transformation
	) {
		int centeredY = (top + bottom - lineHeight) / 2 + 1;
		int availableWidth = right - left;

		if (width > availableWidth) {
			int excess = width - availableWidth;
			double timeSeconds = Util.getMeasuringTimeMs() / 1000.0;
			double period = Math.max(excess * MARQUEE_PERIOD_PER_EXCESS_WIDTH, MARQUEE_MIN_PERIOD);
			double phase = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * timeSeconds / period)) / 2.0 + 0.5;
			double scrollOffset = MathHelper.lerp(phase, 0.0, (double) excess);
			DrawnTextConsumer.Transformation scissored = transformation.withScissor(left, right, top, bottom);
			text(Alignment.LEFT, left - (int) scrollOffset, centeredY, scissored, text.asOrderedText());
		} else {
			int clampedX = MathHelper.clamp(x, left + width / 2, right - width / 2);
			text(Alignment.CENTER, clampedX, centeredY, text);
		}
	}

	/**
	 * Обрабатывает наведение мыши на текстовый элемент: находит глиф под курсором
	 * и вызывает {@code styleCallback} с его стилем.
	 *
	 * @param renderState   состояние рендеринга текстового элемента
	 * @param mouseX        X-координата мыши в экранных пикселях
	 * @param mouseY        Y-координата мыши в экранных пикселях
	 * @param styleCallback обработчик стиля найденного глифа
	 */
	static void handleHover(
			TextGuiElementRenderState renderState,
			float mouseX,
			float mouseY,
			Consumer<Style> styleCallback
	) {
		ScreenRect screenRect = renderState.bounds();

		if (screenRect == null || !screenRect.contains((int) mouseX, (int) mouseY)) {
			return;
		}

		Vector2fc localPos = renderState.matrix.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
		final float localX = localPos.x();
		final float localY = localPos.y();

		renderState.prepare().draw(new TextRenderer.GlyphDrawer() {
			@Override
			public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
				addGlyphInternal(glyph);
			}

			@Override
			public void drawEmptyGlyphRect(EmptyGlyphRect rect) {
				addGlyphInternal(rect);
			}

			private void addGlyphInternal(GlyphRect glyph) {
				if (DrawnTextConsumer.isWithinBounds(
						localX,
						localY,
						glyph.getLeft(),
						glyph.getTop(),
						glyph.getRight(),
						glyph.getBottom()
				)) {
					styleCallback.accept(glyph.style());
				}
			}
		});
	}

	static boolean isWithinBounds(float x, float y, float left, float top, float right, float bottom) {
		return x >= left && x < right && y >= top && y < bottom;
	}

	/**
	 * Реализация {@link DrawnTextConsumer} для обработки кликов по тексту.
	 * Находит стиль глифа под координатами клика и сохраняет его для последующего использования.
	 */
	@Environment(EnvType.CLIENT)
	public static class ClickHandler implements DrawnTextConsumer {

		private static final DrawnTextConsumer.Transformation DEFAULT_TRANSFORMATION =
				new DrawnTextConsumer.Transformation(new Matrix3x2f());

		/** Высота строки в пикселях (стандартная для Minecraft). */
		private static final int LINE_HEIGHT = 9;

		private final TextRenderer textRenderer;
		private final int clickX;
		private final int clickY;
		private DrawnTextConsumer.Transformation transformation = DEFAULT_TRANSFORMATION;
		private boolean insert;
		private @Nullable Style style;
		private final Consumer<Style> setStyleCallback = style -> {
			if (style.getClickEvent() != null || insert && style.getInsertion() != null) {
				this.style = style;
			}
		};

		public ClickHandler(TextRenderer textRenderer, int clickX, int clickY) {
			this.textRenderer = textRenderer;
			this.clickX = clickX;
			this.clickY = clickY;
		}

		@Override
		public DrawnTextConsumer.Transformation getTransformation() {
			return transformation;
		}

		@Override
		public void setTransformation(DrawnTextConsumer.Transformation transformation) {
			this.transformation = transformation;
		}

		@Override
		public void text(
				Alignment alignment,
				int x,
				int y,
				DrawnTextConsumer.Transformation transformation,
				OrderedText text
		) {
			int adjustedX = alignment.getAdjustedX(x, textRenderer, text);
			TextGuiElementRenderState renderState = new TextGuiElementRenderState(
					textRenderer,
					text,
					transformation.pose(),
					adjustedX,
					y,
					ColorHelper.getWhite(transformation.opacity()),
					0,
					true,
					true,
					transformation.scissor()
			);
			DrawnTextConsumer.handleHover(renderState, clickX, clickY, setStyleCallback);
		}

		@Override
		public void marqueedText(
				Text text,
				int x,
				int left,
				int right,
				int top,
				int bottom,
				DrawnTextConsumer.Transformation transformation
		) {
			int textWidth = textRenderer.getWidth(text);
			marqueedText(text, x, left, right, top, bottom, textWidth, LINE_HEIGHT, transformation);
		}

		public DrawnTextConsumer.ClickHandler insert(boolean insert) {
			this.insert = insert;
			return this;
		}

		public @Nullable Style getStyle() {
			return style;
		}
	}

	/**
	 * Иммутабельная трансформация текстового элемента: матрица позиции,
	 * прозрачность и область ножниц (scissor).
	 */
	@Environment(EnvType.CLIENT)
	public record Transformation(Matrix3x2fc pose, float opacity, @Nullable ScreenRect scissor) {

		public Transformation(Matrix3x2fc pose) {
			this(pose, 1.0F, null);
		}

		public DrawnTextConsumer.Transformation withPose(Matrix3x2fc pose) {
			return new DrawnTextConsumer.Transformation(pose, opacity, scissor);
		}

		public DrawnTextConsumer.Transformation scaled(float scale) {
			return withPose(pose.scale(scale, scale, new Matrix3x2f()));
		}

		public DrawnTextConsumer.Transformation withOpacity(float opacity) {
			return this.opacity == opacity
			       ? this
			       : new DrawnTextConsumer.Transformation(pose, opacity, scissor);
		}

		public DrawnTextConsumer.Transformation withScissor(ScreenRect scissor) {
			return scissor.equals(this.scissor)
			       ? this
			       : new DrawnTextConsumer.Transformation(pose, opacity, scissor);
		}

		/**
		 * Создаёт трансформацию с областью ножниц, пересечённой с текущей (если она есть).
		 * Координаты задаются в локальном пространстве до применения матрицы.
		 */
		public DrawnTextConsumer.Transformation withScissor(int left, int right, int top, int bottom) {
			ScreenRect newScissor = new ScreenRect(left, top, right - left, bottom - top).transform(pose);

			if (this.scissor != null) {
				newScissor = Objects.requireNonNullElse(this.scissor.intersection(newScissor), ScreenRect.empty());
			}

			return withScissor(newScissor);
		}
	}
}
