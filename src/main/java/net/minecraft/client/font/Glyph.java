package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code Glyph}.
 */
public interface Glyph {

	GlyphMetrics getMetrics();

	BakedGlyph bake(Glyph.AbstractGlyphBaker baker);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AbstractGlyphBaker}.
	 */
	public interface AbstractGlyphBaker {

		BakedGlyph bake(GlyphMetrics metrics, UploadableGlyph renderable);

		BakedGlyph getBlankGlyph();
	}
}
