package net.minecraft.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * Реализация шрифта на основе FreeType (TrueType/OpenType).
 * Загружает глифы лениво через {@link LazyGlyph}, используя индексы FreeType.
 * Потокобезопасность обеспечивается синхронизацией по объекту {@link FT_Face}.
 */
@Environment(EnvType.CLIENT)
public class TrueTypeFont implements Font {

	private @Nullable ByteBuffer buffer;
	private @Nullable FT_Face face;
	final float oversample;
	private final GlyphContainer<TrueTypeFont.LazyGlyph> container =
		new GlyphContainer<>(TrueTypeFont.LazyGlyph[]::new, TrueTypeFont.LazyGlyph[][]::new);

	public TrueTypeFont(
		ByteBuffer buffer,
		FT_Face face,
		float size,
		float oversample,
		float shiftX,
		float shiftY,
		String excludedCharacters
	) {
		this.buffer = buffer;
		this.face = face;
		this.oversample = oversample;

		IntSet excluded = new IntArraySet();
		excludedCharacters.codePoints().forEach(excluded::add);

		int pixelSize = Math.round(size * oversample);
		FreeType.FT_Set_Pixel_Sizes(face, pixelSize, pixelSize);

		float scaledShiftX = shiftX * oversample;
		float scaledShiftY = -shiftY * oversample;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			FT_Vector transform = FreeTypeUtil.set(FT_Vector.malloc(stack), scaledShiftX, scaledShiftY);
			FreeType.FT_Set_Transform(face, null, transform);

			IntBuffer glyphIndexBuf = stack.mallocInt(1);
			int codePoint = (int) FreeType.FT_Get_First_Char(face, glyphIndexBuf);

			while (true) {
				int glyphIndex = glyphIndexBuf.get(0);
				if (glyphIndex == 0) {
					break;
				}

				if (!excluded.contains(codePoint)) {
					container.put(codePoint, new TrueTypeFont.LazyGlyph(glyphIndex));
				}

				codePoint = (int) FreeType.FT_Get_Next_Char(face, codePoint, glyphIndexBuf);
			}
		}
	}

	@Override
	public @Nullable Glyph getGlyph(int codePoint) {
		TrueTypeFont.LazyGlyph lazyGlyph = container.get(codePoint);
		return lazyGlyph != null ? getOrLoadGlyph(codePoint, lazyGlyph) : null;
	}

	/**
	 * Возвращает уже загруженный глиф или загружает его через FreeType.
	 * Использует двойную проверку блокировки (double-checked locking) для потокобезопасности.
	 */
	private Glyph getOrLoadGlyph(int codePoint, TrueTypeFont.LazyGlyph lazy) {
		Glyph loaded = lazy.glyph;
		if (loaded != null) {
			return loaded;
		}

		FT_Face ftFace = getInfo();
		synchronized (ftFace) {
			loaded = lazy.glyph;
			if (loaded == null) {
				loaded = loadGlyph(codePoint, ftFace, lazy.index);
				lazy.glyph = loaded;
			}
		}

		return loaded;
	}

	private Glyph loadGlyph(int codePoint, FT_Face ftFace, int glyphIndex) {
		int errorCode = FreeType.FT_Load_Glyph(ftFace, glyphIndex, 4194312);
		if (errorCode != 0) {
			FreeTypeUtil.checkFatalError(
				errorCode,
				String.format(Locale.ROOT, "Loading glyph U+%06X", codePoint)
			);
		}

		FT_GlyphSlot glyphSlot = ftFace.glyph();
		if (glyphSlot == null) {
			throw new NullPointerException(
				String.format(Locale.ROOT, "Glyph U+%06X not initialized", codePoint)
			);
		}

		float advance = FreeTypeUtil.getX(glyphSlot.advance());
		int bitmapLeft = glyphSlot.bitmap_left();
		int bitmapTop = glyphSlot.bitmap_top();
		int bitmapWidth = glyphSlot.bitmap().width();
		int bitmapRows = glyphSlot.bitmap().rows();

		return bitmapWidth > 0 && bitmapRows > 0
			? new TrueTypeFont.TtfGlyph(bitmapLeft, bitmapTop, bitmapWidth, bitmapRows, advance, glyphIndex)
			: new EmptyGlyph(advance / oversample);
	}

	FT_Face getInfo() {
		if (buffer != null && face != null) {
			return face;
		}

		throw new IllegalStateException("Provider already closed");
	}

	@Override
	public void close() {
		if (face != null) {
			synchronized (FreeTypeUtil.LOCK) {
				FreeTypeUtil.checkError(FreeType.FT_Done_Face(face), "Deleting face");
			}

			face = null;
		}

		MemoryUtil.memFree(buffer);
		buffer = null;
	}

	@Override
	public IntSet getProvidedGlyphs() {
		return container.getProvidedGlyphs();
	}

	@Environment(EnvType.CLIENT)
	static class LazyGlyph {

		final int index;
		volatile @Nullable Glyph glyph;

		LazyGlyph(int index) {
			this.index = index;
		}
	}

	@Environment(EnvType.CLIENT)
	class TtfGlyph implements Glyph {

		final int width;
		final int height;
		final float bearingX;
		final float ascent;
		private final GlyphMetrics metrics;
		final int glyphIndex;

		TtfGlyph(
			final int bearingX,
			final int ascent,
			final int width,
			final int height,
			final float advance,
			final int glyphIndex
		) {
			this.width = width;
			this.height = height;
			this.metrics = GlyphMetrics.empty(advance / TrueTypeFont.this.oversample);
			this.bearingX = bearingX / TrueTypeFont.this.oversample;
			this.ascent = ascent / TrueTypeFont.this.oversample;
			this.glyphIndex = glyphIndex;
		}

		@Override
		public GlyphMetrics getMetrics() {
			return metrics;
		}

		@Override
		public BakedGlyph bake(Glyph.AbstractGlyphBaker baker) {
			return baker.bake(
				metrics,
				new UploadableGlyph() {
					@Override
					public int getWidth() {
						return TtfGlyph.this.width;
					}

					@Override
					public int getHeight() {
						return TtfGlyph.this.height;
					}

					@Override
					public float getOversample() {
						return TrueTypeFont.this.oversample;
					}

					@Override
					public float getBearingX() {
						return TtfGlyph.this.bearingX;
					}

					@Override
					public float getAscent() {
						return TtfGlyph.this.ascent;
					}

					@Override
					public void upload(int x, int y, GpuTexture texture) {
						FT_Face ftFace = TrueTypeFont.this.getInfo();

						try (NativeImage image = new NativeImage(
							NativeImage.Format.LUMINANCE,
							TtfGlyph.this.width,
							TtfGlyph.this.height,
							false
						)) {
							if (image.makeGlyphBitmapSubpixel(ftFace, TtfGlyph.this.glyphIndex)) {
								RenderSystem.getDevice()
									.createCommandEncoder()
									.writeToTexture(
										texture,
										image,
										0,
										0,
										x,
										y,
										TtfGlyph.this.width,
										TtfGlyph.this.height,
										0,
										0
									);
							}
						}
					}

					@Override
					public boolean hasColor() {
						return false;
					}
				}
			);
		}
	}
}
