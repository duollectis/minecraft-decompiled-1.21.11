package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * Шрифт-заглушка, который для любого кодового символа возвращает
 * «отсутствующий» глиф. Используется как резервный провайдер
 * в {@link FontManager} для шрифта {@code minecraft:missing}.
 */
@Environment(EnvType.CLIENT)
public class BlankFont implements Font {

	private static final Glyph MISSING = new Glyph() {
		@Override
		public GlyphMetrics getMetrics() {
			return BuiltinEmptyGlyph.MISSING;
		}

		@Override
		public BakedGlyph bake(Glyph.AbstractGlyphBaker baker) {
			return baker.getBlankGlyph();
		}
	};

	@Override
	public @Nullable Glyph getGlyph(int codePoint) {
		return MISSING;
	}

	@Override
	public IntSet getProvidedGlyphs() {
		return IntSets.EMPTY_SET;
	}
}
