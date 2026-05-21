package net.minecraft.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
/**
 * {@code BakedGlyphImpl}.
 */
public class BakedGlyphImpl implements BakedGlyph, EffectGlyph {

	public static final float Z_OFFSET = 0.001F;
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

	float getEffectiveMinX(BakedGlyphImpl.BakedGlyphRect glyph) {
		return glyph.x
				+ this.minX
				+ (glyph.style.isItalic() ? Math.min(this.getItalicOffsetAtMinY(), this.getItalicOffsetAtMaxY()) : 0.0F)
				- getXExpansion(glyph.style.isBold());
	}

	float getEffectiveMinY(BakedGlyphImpl.BakedGlyphRect glyph) {
		return glyph.y + this.minY - getXExpansion(glyph.style.isBold());
	}

	float getEffectiveMaxX(BakedGlyphImpl.BakedGlyphRect glyph) {
		return glyph.x
				+ this.maxX
				+ (glyph.hasShadow() ? glyph.shadowOffset : 0.0F)
				+ (glyph.style.isItalic() ? Math.max(this.getItalicOffsetAtMinY(), this.getItalicOffsetAtMaxY()) : 0.0F)
				+ getXExpansion(glyph.style.isBold());
	}

	float getEffectiveMaxY(BakedGlyphImpl.BakedGlyphRect glyph) {
		return glyph.y + this.maxY + (glyph.hasShadow() ? glyph.shadowOffset : 0.0F)
				+ getXExpansion(glyph.style.isBold());
	}

	void draw(
			BakedGlyphImpl.BakedGlyphRect glyph,
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int light,
			boolean fixedZ
	) {
		Style style = glyph.style();
		boolean bl = style.isItalic();
		float f = glyph.x();
		float g = glyph.y();
		int i = glyph.color();
		boolean bl2 = style.isBold();
		float h = fixedZ ? 0.0F : 0.001F;
		float k;
		if (glyph.hasShadow()) {
			int j = glyph.shadowColor();
			this.draw(
					bl,
					f + glyph.shadowOffset(),
					g + glyph.shadowOffset(),
					0.0F,
					matrix,
					vertexConsumer,
					j,
					bl2,
					light
			);
			if (bl2) {
				this.draw(
						bl,
						f + glyph.boldOffset() + glyph.shadowOffset(),
						g + glyph.shadowOffset(),
						h,
						matrix,
						vertexConsumer,
						j,
						true,
						light
				);
			}

			k = fixedZ ? 0.0F : 0.03F;
		}
		else {
			k = 0.0F;
		}

		this.draw(bl, f, g, k, matrix, vertexConsumer, i, bl2, light);
		if (bl2) {
			this.draw(bl, f + glyph.boldOffset(), g, k + h, matrix, vertexConsumer, i, true, light);
		}
	}

	private void draw(
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
		float f = x + this.minX;
		float g = x + this.maxX;
		float h = y + this.minY;
		float i = y + this.maxY;
		float j = italic ? this.getItalicOffsetAtMinY() : 0.0F;
		float k = italic ? this.getItalicOffsetAtMaxY() : 0.0F;
		float l = getXExpansion(bold);
		vertexConsumer.vertex(matrix, f + j - l, h - l, z).color(color).texture(this.minU, this.minV).light(light);
		vertexConsumer.vertex(matrix, f + k - l, i + l, z).color(color).texture(this.minU, this.maxV).light(light);
		vertexConsumer.vertex(matrix, g + k + l, i + l, z).color(color).texture(this.maxU, this.maxV).light(light);
		vertexConsumer.vertex(matrix, g + j + l, h - l, z).color(color).texture(this.maxU, this.minV).light(light);
	}

	private static float getXExpansion(boolean bold) {
		return bold ? 0.1F : 0.0F;
	}

	private float getItalicOffsetAtMaxY() {
		return 1.0F - 0.25F * this.maxY;
	}

	private float getItalicOffsetAtMinY() {
		return 1.0F - 0.25F * this.minY;
	}

	void drawRectangle(
			BakedGlyphImpl.Rectangle rectangle,
			Matrix4f matrix,
			VertexConsumer vertexConsumer,
			int light,
			boolean fixedZ
	) {
		float f = fixedZ ? 0.0F : rectangle.zIndex;
		if (rectangle.hasShadow()) {
			this.drawRectangle(
					rectangle,
					rectangle.shadowOffset(),
					f,
					rectangle.shadowColor(),
					vertexConsumer,
					light,
					matrix
			);
			f += fixedZ ? 0.0F : 0.03F;
		}

		this.drawRectangle(rectangle, 0.0F, f, rectangle.color, vertexConsumer, light, matrix);
	}

	private void drawRectangle(
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
		              .texture(this.minU, this.minV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.maxY + shadowOffset, zOffset)
		              .color(color)
		              .texture(this.minU, this.maxV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.minY + shadowOffset, zOffset)
		              .color(color)
		              .texture(this.maxU, this.maxV)
		              .light(light);
		vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.minY + shadowOffset, zOffset)
		              .color(color)
		              .texture(this.maxU, this.minV)
		              .light(light);
	}

	@Override
	public GlyphMetrics getMetrics() {
		return this.glyph;
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BakedGlyphRect}.
	 */
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
			return this.glyph.getEffectiveMinX(this);
		}

		@Override
		public float getEffectiveMinY() {
			return this.glyph.getEffectiveMinY(this);
		}

		@Override
		public float getEffectiveMaxX() {
			return this.glyph.getEffectiveMaxX(this);
		}

		@Override
		public float getRight() {
			return this.x + this.glyph.glyph.getAdvance(this.style.isBold());
		}

		@Override
		public float getEffectiveMaxY() {
			return this.glyph.getEffectiveMaxY(this);
		}

		boolean hasShadow() {
			return this.shadowColor() != 0;
		}

		@Override
		public void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
			this.glyph.draw(this, matrix4f, consumer, light, noDepth);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return this.glyph.textRenderLayers.getRenderLayer(type);
		}

		@Override
		public GpuTextureView textureView() {
			return this.glyph.textureView;
		}

		@Override
		public RenderPipeline getPipeline() {
			return this.glyph.textRenderLayers.guiPipeline();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Rectangle}.
	 */
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
			return this.minX;
		}

		@Override
		public float getEffectiveMinY() {
			return this.minY;
		}

		@Override
		public float getEffectiveMaxX() {
			return this.maxX + (this.hasShadow() ? this.shadowOffset : 0.0F);
		}

		@Override
		public float getEffectiveMaxY() {
			return this.maxY + (this.hasShadow() ? this.shadowOffset : 0.0F);
		}

		boolean hasShadow() {
			return this.shadowColor() != 0;
		}

		@Override
		public void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
			this.glyph.drawRectangle(this, matrix4f, consumer, light, false);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return this.glyph.textRenderLayers.getRenderLayer(type);
		}

		@Override
		public GpuTextureView textureView() {
			return this.glyph.textureView;
		}

		@Override
		public RenderPipeline getPipeline() {
			return this.glyph.textRenderLayers.guiPipeline();
		}
	}
}
