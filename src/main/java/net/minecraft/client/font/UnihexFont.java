package net.minecraft.client.font;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Шрифт на основе формата Unifont HEX — текстового формата, где каждый глиф
 * задаётся шестнадцатеричной строкой пикселей размером 8×16, 16×16, 24×16 или 32×16.
 */
@Environment(EnvType.CLIENT)
public class UnihexFont implements Font {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final int GLYPH_HEIGHT = 16;
	private static final int NARROW_BYTES_PER_ROW = 2;
	private static final int NARROW_GLYPH_WIDTH = 32;
	private static final int MEDIUM_GLYPH_WIDTH = 64;
	private static final int WIDE_GLYPH_WIDTH = 96;
	private static final int EXTRA_WIDE_GLYPH_WIDTH = 128;
	private final GlyphContainer<UnihexFont.UnicodeTextureGlyph> glyphs;

	UnihexFont(GlyphContainer<UnihexFont.UnicodeTextureGlyph> glyphs) {
		this.glyphs = glyphs;
	}

	@Override
	public @Nullable Glyph getGlyph(int codePoint) {
		return glyphs.get(codePoint);
	}

	@Override
	public IntSet getProvidedGlyphs() {
		return glyphs.getProvidedGlyphs();
	}

	@VisibleForTesting
	static void addRowPixels(IntBuffer pixelsOut, int row, int left, int right) {
		int startBit = NARROW_GLYPH_WIDTH - left - 1;
		int endBit = NARROW_GLYPH_WIDTH - right - 1;

		for (int bit = startBit; bit >= endBit; bit--) {
			if (bit < NARROW_GLYPH_WIDTH && bit >= 0) {
				boolean isSet = (row >> bit & 1) != 0;
				pixelsOut.put(isSet ? -1 : 0);
			} else {
				pixelsOut.put(0);
			}
		}
	}

	static void addGlyphPixels(IntBuffer pixelsOut, UnihexFont.BitmapGlyph glyph, int left, int right) {
		for (int row = 0; row < GLYPH_HEIGHT; row++) {
			addRowPixels(pixelsOut, glyph.getPixels(row), left, right);
		}
	}

	/**
	 * Читает строки HEX-файла и передаёт каждый глиф в {@code callback}.
	 * Формат строки: {@code XXXX:HHHH...} где XXXX — кодовая точка (4–6 hex-цифр),
	 * а HHHH... — пиксельные данные (32, 64, 96 или 128 hex-символов).
	 */
	@VisibleForTesting
	static void readLines(InputStream stream, UnihexFont.BitmapGlyphConsumer callback) throws IOException {
		int lineNum = 0;
		ByteList lineBuffer = new ByteArrayList(EXTRA_WIDE_GLYPH_WIDTH);

		while (true) {
			boolean hasColon = readUntilDelimiter(stream, lineBuffer, 58);
			int headerLen = lineBuffer.size();

			if (headerLen == 0 && !hasColon) {
				return;
			}

			if (!hasColon || headerLen != 4 && headerLen != 5 && headerLen != 6) {
				throw new IllegalArgumentException(
					"Invalid entry at line " + lineNum + ": expected 4, 5 or 6 hex digits followed by a colon"
				);
			}

			int codePoint = 0;
			for (int digitIndex = 0; digitIndex < headerLen; digitIndex++) {
				codePoint = codePoint << 4 | getHexDigitValue(lineNum, lineBuffer.getByte(digitIndex));
			}

			lineBuffer.clear();
			readUntilDelimiter(stream, lineBuffer, 10);
			int dataLen = lineBuffer.size();

			UnihexFont.BitmapGlyph glyph = switch (dataLen) {
				case NARROW_GLYPH_WIDTH -> UnihexFont.FontImage8x16.read(lineNum, lineBuffer);
				case MEDIUM_GLYPH_WIDTH -> UnihexFont.FontImage16x16.read(lineNum, lineBuffer);
				case WIDE_GLYPH_WIDTH -> UnihexFont.FontImage32x16.read24x16(lineNum, lineBuffer);
				case EXTRA_WIDE_GLYPH_WIDTH -> UnihexFont.FontImage32x16.read32x16(lineNum, lineBuffer);
				default -> throw new IllegalArgumentException(
					"Invalid entry at line " + lineNum
						+ ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line"
				);
			};

			callback.accept(codePoint, glyph);
			lineNum++;
			lineBuffer.clear();
		}
	}

	static int getHexDigitValue(int lineNum, ByteList bytes, int index) {
		return getHexDigitValue(lineNum, bytes.getByte(index));
	}

	private static int getHexDigitValue(int lineNum, byte digit) {
		return switch (digit) {
			case 48 -> 0;
			case 49 -> 1;
			case 50 -> 2;
			case 51 -> 3;
			case 52 -> 4;
			case 53 -> 5;
			case 54 -> 6;
			case 55 -> 7;
			case 56 -> 8;
			case 57 -> 9;
			case 65 -> 10;
			case 66 -> 11;
			case 67 -> 12;
			case 68 -> 13;
			case 69 -> 14;
			case 70 -> 15;
			default -> throw new IllegalArgumentException(
				"Invalid entry at line " + lineNum + ": expected hex digit, got " + (char) digit
			);
		};
	}

	private static boolean readUntilDelimiter(InputStream stream, ByteList data, int delimiter) throws IOException {
		while (true) {
			int byteVal = stream.read();
			if (byteVal == -1) {
				return false;
			}

			if (byteVal == delimiter) {
				return true;
			}

			data.add((byte) byteVal);
		}
	}

	/**
	 * Растровое представление одного глифа Unifont.
	 * Каждая строка глифа кодируется как целое число, где биты соответствуют пикселям.
	 */
	@Environment(EnvType.CLIENT)
	public interface BitmapGlyph {

		int getPixels(int y);

		int bitWidth();

		default int getNonemptyColumnBitmask() {
			int mask = 0;
			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				mask |= getPixels(row);
			}

			return mask;
		}

		/**
		 * Возвращает упакованные границы глифа (левая и правая колонки) в виде одного int.
		 * Если глиф пустой — возвращает границы всей ширины.
		 */
		default int getPackedDimensions() {
			int columnMask = getNonemptyColumnBitmask();
			int width = bitWidth();

			if (columnMask == 0) {
				return UnihexFont.Dimensions.pack(0, width);
			}

			int leftCol = Integer.numberOfLeadingZeros(columnMask);
			int rightCol = NARROW_GLYPH_WIDTH - Integer.numberOfTrailingZeros(columnMask) - 1;
			return UnihexFont.Dimensions.pack(leftCol, rightCol);
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface BitmapGlyphConsumer {

		void accept(int codePoint, UnihexFont.BitmapGlyph glyph);
	}

	@Environment(EnvType.CLIENT)
	record DimensionOverride(int from, int to, UnihexFont.Dimensions dimensions) {

		private static final Codec<UnihexFont.DimensionOverride> NON_VALIDATED_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codecs.CODEPOINT.fieldOf("from").forGetter(UnihexFont.DimensionOverride::from),
				Codecs.CODEPOINT.fieldOf("to").forGetter(UnihexFont.DimensionOverride::to),
				UnihexFont.Dimensions.MAP_CODEC.forGetter(UnihexFont.DimensionOverride::dimensions)
			).apply(instance, UnihexFont.DimensionOverride::new)
		);

		public static final Codec<UnihexFont.DimensionOverride> CODEC = NON_VALIDATED_CODEC.validate(
			override -> override.from >= override.to
				? DataResult.error(() -> "Invalid range: [" + override.from + ";" + override.to + "]")
				: DataResult.success(override)
		);
	}

	@Environment(EnvType.CLIENT)
	public record Dimensions(int left, int right) {

		public static final MapCodec<UnihexFont.Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.INT.fieldOf("left").forGetter(UnihexFont.Dimensions::left),
				Codec.INT.fieldOf("right").forGetter(UnihexFont.Dimensions::right)
			).apply(instance, UnihexFont.Dimensions::new)
		);

		public static final Codec<UnihexFont.Dimensions> CODEC = MAP_CODEC.codec();

		public int packedValue() {
			return pack(left, right);
		}

		public static int pack(int left, int right) {
			return (left & 0xFF) << 8 | right & 0xFF;
		}

		public static int getLeft(int packed) {
			return (byte) (packed >> 8);
		}

		public static int getRight(int packed) {
			return (byte) packed;
		}
	}

	@Environment(EnvType.CLIENT)
	record FontImage16x16(short[] contents) implements UnihexFont.BitmapGlyph {

		@Override
		public int getPixels(int y) {
			return contents[y] << GLYPH_HEIGHT;
		}

		static UnihexFont.BitmapGlyph read(int lineNum, ByteList data) {
			short[] rows = new short[GLYPH_HEIGHT];
			int pos = 0;

			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				int d0 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d1 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d2 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d3 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				rows[row] = (short) (d0 << 12 | d1 << 8 | d2 << 4 | d3);
			}

			return new UnihexFont.FontImage16x16(rows);
		}

		@Override
		public int bitWidth() {
			return GLYPH_HEIGHT;
		}
	}

	@Environment(EnvType.CLIENT)
	record FontImage32x16(int[] contents, int bitWidth) implements UnihexFont.BitmapGlyph {

		private static final int BUFFER_SIZE = 24;

		@Override
		public int getPixels(int y) {
			return contents[y];
		}

		static UnihexFont.BitmapGlyph read24x16(int lineNum, ByteList data) {
			int[] rows = new int[GLYPH_HEIGHT];
			int pos = 0;

			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				int d0 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d1 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d2 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d3 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d4 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d5 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int packed = d0 << 20 | d1 << GLYPH_HEIGHT | d2 << 12 | d3 << 8 | d4 << 4 | d5;
				rows[row] = packed << 8;
			}

			return new UnihexFont.FontImage32x16(rows, BUFFER_SIZE);
		}

		public static UnihexFont.BitmapGlyph read32x16(int lineNum, ByteList data) {
			int[] rows = new int[GLYPH_HEIGHT];
			int pos = 0;

			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				int d0 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d1 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d2 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d3 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d4 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d5 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d6 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int d7 = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				rows[row] = d0 << 28 | d1 << BUFFER_SIZE | d2 << 20 | d3 << GLYPH_HEIGHT | d4 << 12 | d5 << 8 | d6 << 4 | d7;
			}

			return new UnihexFont.FontImage32x16(rows, NARROW_GLYPH_WIDTH);
		}
	}

	@Environment(EnvType.CLIENT)
	record FontImage8x16(byte[] contents) implements UnihexFont.BitmapGlyph {

		@Override
		public int getPixels(int y) {
			return contents[y] << FontImage32x16.BUFFER_SIZE;
		}

		static UnihexFont.BitmapGlyph read(int lineNum, ByteList data) {
			byte[] rows = new byte[GLYPH_HEIGHT];
			int pos = 0;

			for (int row = 0; row < GLYPH_HEIGHT; row++) {
				int high = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				int low = UnihexFont.getHexDigitValue(lineNum, data, pos++);
				rows[row] = (byte) (high << 4 | low);
			}

			return new UnihexFont.FontImage8x16(rows);
		}

		@Override
		public int bitWidth() {
			return 8;
		}
	}

	/**
	 * Загрузчик шрифта из ZIP-архива с HEX-файлами.
	 * Поддерживает переопределение размеров глифов через {@link DimensionOverride}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Loader implements FontLoader {

		public static final MapCodec<UnihexFont.Loader> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Identifier.CODEC.fieldOf("hex_file").forGetter(loader -> loader.sizes),
				UnihexFont.DimensionOverride.CODEC
					.listOf()
					.optionalFieldOf("size_overrides", List.of())
					.forGetter(loader -> loader.overrides)
			).apply(instance, UnihexFont.Loader::new)
		);

		private final Identifier sizes;
		private final List<UnihexFont.DimensionOverride> overrides;

		private Loader(Identifier sizes, List<UnihexFont.DimensionOverride> overrides) {
			this.sizes = sizes;
			this.overrides = overrides;
		}

		@Override
		public FontType getType() {
			return FontType.UNIHEX;
		}

		@Override
		public Either<FontLoader.Loadable, FontLoader.Reference> build() {
			return Either.left(this::load);
		}

		private Font load(ResourceManager resourceManager) throws IOException {
			try (InputStream inputStream = resourceManager.open(sizes)) {
				return loadHexFile(inputStream);
			}
		}

		private UnihexFont loadHexFile(InputStream stream) throws IOException {
			GlyphContainer<UnihexFont.BitmapGlyph> rawGlyphs =
				new GlyphContainer<>(UnihexFont.BitmapGlyph[]::new, UnihexFont.BitmapGlyph[][]::new);

			try (ZipInputStream zip = new ZipInputStream(stream)) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					String name = entry.getName();
					if (name.endsWith(".hex")) {
						UnihexFont.LOGGER.info("Found {}, loading", name);
						UnihexFont.readLines(new FixedBufferInputStream(zip), rawGlyphs::put);
					}
				}

				GlyphContainer<UnihexFont.UnicodeTextureGlyph> finalGlyphs = new GlyphContainer<>(
					UnihexFont.UnicodeTextureGlyph[]::new,
					UnihexFont.UnicodeTextureGlyph[][]::new
				);

				for (UnihexFont.DimensionOverride override : overrides) {
					UnihexFont.Dimensions dims = override.dimensions();

					for (int cp = override.from(); cp <= override.to(); cp++) {
						UnihexFont.BitmapGlyph glyph = rawGlyphs.remove(cp);
						if (glyph != null) {
							finalGlyphs.put(cp, new UnihexFont.UnicodeTextureGlyph(glyph, dims.left(), dims.right()));
						}
					}
				}

				rawGlyphs.forEachGlyph((codePoint, glyph) -> {
					int packed = glyph.getPackedDimensions();
					int left = UnihexFont.Dimensions.getLeft(packed);
					int right = UnihexFont.Dimensions.getRight(packed);
					finalGlyphs.put(codePoint, new UnihexFont.UnicodeTextureGlyph(glyph, left, right));
				});

				return new UnihexFont(finalGlyphs);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	record UnicodeTextureGlyph(UnihexFont.BitmapGlyph contents, int left, int right) implements Glyph {

		public int width() {
			return right - left + 1;
		}

		@Override
		public GlyphMetrics getMetrics() {
			return new GlyphMetrics() {
				@Override
				public float getAdvance() {
					return UnicodeTextureGlyph.this.width() / 2 + 1;
				}

				@Override
				public float getShadowOffset() {
					return 0.5F;
				}

				@Override
				public float getBoldOffset() {
					return 0.5F;
				}
			};
		}

		@Override
		public BakedGlyph bake(Glyph.AbstractGlyphBaker baker) {
			return baker.bake(
				getMetrics(),
				new UploadableGlyph() {
					@Override
					public float getOversample() {
						return 2.0F;
					}

					@Override
					public int getWidth() {
						return UnicodeTextureGlyph.this.width();
					}

					@Override
					public int getHeight() {
						return GLYPH_HEIGHT;
					}

					@Override
					public void upload(int x, int y, GpuTexture texture) {
						IntBuffer pixelBuffer = MemoryUtil.memAllocInt(
							UnicodeTextureGlyph.this.width() * GLYPH_HEIGHT
						);

						UnihexFont.addGlyphPixels(
							pixelBuffer,
							UnicodeTextureGlyph.this.contents,
							UnicodeTextureGlyph.this.left,
							UnicodeTextureGlyph.this.right
						);
						pixelBuffer.rewind();

						RenderSystem.getDevice()
							.createCommandEncoder()
							.writeToTexture(
								texture,
								MemoryUtil.memByteBuffer(pixelBuffer),
								NativeImage.Format.RGBA,
								0,
								0,
								x,
								y,
								UnicodeTextureGlyph.this.width(),
								GLYPH_HEIGHT
							);

						MemoryUtil.memFree(pixelBuffer);
					}

					@Override
					public boolean hasColor() {
						return true;
					}
				}
			);
		}
	}
}
