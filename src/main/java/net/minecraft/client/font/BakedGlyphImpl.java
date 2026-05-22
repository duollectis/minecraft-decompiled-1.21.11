package net.minecraft.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;

/**
 * Конкретная реализация {@link BakedGlyph}, хранящая UV-координаты в атласе
 * и геометрические границы глифа. Умеет рисовать себя в буфер вершин
 * с учётом курсива, жирности и тени.
 */
@Environment(EnvType.CLIENT)
public class BakedGlyphImpl implements BakedGlyph, EffectGlyph {

	/** Смещение по Z между слоями одного глифа (основной + жирный дубль). */
	public static final float Z_OFFSET = 0.001F;

	/** Смещение тени по Z относительно основного слоя. */
	private static final float SHADOW_Z_STEP = 0.03F;

	/** Коэффициент наклона для курсива: 1 пиксель на каждые 4 пикселя высоты. */
	private static final float ITALIC_SLOPE = 0.25F;

	/** Расширение границ глифа при жирном начертании. */
	private static final float BOLD_EXPANSION = 0.1F;

	final GlyphMetrics glyph;
	final TextRenderLayerSet textRenderLayers;
	final GpuTextureView textureView;
	private final float minU;
	private final float maxU;
	private final float minV;
	private final float maxV;
	private final float minX;
	private final float maxX;
	private final float minY;
	private final float maxY;

	public BakedGlyphImpl(
			GlyphMetrics glyph,
			TextRenderLayerSet textRenderLayers,
			GpuTextureView textureView,
			float minU,
			float maxU,
			float minV,
			float maxV,
			float minX,
			float maxX,
			float minY,
			float maxY
	) {
		this.glyph = glyph;
		this.textRenderLayers = textRenderLayers;
		this.textureView = textureView;
		this.minU = minU;
		this.maxU = maxU;
		this.minV = minV;
		this.maxV = maxV;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	float getEffectiveMinX(BakedGlyphImpl.BakedGlyphRect rect) {
		return rect.x
				+ minX
				+ (rect.style.isItalic() ? Math.min(getItalicOffsetAtMinY(), getItalicOffsetAtMaxY()) : 0.0F)
				- getXExpansion(rect.style.isBold());
	}

	float getEffectiveMinY(BakedGlyphImpl.BakedGlyphRect rect) {
		return rect.y + minY - getXExpansion(rect.style.isBold());
	}

	float getEffectiveMaxX(BakedGlyphImpl.BakedGlyphRect rect) {
		return rect.x
				+ maxX
				+ (rect.hasShadow() ? rect.shadowOffset : 0.0F)
				+ (rect.style.isItalic() ? Math.max(getItalicOffsetAtMinY(), getItalicOffsetAtMaxY()) : 0.0F)
				+ getXExpansion(rect.style.isBold());
	}

	float getEffectiveMaxY(BakedGlyphImpl.BakedGlyphRect rect) {
		return rect.y + maxY + (rect.hasShadow() ? rect.shadowOffset : 0.0F)
				+ getXExpansion(rect.style.isBold());
	}

	/**
	 * Рисует глиф (с тенью и жирным дублем, если нужно) в буфер вершин.
	 * Порядок: сначала тень, затем основной слой — чтобы тень была позади.
	 */
	void draw(
			BakedGlyphImpl.BakedGlyphRect rect,
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int light,
			boolean fixedZ
	) {
		Style style = rect.style();
		boolean italic = style.isItalic();
		float x = rect.x();
		float y = rect.y();
		int color = rect.color();
		boolean bold = style.isBold();
		float zStep = fixedZ ? 0.0F : Z_OFFSET;
		float shadowZ;

		if (rect.hasShadow()) {
			int shadowColor = rect.shadowColor();
			drawLayer(italic, x + rect.shadowOffset(), y + rect.shadowOffset(), 0.0F, matrix, vertexConsumer, shadowColor, bold, light);

			if (bold) {
				drawLayer(italic, x + rect.boldOffset() + rect.shadowOffset(), y + rect.shadowOffset(), zStep, matrix, vertexConsumer, shadowColor, true, light);
			}

			shadowZ = fixedZ ? 0.0F : SHADOW_Z_STEP;
		} else {
			shadowZ = 0.0F;
		}

		drawLayer(italic, x, y, shadowZ, matrix, vertexConsumer, color, bold, light);

		if (bold) {
			drawLayer(italic, x + rect.boldOffset(), y, shadowZ + zStep, matrix, vertexConsumer, color, true, light);
		}
	}

	private void drawLayer(
			boolean italic,
			float x,
			float y,
			float z,
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int color,
			boolean bold,
			int light
	) {
		float left = x + minX;
		float right = x + maxX;
		float top = y + minY;
		float bottom = y + maxY;
		float italicTopOffset = italic ? getItalicOffsetAtMinY() : 0.0F;
		float italicBottomOffset = italic ? getItalicOffsetAtMaxY() : 0.0F;
		float expansion = getXExpansion(bold);

		vertexConsumer.vertex(matrix, left + italicTopOffset - expansion, top - expansion, z).color(color).texture(minU, minV).light(light);
		vertexConsumer.vertex(matrix, left + italicBottomOffset - expansion, bottom + expansion, z).color(color).texture(minU, maxV).light(light);
		vertexConsumer.vertex(matrix, right + italicBottomOffset + expansion, bottom + expansion, z).color(color).texture(maxU, maxV).light(light);
		vertexConsumer.vertex(matrix, right + italicTopOffset + expansion, top - expansion, z).color(color).texture(maxU, minV).light(light);
	}

	private static float getXExpansion(boolean bold) {
		return bold ? BOLD_EXPANSION : 0.0F;
	}

	private float getItalicOffsetAtMaxY() {
		return 1.0F - ITALIC_SLOPE * maxY;
	}

	private float getItalicOffsetAtMinY() {
		return 1.0F - ITALIC_SLOPE * minY;
	}

	void drawRectangle(
			BakedGlyphImpl.Rectangle rectangle,
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int light,
			boolean fixedZ
	) {
		float z = fixedZ ? 0.0F : rectangle.zIndex;

		if (rectangle.hasShadow()) {
			drawRectangleLayer(rectangle, rectangle.shadowOffset(), z, rectangle.shadowColor(), vertexConsumer, light, matrix);
			z += fixedZ ? 0.0F : SHADOW_Z_STEP;
		}

		drawRectangleLayer(rectangle, 0.0F, z, rectangle.color, vertexConsumer, light, matrix);
	}

	private void drawRectangleLayer(
			BakedGlyphImpl.Rectangle rectangle,
			float shadowOffset,
			float zOffset,
			int color,
			VertexConsumer vertexConsumer,
			int light,
			Matrix4f matrix
	) {
		vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.maxY + shadowOffset, zOffset)
		              .color(color)
		              .texture(minU, minV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.maxY + shadowOffset, zOffset)
		              .color(color)
		              .texture(minU, maxV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.minY + shadowOffset, zOffset)
		              .color(color)
		              .texture(maxU, maxV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.minY + shadowOffset, zOffset)
		              .color(color)
		              .texture(maxU, minV)
		              .light(light);
	}

	@Override
	public GlyphMetrics getMetrics() {
		return glyph;
	}

	@Override
	public TextDrawable.DrawnGlyphRect create(
			float x,
			float y,
			int color,
			int shadowColor,
			Style style,
			float boldOffset,
			float shadowOffset
	) {
		return new BakedGlyphImpl.BakedGlyphRect(x, y, color, shadowColor, this, style, boldOffset, shadowOffset);
	}

	@Override
	public TextDrawable create(
			float minX,
			float minY,
			float maxX,
			float maxY,
			float depth,
			int color,
			int shadowColor,
			float shadowOffset
	) {
		return new BakedGlyphImpl.Rectangle(this, minX, minY, maxX, maxY, depth, color, shadowColor, shadowOffset);
	}

	/**
	 * Запечённый прямоугольник глифа — хранит позицию, цвет и ссылку на родительский
	 * {@link BakedGlyphImpl} для делегирования рендеринга.
	 */
	@Environment(EnvType.CLIENT)
	record BakedGlyphRect(
			float x,
			float y,
			int color,
			int shadowColor,
			BakedGlyphImpl glyph,
			Style style,
			float boldOffset,
			float shadowOffset
	)
			implements TextDrawable.DrawnGlyphRect {

		@Override
		public float getEffectiveMinX() {
			return glyph.getEffectiveMinX(this);
		}

		@Override
		public float getEffectiveMinY() {
			return glyph.getEffectiveMinY(this);
		}

		@Override
		public float getEffectiveMaxX() {
			return glyph.getEffectiveMaxX(this);
		}

		@Override
		public float getRight() {
			return x + glyph.glyph.getAdvance(style.isBold());
		}

		@Override
		public float getEffectiveMaxY() {
			return glyph.getEffectiveMaxY(this);
		}

		boolean hasShadow() {
			return shadowColor() != 0;
		}

		@Override
		public void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
			glyph.draw(this, matrix4f, consumer, light, noDepth);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return glyph.textRenderLayers.getRenderLayer(type);
		}

		@Override
		public GpuTextureView textureView() {
			return glyph.textureView;
		}

		@Override
		public RenderPipeline getPipeline() {
			return glyph.textRenderLayers.guiPipeline();
		}
	}

	/**
	 * Прямоугольный эффект (подчёркивание, зачёркивание, фон) — рисуется
	 * через тот же атлас, что и глиф, используя белый пиксель.
	 */
	@Environment(EnvType.CLIENT)
	record Rectangle(
			BakedGlyphImpl glyph,
			float minX,
			float minY,
			float maxX,
			float maxY,
			float zIndex,
			int color,
			int shadowColor,
			float shadowOffset
	)
			implements TextDrawable {

		@Override
		public float getEffectiveMinX() {
			return minX;
		}

		@Override
		public float getEffectiveMinY() {
			return minY;
		}

		@Override
		public float getEffectiveMaxX() {
			return maxX + (hasShadow() ? shadowOffset : 0.0F);
		}

		@Override
		public float getEffectiveMaxY() {
			return maxY + (hasShadow() ? shadowOffset : 0.0F);
		}

		boolean hasShadow() {
			return shadowColor() != 0;
		}

		@Override
		public void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
			glyph.drawRectangle(this, matrix4f, consumer, light, false);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return glyph.textRenderLayers.getRenderLayer(type);
		}

		@Override
		public GpuTextureView textureView() {
			return glyph.textureView;
		}

		@Override
		public RenderPipeline getPipeline() {
			return glyph.textRenderLayers.guiPipeline();
		}
	}
}
