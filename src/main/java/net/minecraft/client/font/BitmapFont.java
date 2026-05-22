package net.minecraft.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Шрифт на основе растрового изображения (bitmap).
 * Каждый символ задаётся прямоугольной областью в PNG-текстуре,
 * а сетка символов описывается двумерным массивом кодовых точек.
 */
@Environment(EnvType.CLIENT)
public class BitmapFont implements Font {

	static final Logger LOGGER = LogUtils.getLogger();
	private final NativeImage image;
	private final GlyphContainer<BitmapFont.BitmapFontGlyph> glyphs;

	BitmapFont(NativeImage image, GlyphContainer<BitmapFont.BitmapFontGlyph> glyphs) {
		this.image = image;
		this.glyphs = glyphs;
	}

	@Override
	public void close() {
		image.close();
	}

	@Override
	public @Nullable Glyph getGlyph(int codePoint) {
		return glyphs.get(codePoint);
	}

	@Override
	public IntSet getProvidedGlyphs() {
		return IntSets.unmodifiable(glyphs.getProvidedGlyphs());
	}

	/**
	 * Один глиф растрового шрифта: хранит ссылку на исходное изображение,
	 * координаты своей области в нём и метрики для рендеринга.
	 */
	@Environment(EnvType.CLIENT)
	record BitmapFontGlyph(
			float scaleFactor,
			NativeImage image,
			int x,
			int y,
			int width,
			int height,
			int advance,
			int ascent
	) implements Glyph {

		@Override
		public GlyphMetrics getMetrics() {
			return GlyphMetrics.empty(advance);
		}

		@Override
		public BakedGlyph bake(Glyph.AbstractGlyphBaker baker) {
			return baker.bake(
					getMetrics(),
					new UploadableGlyph() {
						@Override
						public float getOversample() {
							return 1.0F / BitmapFontGlyph.this.scaleFactor;
						}

						@Override
						public int getWidth() {
							return BitmapFontGlyph.this.width;
						}

						@Override
						public int getHeight() {
							return BitmapFontGlyph.this.height;
						}

						@Override
						public float getAscent() {
							return BitmapFontGlyph.this.ascent;
						}

						@Override
						public void upload(int x, int y, GpuTexture texture) {
							RenderSystem.getDevice()
							            .createCommandEncoder()
							            .writeToTexture(
									            texture,
									            BitmapFontGlyph.this.image,
									            0,
									            0,
									            x,
									            y,
									            BitmapFontGlyph.this.width,
									            BitmapFontGlyph.this.height,
									            BitmapFontGlyph.this.x,
									            BitmapFontGlyph.this.y
							            );
						}

						@Override
						public boolean hasColor() {
							return BitmapFontGlyph.this.image.getFormat().getChannelCount() > 1;
						}
					}
			);
		}
	}

	/**
	 * Загрузчик растрового шрифта из JSON-описания ресурс-пака.
	 * Читает PNG-файл, разбивает его на ячейки по сетке кодовых точек
	 * и вычисляет реальную ширину каждого символа по непрозрачным пикселям.
	 */
	@Environment(EnvType.CLIENT)
	public record Loader(Identifier file, int height, int ascent, int[][] codepointGrid) implements FontLoader {

		private static final Codec<int[][]> CODE_POINT_GRID_CODEC = Codec.STRING.listOf().xmap(
				strings -> {
					int rowCount = strings.size();
					int[][] grid = new int[rowCount][];

					for (int row = 0; row < rowCount; row++) {
						grid[row] = strings.get(row).codePoints().toArray();
					}

					return grid;
				},
				codePointGrid -> {
					List<String> list = new ArrayList<>(codePointGrid.length);

					for (int[] row : codePointGrid) {
						list.add(new String(row, 0, row.length));
					}

					return list;
				}
		).validate(BitmapFont.Loader::validateCodePointGrid);

		public static final MapCodec<BitmapFont.Loader> CODEC = RecordCodecBuilder.<BitmapFont.Loader>mapCodec(
				instance -> instance.<Identifier, Integer, Integer, int[][]>group(
						Identifier.CODEC.fieldOf("file").forGetter(BitmapFont.Loader::file),
						Codec.INT.optionalFieldOf("height", 8).forGetter(BitmapFont.Loader::height),
						Codec.INT.fieldOf("ascent").forGetter(BitmapFont.Loader::ascent),
						CODE_POINT_GRID_CODEC.fieldOf("chars").forGetter(BitmapFont.Loader::codepointGrid)
				).apply(instance, BitmapFont.Loader::new)
		).validate(BitmapFont.Loader::validate);

		private static DataResult<int[][]> validateCodePointGrid(int[][] codePointGrid) {
			int rowCount = codePointGrid.length;

			if (rowCount == 0) {
				return DataResult.error(() -> "Expected to find data in codepoint grid");
			}

			int firstRowLength = codePointGrid[0].length;

			if (firstRowLength == 0) {
				return DataResult.error(() -> "Expected to find data in codepoint grid");
			}

			for (int row = 1; row < rowCount; row++) {
				int[] currentRow = codePointGrid[row];

				if (currentRow.length != firstRowLength) {
					return DataResult.error(
							() -> "Lines in codepoint grid have to be the same length (found: " + currentRow.length
									+ " codepoints, expected: " + firstRowLength + "), pad with \\u0000"
					);
				}
			}

			return DataResult.success(codePointGrid);
		}

		private static DataResult<BitmapFont.Loader> validate(BitmapFont.Loader loader) {
			return loader.ascent > loader.height
			       ? DataResult.error(() -> "Ascent " + loader.ascent + " higher than height " + loader.height)
			       : DataResult.success(loader);
		}

		@Override
		public FontType getType() {
			return FontType.BITMAP;
		}

		@Override
		public Either<FontLoader.Loadable, FontLoader.Reference> build() {
			return Either.left(this::load);
		}

		/**
		 * Загружает PNG-текстуру и строит контейнер глифов.
		 * Ширина каждого символа определяется по крайнему правому непрозрачному пикселю.
		 */
		private Font load(ResourceManager resourceManager) throws IOException {
			Identifier textureId = file.withPrefixedPath("textures/");

			BitmapFont result;
			try (InputStream inputStream = resourceManager.open(textureId)) {
				NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, inputStream);
				int imageWidth = nativeImage.getWidth();
				int imageHeight = nativeImage.getHeight();
				int cellWidth = imageWidth / codepointGrid[0].length;
				int cellHeight = imageHeight / codepointGrid.length;
				float scaleFactor = (float) height / cellHeight;
				GlyphContainer<BitmapFont.BitmapFontGlyph> glyphContainer = new GlyphContainer<>(
						BitmapFont.BitmapFontGlyph[]::new, BitmapFont.BitmapFontGlyph[][]::new
				);

				for (int row = 0; row < codepointGrid.length; row++) {
					int col = 0;

					for (int codePoint : codepointGrid[row]) {
						int currentCol = col++;

						if (codePoint == 0) {
							continue;
						}

						int charWidth = findCharacterStartX(nativeImage, cellWidth, cellHeight, currentCol, row);
						BitmapFont.BitmapFontGlyph existing = glyphContainer.put(
								codePoint,
								new BitmapFont.BitmapFontGlyph(
										scaleFactor,
										nativeImage,
										currentCol * cellWidth,
										row * cellHeight,
										cellWidth,
										cellHeight,
										(int) (0.5 + charWidth * scaleFactor) + 1,
										ascent
								)
						);

						if (existing != null) {
							LOGGER.warn(
									"Codepoint '{}' declared multiple times in {}",
									Integer.toHexString(codePoint),
									textureId
							);
						}
					}
				}

				result = new BitmapFont(nativeImage, glyphContainer);
			}

			return result;
		}

		/**
		 * Находит X-координату правого края непрозрачной области символа
		 * путём сканирования столбцов справа налево.
		 *
		 * @param image           исходное изображение шрифта
		 * @param characterWidth  ширина ячейки символа в пикселях
		 * @param characterHeight высота ячейки символа в пикселях
		 * @param charPosX        позиция символа по X в сетке (в ячейках)
		 * @param charPosY        позиция символа по Y в сетке (в ячейках)
		 * @return ширина непрозрачной части символа (1-based)
		 */
		private int findCharacterStartX(
				NativeImage image,
				int characterWidth,
				int characterHeight,
				int charPosX,
				int charPosY
		) {
			int col;

			for (col = characterWidth - 1; col >= 0; col--) {
				int pixelX = charPosX * characterWidth + col;

				for (int row = 0; row < characterHeight; row++) {
					int pixelY = charPosY * characterHeight + row;

					if (image.getOpacity(pixelX, pixelY) != 0) {
						return col + 1;
					}
				}
			}

			return col + 1;
		}
	}
}
