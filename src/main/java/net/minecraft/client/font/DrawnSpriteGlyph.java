package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Интерфейс для глифов-спрайтов фиксированного размера 8×8 пикселей
 * (аватары игроков, иконки и т.п.).
 * Реализует стандартный рендеринг с тенью через метод {@link #render}.
 */
@Environment(EnvType.CLIENT)
public interface DrawnSpriteGlyph extends TextDrawable.DrawnGlyphRect {

	float GLYPH_WIDTH = 8.0F;
	float GLYPH_HEIGHT = 8.0F;
	float GLYPH_ADVANCE = 8.0F;

	/** Смещение по Z между слоем тени и основным слоем. */
	float SHADOW_Z_STEP = 0.03F;

	@Override
	default void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
		float zOffset = 0.0F;

		if (shadowColor() != 0) {
			draw(matrix4f, consumer, light, shadowOffset(), shadowOffset(), 0.0F, shadowColor());

			if (!noDepth) {
				zOffset += SHADOW_Z_STEP;
			}
		}

		draw(matrix4f, consumer, light, 0.0F, 0.0F, zOffset, color());
	}

	void draw(Matrix4f matrix, VertexConsumer vertexConsumer, int light, float x, float y, float z, int color);

	float x();

	float y();

	int color();

	int shadowColor();

	float shadowOffset();

	default float getWidth() {
		return GLYPH_WIDTH;
	}

	default float getHeight() {
		return GLYPH_HEIGHT;
	}

	default float getAscent() {
		return GLYPH_HEIGHT;
	}

	@Override
	default float getEffectiveMinX() {
		return x();
	}

	@Override
	default float getEffectiveMaxX() {
		return getEffectiveMinX() + getWidth();
	}

	@Override
	default float getEffectiveMinY() {
		return y() + 7.0F - getAscent();
	}

	@Override
	default float getEffectiveMaxY() {
		return getTop() + getHeight();
	}
}
