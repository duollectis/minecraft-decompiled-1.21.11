package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FreeTypeUtil;
import net.minecraft.client.util.Untracker;
import net.minecraft.util.PngMetadata;
import net.minecraft.util.math.ColorHelper;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Обёртка над нативным (off-heap) буфером пикселей изображения.
 * Поддерживает чтение PNG через STB, запись PNG, копирование, ресайз и рендеринг глифов FreeType.
 * Управление памятью — ручное: необходимо вызывать {@link #close()} после использования.
 */
@Environment(EnvType.CLIENT)
public final class NativeImage implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("NativeImage");
	private static final Set<StandardOpenOption> WRITE_TO_FILE_OPEN_OPTIONS = EnumSet.of(
		StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
	);

	private final NativeImage.Format format;
	private final int width;
	private final int height;
	private final boolean isStbImage;
	private long pointer;
	private final long sizeBytes;

	public NativeImage(int width, int height, boolean useStb) {
		this(NativeImage.Format.RGBA, width, height, useStb);
	}

	public NativeImage(NativeImage.Format format, int width, int height, boolean useStb) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
		}

		this.format = format;
		this.width = width;
		this.height = height;
		sizeBytes = (long) width * height * format.getChannelCount();
		isStbImage = false;
		pointer = useStb
			? MemoryUtil.nmemCalloc(1L, sizeBytes)
			: MemoryUtil.nmemAlloc(sizeBytes);

		MEMORY_POOL.malloc(pointer, (int) sizeBytes);
		if (pointer == 0L) {
			throw new IllegalStateException(
				"Unable to allocate texture of size " + width + "x" + height + " (" + format.getChannelCount() + " channels)"
			);
		}
	}

	public NativeImage(NativeImage.Format format, int width, int height, boolean useStb, long pointer) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
		}

		this.format = format;
		this.width = width;
		this.height = height;
		isStbImage = useStb;
		this.pointer = pointer;
		sizeBytes = (long) width * height * format.getChannelCount();
	}

	@Override
	public String toString() {
		return "NativeImage[" + format + " " + width + "x" + height + "@" + pointer + (isStbImage ? "S" : "N") + "]";
	}

	private boolean isOutOfBounds(int x, int y) {
		return x < 0 || x >= width || y < 0 || y >= height;
	}

	public static NativeImage read(InputStream stream) throws IOException {
		return read(NativeImage.Format.RGBA, stream);
	}

	/**
	 * Читает PNG-изображение из потока, декодируя его через STB.
	 * Буфер потока освобождается и поток закрывается в любом случае.
	 */
	public static NativeImage read(NativeImage.@Nullable Format format, InputStream stream) throws IOException {
		ByteBuffer byteBuffer = null;
		try {
			byteBuffer = TextureUtil.readResource(stream);
			return read(format, byteBuffer);
		} finally {
			MemoryUtil.memFree(byteBuffer);
			IOUtils.closeQuietly(stream);
		}
	}

	public static NativeImage read(ByteBuffer buffer) throws IOException {
		return read(NativeImage.Format.RGBA, buffer);
	}

	/**
	 * Читает PNG из массива байт. Если массив помещается в стековый буфер MemoryStack,
	 * использует его; иначе выделяет heap-буфер через MemoryUtil.
	 */
	public static NativeImage read(byte[] bytes) throws IOException {
		MemoryStack stack = MemoryStack.stackGet();
		if (stack.getPointer() < bytes.length) {
			ByteBuffer heapBuffer = MemoryUtil.memAlloc(bytes.length);
			try {
				return putAndRead(heapBuffer, bytes);
			} finally {
				MemoryUtil.memFree(heapBuffer);
			}
		}

		try (MemoryStack stackFrame = MemoryStack.stackPush()) {
			ByteBuffer stackBuffer = stackFrame.malloc(bytes.length);
			return putAndRead(stackBuffer, bytes);
		}
	}

	private static NativeImage putAndRead(ByteBuffer buffer, byte[] bytes) throws IOException {
		buffer.put(bytes);
		buffer.rewind();
		return read(buffer);
	}

	/**
	 * Читает PNG из нативного ByteBuffer через STB. Валидирует PNG-сигнатуру перед декодированием.
	 * Если {@code format} равен null, формат определяется автоматически по числу каналов.
	 */
	public static NativeImage read(NativeImage.@Nullable Format format, ByteBuffer buffer) throws IOException {
		if (format != null && !format.isWriteable()) {
			throw new UnsupportedOperationException("Don't know how to read format " + format);
		}

		if (MemoryUtil.memAddress(buffer) == 0L) {
			throw new IllegalArgumentException("Invalid buffer");
		}

		PngMetadata.validate(buffer);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer widthBuf = stack.mallocInt(1);
			IntBuffer heightBuf = stack.mallocInt(1);
			IntBuffer channelsBuf = stack.mallocInt(1);
			ByteBuffer decoded = STBImage.stbi_load_from_memory(
				buffer,
				widthBuf,
				heightBuf,
				channelsBuf,
				format == null ? 0 : format.channelCount
			);

			if (decoded == null) {
				throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
			}

			long decodedAddress = MemoryUtil.memAddress(decoded);
			MEMORY_POOL.malloc(decodedAddress, decoded.limit());
			return new NativeImage(
				format == null ? NativeImage.Format.fromChannelCount(channelsBuf.get(0)) : format,
				widthBuf.get(0),
				heightBuf.get(0),
				true,
				decodedAddress
			);
		}
	}

	private void checkAllocated() {
		if (pointer == 0L) {
			throw new IllegalStateException("Image is not allocated.");
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			if (isStbImage) {
				STBImage.nstbi_image_free(pointer);
			} else {
				MemoryUtil.nmemFree(pointer);
			}

			MEMORY_POOL.free(pointer);
		}

		pointer = 0L;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public NativeImage.Format getFormat() {
		return format;
	}

	private int getColor(int x, int y) {
		if (format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", format
			));
		}

		if (isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, width, height
			));
		}

		checkAllocated();
		long offset = (x + (long) y * width) * 4L;
		return MemoryUtil.memGetInt(pointer + offset);
	}

	public int getColorArgb(int x, int y) {
		return ColorHelper.fromAbgr(getColor(x, y));
	}

	public void setColor(int x, int y, int color) {
		if (format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", format
			));
		}

		if (isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, width, height
			));
		}

		checkAllocated();
		long offset = (x + (long) y * width) * 4L;
		MemoryUtil.memPutInt(pointer + offset, color);
	}

	public void setColorArgb(int x, int y, int color) {
		setColor(x, y, ColorHelper.toAbgr(color));
	}

	/**
	 * Применяет оператор к каждому пикселю и возвращает новое изображение с результатами.
	 * Работает только для формата RGBA; цвета конвертируются из ABGR в ARGB перед передачей оператору.
	 */
	public NativeImage applyToCopy(IntUnaryOperator operator) {
		if (format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "function application only works on RGBA images; have %s", format
			));
		}

		checkAllocated();
		NativeImage copy = new NativeImage(width, height, false);
		int pixelCount = width * height;
		IntBuffer src = MemoryUtil.memIntBuffer(pointer, pixelCount);
		IntBuffer dst = MemoryUtil.memIntBuffer(copy.pointer, pixelCount);

		for (int i = 0; i < pixelCount; i++) {
			int argb = ColorHelper.fromAbgr(src.get(i));
			dst.put(i, ColorHelper.toAbgr(operator.applyAsInt(argb)));
		}

		return copy;
	}

	public int[] copyPixelsAbgr() {
		if (format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "getPixels only works on RGBA images; have %s", format
			));
		}

		checkAllocated();
		int[] pixels = new int[width * height];
		MemoryUtil.memIntBuffer(pointer, width * height).get(pixels);
		return pixels;
	}

	public int[] copyPixelsArgb() {
		int[] pixels = copyPixelsAbgr();
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = ColorHelper.fromAbgr(pixels[i]);
		}

		return pixels;
	}

	public byte getOpacity(int x, int y) {
		if (!format.hasOpacityChannel()) {
			throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", format));
		}

		if (isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", x, y, width, height
			));
		}

		int byteOffset = (x + y * width) * format.getChannelCount() + format.getOpacityChannelOffset() / 8;
		return MemoryUtil.memGetByte(pointer + byteOffset);
	}

	/**
	 * @deprecated Используйте {@link #copyPixelsArgb()} или {@link #getColorArgb(int, int)}.
	 */
	@Deprecated
	public int[] makePixelArray() {
		if (format != NativeImage.Format.RGBA) {
			throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
		}

		checkAllocated();
		int[] pixels = new int[getWidth() * getHeight()];
		for (int row = 0; row < getHeight(); row++) {
			for (int col = 0; col < getWidth(); col++) {
				pixels[col + row * getWidth()] = getColorArgb(col, row);
			}
		}

		return pixels;
	}

	public void writeTo(File path) throws IOException {
		writeTo(path.toPath());
	}

	/**
	 * Рендерит глиф FreeType в однокомпонентное изображение (1 канал = grayscale).
	 * Размер изображения должен точно совпадать с размером растеризованного глифа.
	 */
	public boolean makeGlyphBitmapSubpixel(FT_Face face, int glyphIndex) {
		if (format.getChannelCount() != 1) {
			throw new IllegalArgumentException("Can only write fonts into 1-component images.");
		}

		if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, glyphIndex, 4), "Loading glyph")) {
			return false;
		}

		FT_GlyphSlot glyphSlot = Objects.requireNonNull(face.glyph(), "Glyph not initialized");
		FT_Bitmap bitmap = glyphSlot.bitmap();
		if (bitmap.pixel_mode() != 2) {
			throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
		}

		if (bitmap.width() != getWidth() || bitmap.rows() != getHeight()) {
			throw new IllegalArgumentException(String.format(
				Locale.ROOT,
				"Glyph bitmap of size %sx%s does not match image of size: %sx%s",
				bitmap.width(), bitmap.rows(), getWidth(), getHeight()
			));
		}

		int byteCount = bitmap.width() * bitmap.rows();
		ByteBuffer bitmapBuffer = Objects.requireNonNull(bitmap.buffer(byteCount), "Glyph has no bitmap");
		MemoryUtil.memCopy(MemoryUtil.memAddress(bitmapBuffer), pointer, byteCount);
		return true;
	}

	public void writeTo(Path path) throws IOException {
		if (!format.isWriteable()) {
			throw new UnsupportedOperationException("Don't know how to write format " + format);
		}

		checkAllocated();
		try (WritableByteChannel channel = Files.newByteChannel(path, WRITE_TO_FILE_OPEN_OPTIONS)) {
			if (!write(channel)) {
				throw new IOException(
					"Could not write image to the PNG file \"" + path.toAbsolutePath() + "\": "
						+ STBImage.stbi_failure_reason()
				);
			}
		}
	}

	private boolean write(WritableByteChannel channel) throws IOException {
		NativeImage.WriteCallback writeCallback = new NativeImage.WriteCallback(channel);
		try {
			int safeHeight = Math.min(getHeight(), Integer.MAX_VALUE / getWidth() / format.getChannelCount());
			if (safeHeight < getHeight()) {
				LOGGER.warn(
					"Dropping image height from {} to {} to fit the size into 32-bit signed int",
					getHeight(), safeHeight
				);
			}

			if (STBImageWrite.nstbi_write_png_to_func(
				writeCallback.address(), 0L, getWidth(), safeHeight, format.getChannelCount(), pointer, 0
			) != 0) {
				writeCallback.throwStoredException();
				return true;
			}

			return false;
		} finally {
			writeCallback.free();
		}
	}

	public void copyFrom(NativeImage image) {
		if (image.getFormat() != format) {
			throw new UnsupportedOperationException("Image formats don't match.");
		}

		int channels = format.getChannelCount();
		checkAllocated();
		image.checkAllocated();

		if (width == image.width) {
			MemoryUtil.memCopy(image.pointer, pointer, Math.min(sizeBytes, image.sizeBytes));
		} else {
			int copyWidth = Math.min(getWidth(), image.getWidth());
			int copyHeight = Math.min(getHeight(), image.getHeight());
			for (int row = 0; row < copyHeight; row++) {
				int srcOffset = row * image.getWidth() * channels;
				int dstOffset = row * getWidth() * channels;
				MemoryUtil.memCopy(image.pointer + srcOffset, pointer + dstOffset, copyWidth);
			}
		}
	}

	public void fillRect(int x, int y, int width, int height, int color) {
		for (int row = y; row < y + height; row++) {
			for (int col = x; col < x + width; col++) {
				setColorArgb(col, row, color);
			}
		}
	}

	public void copyRect(
		int x,
		int y,
		int translateX,
		int translateY,
		int width,
		int height,
		boolean flipX,
		boolean flipY
	) {
		copyRect(this, x, y, x + translateX, y + translateY, width, height, flipX, flipY);
	}

	public void copyRect(
		NativeImage image,
		int x,
		int y,
		int destX,
		int destY,
		int width,
		int height,
		boolean flipX,
		boolean flipY
	) {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int srcCol = flipX ? width - 1 - col : col;
				int srcRow = flipY ? height - 1 - row : row;
				int color = getColor(x + col, y + row);
				image.setColor(destX + srcCol, destY + srcRow, color);
			}
		}
	}

	public void resizeSubRectTo(int x, int y, int width, int height, NativeImage targetImage) {
		checkAllocated();
		if (targetImage.getFormat() != format) {
			throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
		}

		int channels = format.getChannelCount();
		STBImageResize.nstbir_resize_uint8(
			pointer + (x + y * getWidth()) * channels,
			width,
			height,
			getWidth() * channels,
			targetImage.pointer,
			targetImage.getWidth(),
			targetImage.getHeight(),
			0,
			channels
		);
	}

	public void untrack() {
		Untracker.untrack(pointer);
	}

	public long imageId() {
		return pointer;
	}

	/**
	 * Формат пикселей нативного изображения. Определяет количество каналов,
	 * их порядок и наличие компонент (R, G, B, A, Luminance).
	 */
	@Environment(EnvType.CLIENT)
	public enum Format {
		RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
		RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
		LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
		LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

		final int channelCount;
		private final boolean hasRed;
		private final boolean hasGreen;
		private final boolean hasBlue;
		private final boolean hasLuminance;
		private final boolean hasAlpha;
		private final int redOffset;
		private final int greenOffset;
		private final int blueOffset;
		private final int luminanceOffset;
		private final int alphaOffset;
		private final boolean writeable;

		Format(
			final int channelCount,
			final boolean hasRed,
			final boolean hasGreen,
			final boolean hasBlue,
			final boolean hasLuminance,
			final boolean hasAlpha,
			final int redOffset,
			final int greenOffset,
			final int blueOffset,
			final int luminanceOffset,
			final int alphaOffset,
			final boolean writeable
		) {
			this.channelCount = channelCount;
			this.hasRed = hasRed;
			this.hasGreen = hasGreen;
			this.hasBlue = hasBlue;
			this.hasLuminance = hasLuminance;
			this.hasAlpha = hasAlpha;
			this.redOffset = redOffset;
			this.greenOffset = greenOffset;
			this.blueOffset = blueOffset;
			this.luminanceOffset = luminanceOffset;
			this.alphaOffset = alphaOffset;
			this.writeable = writeable;
		}

		public int getChannelCount() {
			return channelCount;
		}

		public boolean hasRed() {
			return hasRed;
		}

		public boolean hasGreen() {
			return hasGreen;
		}

		public boolean hasBlue() {
			return hasBlue;
		}

		public boolean hasLuminance() {
			return hasLuminance;
		}

		public boolean hasAlpha() {
			return hasAlpha;
		}

		public int getRedOffset() {
			return redOffset;
		}

		public int getGreenOffset() {
			return greenOffset;
		}

		public int getBlueOffset() {
			return blueOffset;
		}

		public int getLuminanceOffset() {
			return luminanceOffset;
		}

		public int getAlphaOffset() {
			return alphaOffset;
		}

		public boolean hasRedChannel() {
			return hasLuminance || hasRed;
		}

		public boolean hasGreenChannel() {
			return hasLuminance || hasGreen;
		}

		public boolean hasBlueChannel() {
			return hasLuminance || hasBlue;
		}

		public boolean hasOpacityChannel() {
			return hasLuminance || hasAlpha;
		}

		public int getRedChannelOffset() {
			return hasLuminance ? luminanceOffset : redOffset;
		}

		public int getGreenChannelOffset() {
			return hasLuminance ? luminanceOffset : greenOffset;
		}

		public int getBlueChannelOffset() {
			return hasLuminance ? luminanceOffset : blueOffset;
		}

		public int getOpacityChannelOffset() {
			return hasLuminance ? luminanceOffset : alphaOffset;
		}

		public boolean isWriteable() {
			return writeable;
		}

		static NativeImage.Format fromChannelCount(int channelCount) {
			return switch (channelCount) {
				case 1 -> LUMINANCE;
				case 2 -> LUMINANCE_ALPHA;
				case 3 -> RGB;
				default -> RGBA;
			};
		}
	}

	@Environment(EnvType.CLIENT)
	static class WriteCallback extends STBIWriteCallback {

		private final WritableByteChannel channel;
		private @Nullable IOException exception;

		WriteCallback(WritableByteChannel channel) {
			this.channel = channel;
		}

		@Override
		public void invoke(long context, long data, int size) {
			ByteBuffer buffer = getData(data, size);
			try {
				channel.write(buffer);
			} catch (IOException ex) {
				exception = ex;
			}
		}

		public void throwStoredException() throws IOException {
			if (exception != null) {
				throw exception;
			}
		}
	}
}
