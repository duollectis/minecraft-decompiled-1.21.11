package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Незапечённый глиф шрифта. Содержит метрики и логику запекания в {@link BakedGlyph}.
 */
@Environment(EnvType.CLIENT)
public interface Glyph {

	GlyphMetrics getMetrics();

	BakedGlyph bake(Glyph.AbstractGlyphBaker baker);

	/**
	 * Абстрактный пекарь глифов — мост между {@link Glyph} и атласом текстур.
	 * Позволяет запекать глиф в {@link BakedGlyph} без прямой зависимости от {@link GlyphBaker}.
	 */
	@Environment(EnvType.CLIENT)
	interface AbstractGlyphBaker {

		BakedGlyph bake(GlyphMetrics metrics, UploadableGlyph renderable);

		BakedGlyph getBlankGlyph();
	}
}
