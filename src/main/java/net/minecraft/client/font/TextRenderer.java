package net.minecraft.client.font;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.*;
import net.minecraft.util.Language;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Рендерер текста. Преобразует строки и {@link OrderedText} в наборы запечённых глифов
 * и передаёт их в {@link VertexConsumerProvider} для отрисовки.
 */
@Environment(EnvType.CLIENT)
public class TextRenderer {

	private static final float Z_INDEX = 0.01F;
	private static final float SHADOW_OFFSET = 0.01F;
	private static final float SHADOW_OFFSET_NEGATIVE = -0.01F;
	public static final float FORWARD_SHIFT = 0.03F;
	public final int fontHeight = 9;

	private final Random random = Random.create();
	final TextRenderer.GlyphsProvider fonts;
	private final TextHandler handler;

	public TextRenderer(TextRenderer.GlyphsProvider fonts) {
		this.fonts = fonts;
		handler = new TextHandler(
				(codePoint, style) -> fonts
						.getGlyphs(style.getFont())
						.get(codePoint)
						.getMetrics()
						.getAdvance(style.isBold())
		);
	}

	private GlyphProvider getGlyphs(StyleSpriteSource source) {
		return fonts.getGlyphs(source);
	}

	public String mirror(String text) {
		try {
			Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
			bidi.setReorderingMode(0);
			return bidi.writeReordered(2);
		} catch (ArabicShapingException exception) {
			return text;
		}
	}

	public void draw(
			String string,
			float x,
			float y,
			int color,
			boolean shadow,
			Matrix4f matrix,
			VertexConsumerProvider vertexConsumers,
			TextRenderer.TextLayerType layerType,
			int backgroundColor,
			int light
	) {
		TextRenderer.GlyphDrawable drawable = prepare(string, x, y, color, shadow, backgroundColor);
		drawable.draw(TextRenderer.GlyphDrawer.drawing(vertexConsumers, matrix, layerType, light));
	}

	public void draw(
			Text text,
			float x,
			float y,
			int color,
			boolean shadow,
			Matrix4f matrix,
			VertexConsumerProvider vertexConsumers,
			TextRenderer.TextLayerType layerType,
			int backgroundColor,
			int light
	) {
		TextRenderer.GlyphDrawable drawable = prepare(text.asOrderedText(), x, y, color, shadow, false, backgroundColor);
		drawable.draw(TextRenderer.GlyphDrawer.drawing(vertexConsumers, matrix, layerType, light));
	}

	public void draw(
			OrderedText text,
			float x,
			float y,
			int color,
			boolean shadow,
			Matrix4f matrix,
			VertexConsumerProvider vertexConsumers,
			TextRenderer.TextLayerType layerType,
			int backgroundColor,
			int light
	) {
		TextRenderer.GlyphDrawable drawable = prepare(text, x, y, color, shadow, false, backgroundColor);
		drawable.draw(TextRenderer.GlyphDrawer.drawing(vertexConsumers, matrix, layerType, light));
	}

	/**
	 * Рисует текст с цветным контуром (outline). Каждый символ рисуется 8 раз со смещением
	 * в цвете контура, затем поверх — основной текст с {@link TextLayerType#POLYGON_OFFSET}.
	 */
	public void drawWithOutline(
			OrderedText text,
			float x,
			float y,
			int color,
			int outlineColor,
			Matrix4f matrix,
			VertexConsumerProvider vertexConsumers,
			int light
	) {
		TextRenderer.Drawer outlineDrawer = new TextRenderer.Drawer(0.0F, 0.0F, outlineColor, false, false);

		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetY = -1; offsetY <= 1; offsetY++) {
				if (offsetX == 0 && offsetY == 0) {
					continue;
				}

				float[] cursorX = new float[]{x};
				int dx = offsetX;
				int dy = offsetY;
				text.accept((index, style, codePoint) -> {
					boolean bold = style.isBold();
					BakedGlyph glyph = getGlyph(codePoint, style);
					outlineDrawer.x = cursorX[0] + dx * glyph.getMetrics().getShadowOffset();
					outlineDrawer.y = y + dy * glyph.getMetrics().getShadowOffset();
					cursorX[0] += glyph.getMetrics().getAdvance(bold);
					return outlineDrawer.accept(index, style.withColor(outlineColor), glyph);
				});
			}
		}

		TextRenderer.GlyphDrawer outlineGlyphDrawer =
				TextRenderer.GlyphDrawer.drawing(vertexConsumers, matrix, TextRenderer.TextLayerType.NORMAL, light);

		for (TextDrawable.DrawnGlyphRect drawnGlyph : outlineDrawer.drawnGlyphs) {
			outlineGlyphDrawer.drawGlyph(drawnGlyph);
		}

		TextRenderer.Drawer mainDrawer = new TextRenderer.Drawer(x, y, color, false, true);
		text.accept(mainDrawer);
		mainDrawer.draw(TextRenderer.GlyphDrawer.drawing(
				vertexConsumers,
				matrix,
				TextRenderer.TextLayerType.POLYGON_OFFSET,
				light
		));
	}

	BakedGlyph getGlyph(int codePoint, Style style) {
		GlyphProvider glyphProvider = getGlyphs(style.getFont());
		BakedGlyph glyph = glyphProvider.get(codePoint);
		if (style.isObfuscated() && codePoint != 32) {
			int width = MathHelper.ceil(glyph.getMetrics().getAdvance(false));
			glyph = glyphProvider.getObfuscated(random, width);
		}

		return glyph;
	}

	public TextRenderer.GlyphDrawable prepare(
			String string,
			float x,
			float y,
			int color,
			boolean shadow,
			int backgroundColor
	) {
		if (isRightToLeft()) {
			string = mirror(string);
		}

		TextRenderer.Drawer drawer = new TextRenderer.Drawer(x, y, color, backgroundColor, shadow, false);
		TextVisitFactory.visitFormatted(string, Style.EMPTY, drawer);
		return drawer;
	}

	public TextRenderer.GlyphDrawable prepare(
			OrderedText text,
			float x,
			float y,
			int color,
			boolean shadow,
			boolean trackEmpty,
			int backgroundColor
	) {
		TextRenderer.Drawer drawer = new TextRenderer.Drawer(x, y, color, backgroundColor, shadow, trackEmpty);
		text.accept(drawer);
		return drawer;
	}

	public int getWidth(String text) {
		return MathHelper.ceil(handler.getWidth(text));
	}

	public int getWidth(StringVisitable text) {
		return MathHelper.ceil(handler.getWidth(text));
	}

	public int getWidth(OrderedText text) {
		return MathHelper.ceil(handler.getWidth(text));
	}

	public String trimToWidth(String text, int maxWidth, boolean backwards) {
		return backwards
				? handler.trimToWidthBackwards(text, maxWidth, Style.EMPTY)
				: handler.trimToWidth(text, maxWidth, Style.EMPTY);
	}

	public String trimToWidth(String text, int maxWidth) {
		return handler.trimToWidth(text, maxWidth, Style.EMPTY);
	}

	public StringVisitable trimToWidth(StringVisitable text, int width) {
		return handler.trimToWidth(text, width, Style.EMPTY);
	}

	public int getWrappedLinesHeight(StringVisitable text, int maxWidth) {
		return 9 * handler.wrapLines(text, maxWidth, Style.EMPTY).size();
	}

	public List<OrderedText> wrapLines(StringVisitable text, int width) {
		return Language.getInstance().reorder(handler.wrapLines(text, width, Style.EMPTY));
	}

	public List<StringVisitable> wrapLinesWithoutLanguage(StringVisitable text, int width) {
		return handler.wrapLines(text, width, Style.EMPTY);
	}

	public boolean isRightToLeft() {
		return Language.getInstance().isRightToLeft();
	}

	public TextHandler getTextHandler() {
		return handler;
	}

	@Environment(EnvType.CLIENT)
	class Drawer implements CharacterVisitor, TextRenderer.GlyphDrawable {

		private final boolean shadow;
		private final int color;
		private final int backgroundColor;
		private final boolean trackEmpty;
		float x;
		float y;
		private float minX = Float.MAX_VALUE;
		private float minY = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE;
		private float maxY = -Float.MAX_VALUE;
		private float minBackgroundX = Float.MAX_VALUE;
		private float minBackgroundY = Float.MAX_VALUE;
		private float maxBackgroundX = -Float.MAX_VALUE;
		private float maxBackgroundY = -Float.MAX_VALUE;
		final List<TextDrawable.DrawnGlyphRect> drawnGlyphs = new ArrayList<>();
		private @Nullable List<TextDrawable> rectangles;
		private @Nullable List<EmptyGlyphRect> emptyGlyphRects;

		public Drawer(final float x, final float y, final int color, final boolean shadow, final boolean trackEmpty) {
			this(x, y, color, 0, shadow, trackEmpty);
		}

		public Drawer(
				final float x,
				final float y,
				final int color,
				final int backgroundColor,
				final boolean shadow,
				final boolean trackEmpty
		) {
			this.x = x;
			this.y = y;
			this.shadow = shadow;
			this.color = color;
			this.backgroundColor = backgroundColor;
			this.trackEmpty = trackEmpty;
			updateBackgroundBounds(x, y, 0.0F);
		}

		private void updateTextBounds(float newMinX, float newMinY, float newMaxX, float newMaxY) {
			minX = Math.min(minX, newMinX);
			minY = Math.min(minY, newMinY);
			maxX = Math.max(maxX, newMaxX);
			maxY = Math.max(maxY, newMaxY);
		}

		private void updateBackgroundBounds(float cursorX, float cursorY, float width) {
			if (ColorHelper.getAlpha(backgroundColor) == 0) {
				return;
			}

			minBackgroundX = Math.min(minBackgroundX, cursorX - 1.0F);
			minBackgroundY = Math.min(minBackgroundY, cursorY - 1.0F);
			maxBackgroundX = Math.max(maxBackgroundX, cursorX + width);
			maxBackgroundY = Math.max(maxBackgroundY, cursorY + 9.0F);
			updateTextBounds(minBackgroundX, minBackgroundY, maxBackgroundX, maxBackgroundY);
		}

		private void addGlyph(TextDrawable.DrawnGlyphRect glyph) {
			drawnGlyphs.add(glyph);
			updateTextBounds(
					glyph.getEffectiveMinX(),
					glyph.getEffectiveMinY(),
					glyph.getEffectiveMaxX(),
					glyph.getEffectiveMaxY()
			);
		}

		private void addRectangle(TextDrawable rectangle) {
			if (rectangles == null) {
				rectangles = new ArrayList<>();
			}

			rectangles.add(rectangle);
			updateTextBounds(
					rectangle.getEffectiveMinX(),
					rectangle.getEffectiveMinY(),
					rectangle.getEffectiveMaxX(),
					rectangle.getEffectiveMaxY()
			);
		}

		private void addEmptyGlyphRect(EmptyGlyphRect rect) {
			if (emptyGlyphRects == null) {
				emptyGlyphRects = new ArrayList<>();
			}

			emptyGlyphRects.add(rect);
		}

		@Override
		public boolean accept(int index, Style style, int codePoint) {
			BakedGlyph glyph = TextRenderer.this.getGlyph(codePoint, style);
			return accept(index, style, glyph);
		}

		public boolean accept(int index, Style style, BakedGlyph glyph) {
			GlyphMetrics metrics = glyph.getMetrics();
			boolean bold = style.isBold();
			TextColor textColor = style.getColor();
			int renderColor = getRenderColor(textColor);
			int shadowColor = getShadowColor(style, renderColor);
			float advance = metrics.getAdvance(bold);
			float glyphStartX = index == 0 ? x - 1.0F : x;
			float shadowOffset = metrics.getShadowOffset();
			float boldOffset = bold ? metrics.getBoldOffset() : 0.0F;
			TextDrawable.DrawnGlyphRect drawnGlyph = glyph.create(x, y, renderColor, shadowColor, style, boldOffset, shadowOffset);

			if (drawnGlyph != null) {
				addGlyph(drawnGlyph);
			} else if (trackEmpty) {
				addEmptyGlyphRect(new EmptyGlyphRect(x, y, advance, 7.0F, 9.0F, style));
			}

			updateBackgroundBounds(x, y, advance);

			if (style.isStrikethrough()) {
				addRectangle(TextRenderer.this.fonts
						.getRectangleGlyph()
						.create(glyphStartX, y + 4.5F - 1.0F, x + advance, y + 4.5F, Z_INDEX, renderColor, shadowColor, shadowOffset));
			}

			if (style.isUnderlined()) {
				addRectangle(TextRenderer.this.fonts
						.getRectangleGlyph()
						.create(glyphStartX, y + 9.0F - 1.0F, x + advance, y + 9.0F, Z_INDEX, renderColor, shadowColor, shadowOffset));
			}

			x += advance;
			return true;
		}

		@Override
		public void draw(TextRenderer.GlyphDrawer glyphDrawer) {
			if (ColorHelper.getAlpha(backgroundColor) != 0) {
				glyphDrawer.drawRectangle(
						TextRenderer.this.fonts
								.getRectangleGlyph()
								.create(
										minBackgroundX,
										minBackgroundY,
										maxBackgroundX,
										maxBackgroundY,
										SHADOW_OFFSET_NEGATIVE,
										backgroundColor,
										0,
										0.0F
								)
				);
			}

			for (TextDrawable.DrawnGlyphRect drawnGlyph : drawnGlyphs) {
				glyphDrawer.drawGlyph(drawnGlyph);
			}

			if (rectangles != null) {
				for (TextDrawable rectangle : rectangles) {
					glyphDrawer.drawRectangle(rectangle);
				}
			}

			if (emptyGlyphRects != null) {
				for (EmptyGlyphRect emptyRect : emptyGlyphRects) {
					glyphDrawer.drawEmptyGlyphRect(emptyRect);
				}
			}
		}

		private int getRenderColor(@Nullable TextColor override) {
			if (override == null) {
				return color;
			}

			int alpha = ColorHelper.getAlpha(color);
			int rgb = override.getRgb();
			return ColorHelper.withAlpha(alpha, rgb);
		}

		private int getShadowColor(Style style, int textColor) {
			Integer customShadow = style.getShadowColor();
			if (customShadow != null) {
				float textAlpha = ColorHelper.getAlphaFloat(textColor);
				float shadowAlpha = ColorHelper.getAlphaFloat(customShadow);
				return textAlpha != 1.0F
						? ColorHelper.withAlpha(ColorHelper.channelFromFloat(textAlpha * shadowAlpha), customShadow)
						: customShadow;
			}

			return shadow ? ColorHelper.scaleRgb(textColor, 0.25F) : 0;
		}

		@Override
		public @Nullable ScreenRect getScreenRect() {
			if (minX >= maxX || minY >= maxY) {
				return null;
			}

			int left = MathHelper.floor(minX);
			int top = MathHelper.floor(minY);
			int right = MathHelper.ceil(maxX);
			int bottom = MathHelper.ceil(maxY);
			return new ScreenRect(left, top, right - left, bottom - top);
		}
	}

	@Environment(EnvType.CLIENT)
	public interface GlyphDrawable {

		void draw(TextRenderer.GlyphDrawer glyphDrawer);

		@Nullable ScreenRect getScreenRect();
	}

	@Environment(EnvType.CLIENT)
	public interface GlyphDrawer {

		static TextRenderer.GlyphDrawer drawing(
				VertexConsumerProvider vertexConsumers,
				Matrix4f matrix,
				TextRenderer.TextLayerType layerType,
				int light
		) {
			return new TextRenderer.GlyphDrawer() {
				@Override
				public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
					draw(glyph);
				}

				@Override
				public void drawRectangle(TextDrawable rect) {
					draw(rect);
				}

				private void draw(TextDrawable glyph) {
					VertexConsumer vertexConsumer = vertexConsumers.getBuffer(glyph.getRenderLayer(layerType));
					glyph.render(matrix, vertexConsumer, light, false);
				}
			};
		}

		default void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
		}

		default void drawRectangle(TextDrawable rect) {
		}

		default void drawEmptyGlyphRect(EmptyGlyphRect rect) {
		}
	}

	@Environment(EnvType.CLIENT)
	public interface GlyphsProvider {

		GlyphProvider getGlyphs(StyleSpriteSource source);

		EffectGlyph getRectangleGlyph();
	}

	@Environment(EnvType.CLIENT)
	public enum TextLayerType {
		NORMAL,
		SEE_THROUGH,
		POLYGON_OFFSET;
	}
}
