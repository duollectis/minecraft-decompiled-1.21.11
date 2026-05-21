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

@Environment(EnvType.CLIENT)
/**
 * {@code NativeImage}.
 */
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
		if (width > 0 && height > 0) {
			this.format = format;
			this.width = width;
			this.height = height;
			this.sizeBytes = (long) width * height * format.getChannelCount();
			this.isStbImage = false;
			if (useStb) {
				this.pointer = MemoryUtil.nmemCalloc(1L, this.sizeBytes);
			}
			else {
				this.pointer = MemoryUtil.nmemAlloc(this.sizeBytes);
			}

			MEMORY_POOL.malloc(this.pointer, (int) this.sizeBytes);
			if (this.pointer == 0L) {
				throw new IllegalStateException(
						"Unable to allocate texture of size " + width + "x" + height + " (" + format.getChannelCount()
								+ " channels)");
			}
		}
		else {
			throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
		}
	}

	public NativeImage(NativeImage.Format format, int width, int height, boolean useStb, long pointer) {
		if (width > 0 && height > 0) {
			this.format = format;
			this.width = width;
			this.height = height;
			this.isStbImage = useStb;
			this.pointer = pointer;
			this.sizeBytes = (long) width * height * format.getChannelCount();
		}
		else {
			throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
		}
	}

	@Override
	public String toString() {
		return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pointer + (
				this.isStbImage ? "S" : "N"
		) + "]";
	}

	private boolean isOutOfBounds(int x, int y) {
		return x < 0 || x >= this.width || y < 0 || y >= this.height;
	}

	/**
	 * Read.
	 *
	 * @param stream stream
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage read(InputStream stream) throws IOException {
		return read(NativeImage.Format.RGBA, stream);
	}

	/**
	 * Read.
	 *
	 * @param format format
	 * @param stream stream
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage read(NativeImage.@Nullable Format format, InputStream stream) throws IOException {
		ByteBuffer byteBuffer = null;

		NativeImage var3;
		try {
			byteBuffer = TextureUtil.readResource(stream);
			var3 = read(format, byteBuffer);
		}
		finally {
			MemoryUtil.memFree(byteBuffer);
			IOUtils.closeQuietly(stream);
		}

		return var3;
	}

	/**
	 * Read.
	 *
	 * @param buffer buffer
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage read(ByteBuffer buffer) throws IOException {
		return read(NativeImage.Format.RGBA, buffer);
	}

	/**
	 * Read.
	 *
	 * @param bytes bytes
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage read(byte[] bytes) throws IOException {
		MemoryStack memoryStack = MemoryStack.stackGet();
		int i = memoryStack.getPointer();
		if (i < bytes.length) {
			ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);

			NativeImage var13;
			try {
				var13 = putAndRead(byteBuffer, bytes);
			}
			finally {
				MemoryUtil.memFree(byteBuffer);
			}

			return var13;
		}
		else {
			MemoryStack memoryStack2 = MemoryStack.stackPush();

			NativeImage var5;
			try {
				ByteBuffer byteBuffer2 = memoryStack2.malloc(bytes.length);
				var5 = putAndRead(byteBuffer2, bytes);
			}
			catch (Throwable var11) {
				if (memoryStack2 != null) {
					try {
						memoryStack2.close();
					}
					catch (Throwable var9) {
						var11.addSuppressed(var9);
					}
				}

				throw var11;
			}

			if (memoryStack2 != null) {
				memoryStack2.close();
			}

			return var5;
		}
	}

	private static NativeImage putAndRead(ByteBuffer buffer, byte[] bytes) throws IOException {
		buffer.put(bytes);
		buffer.rewind();
		return read(buffer);
	}

	/**
	 * Read.
	 *
	 * @param format format
	 * @param buffer buffer
	 *
	 * @return NativeImage — результат операции
	 */
	public static NativeImage read(NativeImage.@Nullable Format format, ByteBuffer buffer) throws IOException {
		if (format != null && !format.isWriteable()) {
			throw new UnsupportedOperationException("Don't know how to read format " + format);
		}
		else if (MemoryUtil.memAddress(buffer) == 0L) {
			throw new IllegalArgumentException("Invalid buffer");
		}
		else {
			PngMetadata.validate(buffer);
			MemoryStack memoryStack = MemoryStack.stackPush();

			NativeImage var9;
			try {
				IntBuffer intBuffer = memoryStack.mallocInt(1);
				IntBuffer intBuffer2 = memoryStack.mallocInt(1);
				IntBuffer intBuffer3 = memoryStack.mallocInt(1);
				ByteBuffer
						byteBuffer =
						STBImage.stbi_load_from_memory(
								buffer,
								intBuffer,
								intBuffer2,
								intBuffer3,
								format == null ? 0 : format.channelCount
						);
				if (byteBuffer == null) {
					throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
				}

				long l = MemoryUtil.memAddress(byteBuffer);
				MEMORY_POOL.malloc(l, byteBuffer.limit());
				var9 = new NativeImage(
						format == null ? NativeImage.Format.fromChannelCount(intBuffer3.get(0)) : format,
						intBuffer.get(0),
						intBuffer2.get(0),
						true,
						l
				);
			}
			catch (Throwable var11) {
				if (memoryStack != null) {
					try {
						memoryStack.close();
					}
					catch (Throwable var10) {
						var11.addSuppressed(var10);
					}
				}

				throw var11;
			}

			if (memoryStack != null) {
				memoryStack.close();
			}

			return var9;
		}
	}

	private void checkAllocated() {
		if (this.pointer == 0L) {
			throw new IllegalStateException("Image is not allocated.");
		}
	}

	@Override
	public void close() {
		if (this.pointer != 0L) {
			if (this.isStbImage) {
				STBImage.nstbi_image_free(this.pointer);
			}
			else {
				MemoryUtil.nmemFree(this.pointer);
			}

			MEMORY_POOL.free(this.pointer);
		}

		this.pointer = 0L;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public NativeImage.Format getFormat() {
		return this.format;
	}

	private int getColor(int x, int y) {
		if (this.format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"getPixelRGBA only works on RGBA images; have %s",
					this.format
			));
		}
		else if (this.isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"(%s, %s) outside of image bounds (%s, %s)",
					x,
					y,
					this.width,
					this.height
			));
		}
		else {
			this.checkAllocated();
			long l = (x + (long) y * this.width) * 4L;
			return MemoryUtil.memGetInt(this.pointer + l);
		}
	}

	public int getColorArgb(int x, int y) {
		return ColorHelper.fromAbgr(this.getColor(x, y));
	}

	public void setColor(int x, int y, int color) {
		if (this.format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"setPixelRGBA only works on RGBA images; have %s",
					this.format
			));
		}
		else if (this.isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"(%s, %s) outside of image bounds (%s, %s)",
					x,
					y,
					this.width,
					this.height
			));
		}
		else {
			this.checkAllocated();
			long l = (x + (long) y * this.width) * 4L;
			MemoryUtil.memPutInt(this.pointer + l, color);
		}
	}

	public void setColorArgb(int x, int y, int color) {
		this.setColor(x, y, ColorHelper.toAbgr(color));
	}

	/**
	 * Применяет to copy.
	 *
	 * @param operator operator
	 *
	 * @return NativeImage — результат операции
	 */
	public NativeImage applyToCopy(IntUnaryOperator operator) {
		if (this.format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"function application only works on RGBA images; have %s",
					this.format
			));
		}
		else {
			this.checkAllocated();
			NativeImage nativeImage = new NativeImage(this.width, this.height, false);
			int i = this.width * this.height;
			IntBuffer intBuffer = MemoryUtil.memIntBuffer(this.pointer, i);
			IntBuffer intBuffer2 = MemoryUtil.memIntBuffer(nativeImage.pointer, i);

			for (int j = 0; j < i; j++) {
				int k = ColorHelper.fromAbgr(intBuffer.get(j));
				int l = operator.applyAsInt(k);
				intBuffer2.put(j, ColorHelper.toAbgr(l));
			}

			return nativeImage;
		}
	}

	/**
	 * Создаёт копию pixels abgr.
	 *
	 * @return int[] — результат операции
	 */
	public int[] copyPixelsAbgr() {
		if (this.format != NativeImage.Format.RGBA) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"getPixels only works on RGBA images; have %s",
					this.format
			));
		}
		else {
			this.checkAllocated();
			int[] is = new int[this.width * this.height];
			MemoryUtil.memIntBuffer(this.pointer, this.width * this.height).get(is);
			return is;
		}
	}

	/**
	 * Создаёт копию pixels argb.
	 *
	 * @return int[] — результат операции
	 */
	public int[] copyPixelsArgb() {
		int[] is = this.copyPixelsAbgr();

		for (int i = 0; i < is.length; i++) {
			is[i] = ColorHelper.fromAbgr(is[i]);
		}

		return is;
	}

	public byte getOpacity(int x, int y) {
		if (!this.format.hasOpacityChannel()) {
			throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
		}
		else if (this.isOutOfBounds(x, y)) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"(%s, %s) outside of image bounds (%s, %s)",
					x,
					y,
					this.width,
					this.height
			));
		}
		else {
			int i = (x + y * this.width) * this.format.getChannelCount() + this.format.getOpacityChannelOffset() / 8;
			return MemoryUtil.memGetByte(this.pointer + i);
		}
	}

	@Deprecated
	/**
	 * Make pixel array.
	 *
	 * @return int[] — результат операции
	 */
	public int[] makePixelArray() {
		if (this.format != NativeImage.Format.RGBA) {
			throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
		}
		else {
			this.checkAllocated();
			int[] is = new int[this.getWidth() * this.getHeight()];

			for (int i = 0; i < this.getHeight(); i++) {
				for (int j = 0; j < this.getWidth(); j++) {
					is[j + i * this.getWidth()] = this.getColorArgb(j, i);
				}
			}

			return is;
		}
	}

	/**
	 * Записывает to.
	 *
	 * @param path path
	 */
	public void writeTo(File path) throws IOException {
		this.writeTo(path.toPath());
	}

	/**
	 * Make glyph bitmap subpixel.
	 *
	 * @param face face
	 * @param glyphIndex glyph index
	 *
	 * @return boolean — результат операции
	 */
	public boolean makeGlyphBitmapSubpixel(FT_Face face, int glyphIndex) {
		if (this.format.getChannelCount() != 1) {
			throw new IllegalArgumentException("Can only write fonts into 1-component images.");
		}
		else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(face, glyphIndex, 4), "Loading glyph")) {
			return false;
		}
		else {
			FT_GlyphSlot fT_GlyphSlot = Objects.requireNonNull(face.glyph(), "Glyph not initialized");
			FT_Bitmap fT_Bitmap = fT_GlyphSlot.bitmap();
			if (fT_Bitmap.pixel_mode() != 2) {
				throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
			}
			else if (fT_Bitmap.width() == this.getWidth() && fT_Bitmap.rows() == this.getHeight()) {
				int i = fT_Bitmap.width() * fT_Bitmap.rows();
				ByteBuffer byteBuffer = Objects.requireNonNull(fT_Bitmap.buffer(i), "Glyph has no bitmap");
				MemoryUtil.memCopy(MemoryUtil.memAddress(byteBuffer), this.pointer, i);
				return true;
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Glyph bitmap of size %sx%s does not match image of size: %sx%s",
								fT_Bitmap.width(),
								fT_Bitmap.rows(),
								this.getWidth(),
								this.getHeight()
						)
				);
			}
		}
	}

	/**
	 * Записывает to.
	 *
	 * @param path path
	 */
	public void writeTo(Path path) throws IOException {
		if (!this.format.isWriteable()) {
			throw new UnsupportedOperationException("Don't know how to write format " + this.format);
		}
		else {
			this.checkAllocated();

			try (WritableByteChannel writableByteChannel = Files.newByteChannel(path, WRITE_TO_FILE_OPEN_OPTIONS)) {
				if (!this.write(writableByteChannel)) {
					throw new IOException("Could not write image to the PNG file \"" + path.toAbsolutePath() + "\": "
							+ STBImage.stbi_failure_reason());
				}
			}
		}
	}

	private boolean write(WritableByteChannel channel) throws IOException {
		NativeImage.WriteCallback writeCallback = new NativeImage.WriteCallback(channel);

		boolean var4;
		try {
			int i = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.getChannelCount());
			if (i < this.getHeight()) {
				LOGGER.warn(
						"Dropping image height from {} to {} to fit the size into 32-bit signed int",
						this.getHeight(),
						i
				);
			}

			if (STBImageWrite.nstbi_write_png_to_func(
					writeCallback.address(),
					0L,
					this.getWidth(),
					i,
					this.format.getChannelCount(),
					this.pointer,
					0
			) != 0) {
				writeCallback.throwStoredException();
				return true;
			}

			var4 = false;
		}
		finally {
			writeCallback.free();
		}

		return var4;
	}

	/**
	 * Создаёт копию from.
	 *
	 * @param image image
	 */
	public void copyFrom(NativeImage image) {
		if (image.getFormat() != this.format) {
			throw new UnsupportedOperationException("Image formats don't match.");
		}
		else {
			int i = this.format.getChannelCount();
			this.checkAllocated();
			image.checkAllocated();
			if (this.width == image.width) {
				MemoryUtil.memCopy(image.pointer, this.pointer, Math.min(this.sizeBytes, image.sizeBytes));
			}
			else {
				int j = Math.min(this.getWidth(), image.getWidth());
				int k = Math.min(this.getHeight(), image.getHeight());

				for (int l = 0; l < k; l++) {
					int m = l * image.getWidth() * i;
					int n = l * this.getWidth() * i;
					MemoryUtil.memCopy(image.pointer + m, this.pointer + n, j);
				}
			}
		}
	}

	/**
	 * Fill rect.
	 *
	 * @param x x
	 * @param y y
	 * @param width width
	 * @param height height
	 * @param color color
	 */
	public void fillRect(int x, int y, int width, int height, int color) {
		for (int i = y; i < y + height; i++) {
			for (int j = x; j < x + width; j++) {
				this.setColorArgb(j, i, color);
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
		this.copyRect(this, x, y, x + translateX, y + translateY, width, height, flipX, flipY);
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
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int k = flipX ? width - 1 - j : j;
				int l = flipY ? height - 1 - i : i;
				int m = this.getColor(x + j, y + i);
				image.setColor(destX + k, destY + l, m);
			}
		}
	}

	/**
	 * Resize sub rect to.
	 *
	 * @param x x
	 * @param y y
	 * @param width width
	 * @param height height
	 * @param targetImage target image
	 */
	public void resizeSubRectTo(int x, int y, int width, int height, NativeImage targetImage) {
		this.checkAllocated();
		if (targetImage.getFormat() != this.format) {
			throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
		}
		else {
			int i = this.format.getChannelCount();
			STBImageResize.nstbir_resize_uint8(
					this.pointer + (x + y * this.getWidth()) * i,
					width,
					height,
					this.getWidth() * i,
					targetImage.pointer,
					targetImage.getWidth(),
					targetImage.getHeight(),
					0,
					i
			);
		}
	}

	/**
	 * Untrack.
	 */
	public void untrack() {
		Untracker.untrack(this.pointer);
	}

	/**
	 * Image id.
	 *
	 * @return long — результат операции
	 */
	public long imageId() {
		return this.pointer;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Format}.
	 */
	public static enum Format {
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

		private Format(
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
			return this.channelCount;
		}

		public boolean hasRed() {
			return this.hasRed;
		}

		public boolean hasGreen() {
			return this.hasGreen;
		}

		public boolean hasBlue() {
			return this.hasBlue;
		}

		public boolean hasLuminance() {
			return this.hasLuminance;
		}

		public boolean hasAlpha() {
			return this.hasAlpha;
		}

		public int getRedOffset() {
			return this.redOffset;
		}

		public int getGreenOffset() {
			return this.greenOffset;
		}

		public int getBlueOffset() {
			return this.blueOffset;
		}

		public int getLuminanceOffset() {
			return this.luminanceOffset;
		}

		public int getAlphaOffset() {
			return this.alphaOffset;
		}

		public boolean hasRedChannel() {
			return this.hasLuminance || this.hasRed;
		}

		public boolean hasGreenChannel() {
			return this.hasLuminance || this.hasGreen;
		}

		public boolean hasBlueChannel() {
			return this.hasLuminance || this.hasBlue;
		}

		public boolean hasOpacityChannel() {
			return this.hasLuminance || this.hasAlpha;
		}

		public int getRedChannelOffset() {
			return this.hasLuminance ? this.luminanceOffset : this.redOffset;
		}

		public int getGreenChannelOffset() {
			return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
		}

		public int getBlueChannelOffset() {
			return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
		}

		public int getOpacityChannelOffset() {
			return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
		}

		public boolean isWriteable() {
			return this.writeable;
		}

		static NativeImage.Format fromChannelCount(int glFormat) {
			switch (glFormat) {
				case 1:
					return LUMINANCE;
				case 2:
					return LUMINANCE_ALPHA;
				case 3:
					return RGB;
				case 4:
				default:
					return RGBA;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WriteCallback}.
	 */
	static class WriteCallback extends STBIWriteCallback {

		private final WritableByteChannel channel;
		private @Nullable IOException exception;

		WriteCallback(WritableByteChannel channel) {
			this.channel = channel;
		}

		/**
		 * Invoke.
		 *
		 * @param context context
		 * @param data data
		 * @param size size
		 */
		public void invoke(long context, long data, int size) {
			ByteBuffer byteBuffer = getData(data, size);

			try {
				this.channel.write(byteBuffer);
			}
			catch (IOException var8) {
				this.exception = var8;
			}
		}

		/**
		 * Throw stored exception.
		 */
		public void throwStoredException() throws IOException {
			if (this.exception != null) {
				throw this.exception;
			}
		}
	}
}
