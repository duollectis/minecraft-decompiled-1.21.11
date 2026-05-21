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

@Environment(EnvType.CLIENT)
/**
 * {@code FontStorage}.
 */
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
	private final GlyphContainer<FontStorage.GlyphPair>
			bakedGlyphCache =
			new GlyphContainer<>(FontStorage.GlyphPair[]::new, FontStorage.GlyphPair[][]::new);
	private final IntFunction<FontStorage.GlyphPair> findGlyph = this::findGlyph;
	BakedGlyph blankBakedGlyph = MISSING_GLYPH;
	private final Supplier<BakedGlyph> blankGlyphSupplier = () -> this.blankBakedGlyph;
	private final FontStorage.GlyphPair
			blankBakedGlyphPair =
			new FontStorage.GlyphPair(this.blankGlyphSupplier, this.blankGlyphSupplier);
	private @Nullable EffectGlyph whiteRectangleBakedGlyph;
	private final GlyphProvider anyGlyphs = new FontStorage.Glyphs(false);
	private final GlyphProvider advanceValidatingGlyphs = new FontStorage.Glyphs(true);

	public FontStorage(GlyphBaker baker) {
		this.glyphBaker = baker;
	}

	public void setFonts(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
		this.allFonts = allFonts;
		this.setActiveFilters(activeFilters);
	}

	public void setActiveFilters(Set<FontFilterType> activeFilters) {
		this.availableFonts = List.of();
		this.clear();
		this.availableFonts = this.applyFilters(this.allFonts, activeFilters);
	}

	private void clear() {
		this.glyphBaker.clear();
		this.bakedGlyphCache.clear();
		this.charactersByWidth.clear();
		this.blankBakedGlyph = Objects.requireNonNull(BuiltinEmptyGlyph.MISSING.bake(this.glyphBaker));
		this.whiteRectangleBakedGlyph = BuiltinEmptyGlyph.WHITE.bake(this.glyphBaker);
	}

	private List<Font> applyFilters(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
		IntSet intSet = new IntOpenHashSet();
		List<Font> list = new ArrayList<>();

		for (Font.FontFilterPair fontFilterPair : allFonts) {
			if (fontFilterPair.filter().isAllowed(activeFilters)) {
				list.add(fontFilterPair.provider());
				intSet.addAll(fontFilterPair.provider().getProvidedGlyphs());
			}
		}

		Set<Font> set = Sets.newHashSet();
		intSet.forEach(
				codePoint -> {
					for (Font font : list) {
						Glyph glyph = font.getGlyph(codePoint);
						if (glyph != null) {
							set.add(font);
							if (glyph.getMetrics() != BuiltinEmptyGlyph.MISSING) {
								((IntList) this.charactersByWidth.computeIfAbsent(
										MathHelper.ceil(glyph
												.getMetrics()
												.getAdvance(false)), i -> new IntArrayList()
								)
								)
										.add(codePoint);
							}
							break;
						}
					}
				}
		);
		return list.stream().filter(set::contains).toList();
	}

	@Override
	public void close() {
		this.glyphBaker.close();
	}

	private static boolean isAdvanceInvalid(GlyphMetrics glyph) {
		float f = glyph.getAdvance(false);
		if (!(f < 0.0F) && !(f > 32.0F)) {
			float g = glyph.getAdvance(true);
			return g < 0.0F || g > 32.0F;
		}
		else {
			return true;
		}
	}

	private FontStorage.GlyphPair findGlyph(int codePoint) {
		FontStorage.LazyBakedGlyph lazyBakedGlyph = null;

		for (Font font : this.availableFonts) {
			Glyph glyph = font.getGlyph(codePoint);
			if (glyph != null) {
				if (lazyBakedGlyph == null) {
					lazyBakedGlyph = new FontStorage.LazyBakedGlyph(glyph);
				}

				if (!isAdvanceInvalid(glyph.getMetrics())) {
					if (lazyBakedGlyph.glyph == glyph) {
						return new FontStorage.GlyphPair(lazyBakedGlyph, lazyBakedGlyph);
					}

					return new FontStorage.GlyphPair(lazyBakedGlyph, new FontStorage.LazyBakedGlyph(glyph));
				}
			}
		}

		return lazyBakedGlyph != null ? new FontStorage.GlyphPair(lazyBakedGlyph, this.blankGlyphSupplier)
		                              : this.blankBakedGlyphPair;
	}

	FontStorage.GlyphPair getBaked(int codePoint) {
		return this.bakedGlyphCache.computeIfAbsent(codePoint, this.findGlyph);
	}

	public BakedGlyph getObfuscatedBakedGlyph(Random random, int width) {
		IntList intList = (IntList) this.charactersByWidth.get(width);
		return intList != null && !intList.isEmpty()
		       ? this.getBaked(intList.getInt(random.nextInt(intList.size()))).advanceValidating().get()
		       : this.blankBakedGlyph;
	}

	public EffectGlyph getRectangleBakedGlyph() {
		return Objects.requireNonNull(this.whiteRectangleBakedGlyph);
	}

	public GlyphProvider getGlyphs(boolean advanceValidating) {
		return advanceValidating ? this.advanceValidatingGlyphs : this.anyGlyphs;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code GlyphPair}.
	 */
	record GlyphPair(Supplier<BakedGlyph> any, Supplier<BakedGlyph> advanceValidating) {

		Supplier<BakedGlyph> get(boolean advanceValidating) {
			return advanceValidating ? this.advanceValidating : this.any;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Glyphs}.
	 */
	public class Glyphs implements GlyphProvider {

		private final boolean advanceValidating;

		public Glyphs(final boolean advanceValidating) {
			this.advanceValidating = advanceValidating;
		}

		@Override
		public BakedGlyph get(int codePoint) {
			return FontStorage.this.getBaked(codePoint).get(this.advanceValidating).get();
		}

		@Override
		public BakedGlyph getObfuscated(Random random, int width) {
			return FontStorage.this.getObfuscatedBakedGlyph(random, width);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LazyBakedGlyph}.
	 */
	class LazyBakedGlyph implements Supplier<BakedGlyph> {

		final Glyph glyph;
		private @Nullable BakedGlyph baked;

		LazyBakedGlyph(final Glyph glyph) {
			this.glyph = glyph;
		}

		public BakedGlyph get() {
			if (this.baked == null) {
				this.baked = this.glyph.bake(FontStorage.this.abstractBaker);
			}

			return this.baked;
		}
	}
}
