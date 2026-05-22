package net.minecraft.client.font;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Хранилище запечённых глифов для одного шрифта. Управляет кешем {@link BakedGlyph},
 * применяет фильтры шрифтов и предоставляет провайдеры глифов для рендеринга текста.
 */
@Environment(EnvType.CLIENT)
public class FontStorage implements AutoCloseable {

	private static final float MAX_ADVANCE = 32.0F;
	private static final BakedGlyph MISSING_GLYPH = new BakedGlyph() {
		@Override
		public GlyphMetrics getMetrics() {
			return BuiltinEmptyGlyph.MISSING;
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

	final GlyphBaker glyphBaker;
	final Glyph.AbstractGlyphBaker abstractBaker = new Glyph.AbstractGlyphBaker() {
		@Override
		public BakedGlyph bake(GlyphMetrics metrics, UploadableGlyph renderable) {
			return Objects.requireNonNullElse(
					FontStorage.this.glyphBaker.bake(metrics, renderable),
					FontStorage.this.blankBakedGlyph
			);
		}

		@Override
		public BakedGlyph getBlankGlyph() {
			return FontStorage.this.blankBakedGlyph;
		}
	};

	private List<Font.FontFilterPair> allFonts = List.of();
	private List<Font> availableFonts = List.of();
	private final Int2ObjectMap<IntList> charactersByWidth = new Int2ObjectOpenHashMap();
	private final GlyphContainer<FontStorage.GlyphPair> bakedGlyphCache =
			new GlyphContainer<>(FontStorage.GlyphPair[]::new, FontStorage.GlyphPair[][]::new);
	private final IntFunction<FontStorage.GlyphPair> findGlyph = this::findGlyph;
	BakedGlyph blankBakedGlyph = MISSING_GLYPH;
	private final Supplier<BakedGlyph> blankGlyphSupplier = () -> blankBakedGlyph;
	private final FontStorage.GlyphPair blankBakedGlyphPair =
			new FontStorage.GlyphPair(blankGlyphSupplier, blankGlyphSupplier);
	private @Nullable EffectGlyph whiteRectangleBakedGlyph;
	private final GlyphProvider anyGlyphs = new FontStorage.Glyphs(false);
	private final GlyphProvider advanceValidatingGlyphs = new FontStorage.Glyphs(true);

	public FontStorage(GlyphBaker baker) {
		glyphBaker = baker;
	}

	public void setFonts(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
		this.allFonts = allFonts;
		setActiveFilters(activeFilters);
	}

	public void setActiveFilters(Set<FontFilterType> activeFilters) {
		availableFonts = List.of();
		clear();
		availableFonts = applyFilters(allFonts, activeFilters);
	}

	private void clear() {
		glyphBaker.clear();
		bakedGlyphCache.clear();
		charactersByWidth.clear();
		blankBakedGlyph = Objects.requireNonNull(BuiltinEmptyGlyph.MISSING.bake(glyphBaker));
		whiteRectangleBakedGlyph = BuiltinEmptyGlyph.WHITE.bake(glyphBaker);
	}

	private List<Font> applyFilters(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
		IntSet codePoints = new IntOpenHashSet();
		List<Font> allowed = new ArrayList<>();

		for (Font.FontFilterPair pair : allFonts) {
			if (pair.filter().isAllowed(activeFilters)) {
				allowed.add(pair.provider());
				codePoints.addAll(pair.provider().getProvidedGlyphs());
			}
		}

		Set<Font> usedFonts = Sets.newHashSet();
		codePoints.forEach(codePoint -> {
			for (Font font : allowed) {
				Glyph glyph = font.getGlyph(codePoint);
				if (glyph == null) {
					continue;
				}

				usedFonts.add(font);
				if (glyph.getMetrics() != BuiltinEmptyGlyph.MISSING) {
					charactersByWidth.computeIfAbsent(
							MathHelper.ceil(glyph.getMetrics().getAdvance(false)),
							width -> new IntArrayList()
					).add(codePoint);
				}

				break;
			}
		});

		return allowed.stream().filter(usedFonts::contains).toList();
	}

	@Override
	public void close() {
		glyphBaker.close();
	}

	private static boolean isAdvanceInvalid(GlyphMetrics glyph) {
		float advance = glyph.getAdvance(false);
		if (advance < 0.0F || advance > MAX_ADVANCE) {
			return true;
		}

		float boldAdvance = glyph.getAdvance(true);
		return boldAdvance < 0.0F || boldAdvance > MAX_ADVANCE;
	}

	private FontStorage.GlyphPair findGlyph(int codePoint) {
		FontStorage.LazyBakedGlyph firstFound = null;

		for (Font font : availableFonts) {
			Glyph glyph = font.getGlyph(codePoint);
			if (glyph == null) {
				continue;
			}

			if (firstFound == null) {
				firstFound = new FontStorage.LazyBakedGlyph(glyph);
			}

			if (isAdvanceInvalid(glyph.getMetrics())) {
				continue;
			}

			return firstFound.glyph == glyph
					? new FontStorage.GlyphPair(firstFound, firstFound)
					: new FontStorage.GlyphPair(firstFound, new FontStorage.LazyBakedGlyph(glyph));
		}

		return firstFound != null
				? new FontStorage.GlyphPair(firstFound, blankGlyphSupplier)
				: blankBakedGlyphPair;
	}

	FontStorage.GlyphPair getBaked(int codePoint) {
		return bakedGlyphCache.computeIfAbsent(codePoint, findGlyph);
	}

	public BakedGlyph getObfuscatedBakedGlyph(Random random, int width) {
		IntList candidates = charactersByWidth.get(width);
		return candidates != null && !candidates.isEmpty()
				? getBaked(candidates.getInt(random.nextInt(candidates.size()))).advanceValidating().get()
				: blankBakedGlyph;
	}

	public EffectGlyph getRectangleBakedGlyph() {
		return Objects.requireNonNull(whiteRectangleBakedGlyph);
	}

	public GlyphProvider getGlyphs(boolean advanceValidating) {
		return advanceValidating ? advanceValidatingGlyphs : anyGlyphs;
	}

	/**
	 * Пара запечённых глифов: один без ограничения advance, второй — с валидацией advance.
	 * Используется для поддержки режима обфускации текста.
	 */
	@Environment(EnvType.CLIENT)
	record GlyphPair(Supplier<BakedGlyph> any, Supplier<BakedGlyph> advanceValidating) {

		Supplier<BakedGlyph> get(boolean advanceValidating) {
			return advanceValidating ? this.advanceValidating : any;
		}
	}

	@Environment(EnvType.CLIENT)
	public class Glyphs implements GlyphProvider {

		private final boolean advanceValidating;

		public Glyphs(final boolean advanceValidating) {
			this.advanceValidating = advanceValidating;
		}

		@Override
		public BakedGlyph get(int codePoint) {
			return FontStorage.this.getBaked(codePoint).get(advanceValidating).get();
		}

		@Override
		public BakedGlyph getObfuscated(Random random, int width) {
			return FontStorage.this.getObfuscatedBakedGlyph(random, width);
		}
	}

	/**
	 * Ленивый поставщик запечённого глифа — откладывает вызов {@link Glyph#bake} до первого обращения.
	 */
	@Environment(EnvType.CLIENT)
	class LazyBakedGlyph implements Supplier<BakedGlyph> {

		final Glyph glyph;
		private @Nullable BakedGlyph baked;

		LazyBakedGlyph(final Glyph glyph) {
			this.glyph = glyph;
		}

		public BakedGlyph get() {
			if (baked == null) {
				baked = glyph.bake(FontStorage.this.abstractBaker);
			}

			return baked;
		}
	}
}
