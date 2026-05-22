package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * Провайдер глифов для конкретного шрифта.
 * Реализации: {@link BitmapFont}, {@link TrueTypeFont}, {@link SpaceFont},
 * {@link UnihexFont}, {@link BlankFont}.
 */
@Environment(EnvType.CLIENT)
public interface Font extends AutoCloseable {

	/** Стандартный подъём (ascent) шрифта в пикселях. */
	float DEFAULT_ASCENT = 7.0F;

	@Override
	default void close() {
	}

	default @Nullable Glyph getGlyph(int codePoint) {
		return null;
	}

	IntSet getProvidedGlyphs();

	/**
	 * Пара «провайдер шрифта + карта фильтров», используемая в {@link FontStorage}
	 * для условного включения/исключения провайдеров по активным фильтрам.
	 */
	@Environment(EnvType.CLIENT)
	public record FontFilterPair(Font provider, FontFilterType.FilterMap filter) implements AutoCloseable {

		@Override
		public void close() {
			provider.close();
		}
	}
}
