package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.annotation.DeobfuscateClass;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * Утилитарный класс для работы с текстурами: чтение из потоков,
 * экспорт в PNG, заполнение прозрачных областей цветом.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class TextureUtil {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final int MIN_MIPMAP_LEVEL = 0;

	private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

	/** Направления для BFS-обхода соседних пикселей: право, лево, вниз, вверх. */
	private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	/**
	 * Читает ресурс из входного потока в нативный буфер памяти.
	 * Если поток поддерживает {@link SeekableByteChannel}, размер буфера определяется заранее.
	 * Возвращённый буфер необходимо освободить через {@code MemoryUtil.memFree()}.
	 *
	 * @param inputStream входной поток с данными ресурса
	 * @return нативный {@link ByteBuffer} с содержимым потока (позиция = 0, лимит = размер данных)
	 * @throws IOException при ошибке чтения
	 */
	public static ByteBuffer readResource(InputStream inputStream) throws IOException {
		ReadableByteChannel channel = Channels.newChannel(inputStream);
		int initialSize = channel instanceof SeekableByteChannel seekable
			? (int) seekable.size() + 1
			: DEFAULT_IMAGE_BUFFER_SIZE;

		return readResource(channel, initialSize);
	}

	private static ByteBuffer readResource(ReadableByteChannel channel, int bufSize) throws IOException {
		ByteBuffer buffer = MemoryUtil.memAlloc(bufSize);

		try {
			while (channel.read(buffer) != -1) {
				if (!buffer.hasRemaining()) {
					buffer = MemoryUtil.memRealloc(buffer, buffer.capacity() * 2);
				}
			}

			buffer.flip();
			return buffer;
		} catch (IOException exception) {
			MemoryUtil.memFree(buffer);
			throw exception;
		}
	}

	/**
	 * Асинхронно экспортирует все уровни мипмапов текстуры в PNG-файлы.
	 * Файлы сохраняются в директорию {@code directory} с именами вида {@code prefix_N.png}.
	 * Операция выполняется через GPU-буфер и завершается асинхронно.
	 *
	 * @param directory     директория для сохранения файлов
	 * @param prefix        префикс имени файла
	 * @param texture       исходная GPU-текстура
	 * @param scales        количество уровней мипмапов (0 = только базовый)
	 * @param colorFunction функция преобразования цвета пикселя перед сохранением
	 * @throws IllegalArgumentException если суммарный размер текстур превышает 2 ГБ
	 */
	public static void writeAsPNG(
		Path directory,
		String prefix,
		GpuTexture texture,
		int scales,
		IntUnaryOperator colorFunction
	) {
		RenderSystem.assertOnRenderThread();

		long totalBytes = 0L;

		for (int mip = 0; mip <= scales; mip++) {
			totalBytes += (long) texture.getFormat().pixelSize() * texture.getWidth(mip) * texture.getHeight(mip);
		}

		if (totalBytes > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Exporting textures larger than 2GB is not supported");
		}

		GpuBuffer outputBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, totalBytes);
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

		Runnable readbackTask = () -> {
			try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(outputBuffer, true, false)) {
				int byteOffset = 0;

				for (int mip = 0; mip <= scales; mip++) {
					int mipWidth = texture.getWidth(mip);
					int mipHeight = texture.getHeight(mip);
					int pixelSize = texture.getFormat().pixelSize();

					try (NativeImage image = new NativeImage(mipWidth, mipHeight, false)) {
						for (int row = 0; row < mipHeight; row++) {
							for (int col = 0; col < mipWidth; col++) {
								int rawColor = mappedView.data().getInt(byteOffset + (col + row * mipWidth) * pixelSize);
								image.setColor(col, row, colorFunction.applyAsInt(rawColor));
							}
						}

						Path outputPath = directory.resolve(prefix + "_" + mip + ".png");

						try {
							image.writeTo(outputPath);
							LOGGER.debug("Exported png to: {}", outputPath.toAbsolutePath());
						} catch (IOException exception) {
							LOGGER.debug("Unable to write: ", exception);
						}
					}

					byteOffset += pixelSize * mipWidth * mipHeight;
				}
			}

			outputBuffer.close();
		};

		AtomicInteger completedMips = new AtomicInteger();
		int bufferOffset = 0;

		for (int mip = 0; mip <= scales; mip++) {
			final int currentMip = mip;
			commandEncoder.copyTextureToBuffer(
				texture, outputBuffer, bufferOffset, () -> {
					if (completedMips.getAndIncrement() == scales) {
						readbackTask.run();
					}
				}, currentMip
			);
			bufferOffset += texture.getFormat().pixelSize() * texture.getWidth(mip) * texture.getHeight(mip);
		}
	}

	/** Возвращает путь к директории отладочных текстур относительно заданного пути. */
	public static Path getDebugTexturePath(Path path) {
		return path.resolve("screenshots").resolve("debug");
	}

	/** Возвращает путь к директории отладочных текстур относительно текущей директории. */
	public static Path getDebugTexturePath() {
		return getDebugTexturePath(Path.of("."));
	}

	/**
	 * Заполняет прозрачные пиксели изображения цветом ближайшего непрозрачного пикселя.
	 * Использует BFS (поиск в ширину) для распространения цвета от непрозрачных пикселей.
	 * Альфа-канал заполненных пикселей устанавливается в 0 (остаются прозрачными).
	 *
	 * <p>Применяется для устранения артефактов на краях текстур при билинейной фильтрации.
	 *
	 * @param image изображение для обработки (модифицируется на месте)
	 */
	public static void solidify(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] colors = new int[width * height];
		int[] distances = new int[width * height];
		Arrays.fill(distances, Integer.MAX_VALUE);

		IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = image.getColorArgb(x, y);

				if (ColorHelper.getAlpha(color) != 0) {
					int packed = pack(x, y, width);
					distances[packed] = 0;
					colors[packed] = color;
					queue.enqueue(packed);
				}
			}
		}

		while (!queue.isEmpty()) {
			int packed = queue.dequeueInt();
			int px = x(packed, width);
			int py = y(packed, width);

			for (int[] direction : DIRECTIONS) {
				int nx = px + direction[0];
				int ny = py + direction[1];
				int neighborPacked = pack(nx, ny, width);

				if (nx >= 0 && ny >= 0 && nx < width && ny < height
					&& distances[neighborPacked] > distances[packed] + 1
				) {
					distances[neighborPacked] = distances[packed] + 1;
					colors[neighborPacked] = colors[packed];
					queue.enqueue(neighborPacked);
				}
			}
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = image.getColorArgb(x, y);

				if (ColorHelper.getAlpha(color) == 0) {
					image.setColorArgb(x, y, ColorHelper.withAlpha(0, colors[pack(x, y, width)]));
				} else {
					image.setColorArgb(x, y, color);
				}
			}
		}
	}

	/**
	 * Заполняет прозрачные пиксели изображения затемнённой версией наиболее тёмного
	 * непрозрачного цвета (75% от минимального по сумме RGB-каналов).
	 *
	 * <p>Применяется для создания фонового цвета под прозрачными областями текстур.
	 *
	 * @param image изображение для обработки (модифицируется на месте)
	 */
	public static void fillEmptyAreasWithDarkColor(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int darkestColor = -1;
		int minBrightness = Integer.MAX_VALUE;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = image.getColorArgb(x, y);

				if (ColorHelper.getAlpha(color) != 0) {
					int brightness = ColorHelper.getRed(color) + ColorHelper.getGreen(color) + ColorHelper.getBlue(color);

					if (brightness < minBrightness) {
						minBrightness = brightness;
						darkestColor = color;
					}
				}
			}
		}

		// Затемняем до 75% от найденного минимального цвета
		int fillRed = 3 * ColorHelper.getRed(darkestColor) / 4;
		int fillGreen = 3 * ColorHelper.getGreen(darkestColor) / 4;
		int fillBlue = 3 * ColorHelper.getBlue(darkestColor) / 4;
		int fillColor = ColorHelper.getArgb(0, fillRed, fillGreen, fillBlue);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = image.getColorArgb(x, y);

				if (ColorHelper.getAlpha(color) == 0) {
					image.setColorArgb(x, y, fillColor);
				}
			}
		}
	}

	private static int pack(int x, int y, int width) {
		return x + y * width;
	}

	private static int x(int packed, int width) {
		return packed % width;
	}

	private static int y(int packed, int width) {
		return packed / width;
	}
}
