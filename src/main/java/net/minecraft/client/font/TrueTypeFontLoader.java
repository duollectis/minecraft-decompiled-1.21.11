package net.minecraft.client.font;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Загрузчик TrueType-шрифта через FreeType.
 * Читает TTF-файл из ресурсов, инициализирует {@link FT_Face} и создаёт {@link TrueTypeFont}.
 * При ошибке освобождает нативную память и FreeType-объекты во избежание утечек.
 */
@Environment(EnvType.CLIENT)
public record TrueTypeFontLoader(
	Identifier location,
	float size,
	float oversample,
	TrueTypeFontLoader.Shift shift,
	String skip
) implements FontLoader {

	private static final Codec<String> SKIP_CODEC =
		Codec.withAlternative(Codec.STRING, Codec.STRING.listOf(), chars -> String.join("", chars));

	public static final MapCodec<TrueTypeFontLoader> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Identifier.CODEC.fieldOf("file").forGetter(TrueTypeFontLoader::location),
			Codec.FLOAT.optionalFieldOf("size", 11.0F).forGetter(TrueTypeFontLoader::size),
			Codec.FLOAT.optionalFieldOf("oversample", 1.0F).forGetter(TrueTypeFontLoader::oversample),
			TrueTypeFontLoader.Shift.CODEC
				.optionalFieldOf("shift", TrueTypeFontLoader.Shift.NONE)
				.forGetter(TrueTypeFontLoader::shift),
			SKIP_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeFontLoader::skip)
		).apply(instance, TrueTypeFontLoader::new)
	);

	@Override
	public FontType getType() {
		return FontType.TTF;
	}

	@Override
	public Either<FontLoader.Loadable, FontLoader.Reference> build() {
		return Either.left(this::load);
	}

	private Font load(ResourceManager resourceManager) throws IOException {
		FT_Face ftFace = null;
		ByteBuffer fontBuffer = null;

		try {
			try (InputStream inputStream = resourceManager.open(location.withPrefixedPath("font/"))) {
				fontBuffer = TextureUtil.readResource(inputStream);
			}

			synchronized (FreeTypeUtil.LOCK) {
				try (MemoryStack stack = MemoryStack.stackPush()) {
					PointerBuffer facePointer = stack.mallocPointer(1);
					FreeTypeUtil.checkFatalError(
						FreeType.FT_New_Memory_Face(
							FreeTypeUtil.initialize(),
							fontBuffer,
							0L,
							facePointer
						),
						"Initializing font face"
					);
					ftFace = FT_Face.create(facePointer.get());
				}

				String format = FreeType.FT_Get_Font_Format(ftFace);
				if (!"TrueType".equals(format)) {
					throw new IOException("Font is not in TTF format, was " + format);
				}

				FreeTypeUtil.checkFatalError(
					FreeType.FT_Select_Charmap(ftFace, FreeType.FT_ENCODING_UNICODE),
					"Find unicode charmap"
				);

				return new TrueTypeFont(
					fontBuffer,
					ftFace,
					size,
					oversample,
					shift.x,
					shift.y,
					skip
				);
			}
		} catch (Exception exception) {
			synchronized (FreeTypeUtil.LOCK) {
				if (ftFace != null) {
					FreeType.FT_Done_Face(ftFace);
				}
			}

			MemoryUtil.memFree(fontBuffer);
			throw exception;
		}
	}

	/**
	 * Смещение глифов шрифта по осям X и Y в пикселях.
	 * Диапазон значений ограничен [-512; 512] для предотвращения некорректного рендеринга.
	 */
	@Environment(EnvType.CLIENT)
	public record Shift(float x, float y) {

		public static final TrueTypeFontLoader.Shift NONE = new TrueTypeFontLoader.Shift(0.0F, 0.0F);

		public static final Codec<TrueTypeFontLoader.Shift> CODEC = Codec.floatRange(-512.0F, 512.0F)
			.listOf()
			.comapFlatMap(
				floatList -> Util
					.decodeFixedLengthList(floatList, 2)
					.map(list -> new TrueTypeFontLoader.Shift(list.get(0), list.get(1))),
				shiftVal -> List.of(shiftVal.x, shiftVal.y)
			);
	}
}
