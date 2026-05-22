package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import org.jspecify.annotations.Nullable;

/**
 * Невидимый глиф с заданным горизонтальным отступом (advance).
 * Используется для пробелов и символов без визуального представления
 * (например, в {@link SpaceFont}).
 */
@Environment(EnvType.CLIENT)
public class EmptyGlyph implements Glyph {

	final GlyphMetrics glyph;

	public EmptyGlyph(float advance) {
		glyph = GlyphMetrics.empty(advance);
	}

	@Override
	public GlyphMetrics getMetrics() {
		return glyph;
	}

	@Override
	public BakedGlyph bake(Glyph.AbstractGlyphBaker baker) {
		return new BakedGlyph() {
			@Override
			public GlyphMetrics getMetrics() {
				return EmptyGlyph.this.glyph;
			}

			@Override
			public TextDrawable.@Nullable DrawnGlyphRect create(
					float x,
					float y,
					int color,
					int shadowColor,
					Style style,
					float boldOffset,
					float shadowOffset
			) {
				return null;
			}
		};
	}
}
