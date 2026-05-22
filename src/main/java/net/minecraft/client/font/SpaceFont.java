package net.minecraft.client.font;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Шрифт пробелов — содержит только глифы с нулевой высотой и заданной шириной продвижения.
 * Используется для управления горизонтальными отступами в тексте.
 */
@Environment(EnvType.CLIENT)
public class SpaceFont implements Font {

	private final Int2ObjectMap<EmptyGlyph> codePointsToGlyphs;

	public SpaceFont(Map<Integer, Float> codePointsToAdvances) {
		codePointsToGlyphs = new Int2ObjectOpenHashMap(codePointsToAdvances.size());
		codePointsToAdvances.forEach((codePoint, advance) -> codePointsToGlyphs.put(
				codePoint,
				new EmptyGlyph(advance)
		));
	}

	@Override
	public @Nullable Glyph getGlyph(int codePoint) {
		return codePointsToGlyphs.get(codePoint);
	}

	@Override
	public IntSet getProvidedGlyphs() {
		return IntSets.unmodifiable(codePointsToGlyphs.keySet());
	}

	@Environment(EnvType.CLIENT)
	public record Loader(Map<Integer, Float> advances) implements FontLoader {

		public static final MapCodec<SpaceFont.Loader> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Codec
								.unboundedMap(Codecs.CODEPOINT, Codec.FLOAT)
								.fieldOf("advances")
								.forGetter(SpaceFont.Loader::advances))
						.apply(instance, SpaceFont.Loader::new)
		);

		@Override
		public FontType getType() {
			return FontType.SPACE;
		}

		@Override
		public Either<FontLoader.Loadable, FontLoader.Reference> build() {
			FontLoader.Loadable loadable = resourceManager -> new SpaceFont(advances);
			return Either.left(loadable);
		}
	}
}
