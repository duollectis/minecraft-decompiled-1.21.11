package net.minecraft.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.util.function.Consumer;

/**
 * Утилита для захвата и сохранения скриншотов из фреймбуфера OpenGL.
 * Поддерживает уменьшение разрешения с усреднением пикселей для антиалиасинга.
 */
@Environment(EnvType.CLIENT)
public class ScreenshotRecorder {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String SCREENSHOTS_DIRECTORY = "screenshots";

	public static void saveScreenshot(File gameDirectory, Framebuffer framebuffer, Consumer<Text> messageReceiver) {
		saveScreenshot(gameDirectory, null, framebuffer, 1, messageReceiver);
	}

	/**
	 * Делает скриншот фреймбуфера и асинхронно сохраняет его в директорию screenshots.
	 * При успехе отправляет кликабельное сообщение с именем файла, при ошибке — сообщение об ошибке.
	 *
	 * @param gameDirectory    корневая директория игры
	 * @param fileName         имя файла или {@code null} для автогенерации по времени
	 * @param framebuffer      фреймбуфер для захвата
	 * @param downscaleFactor  коэффициент уменьшения (1 = без уменьшения)
	 * @param messageReceiver  получатель сообщения о результате
	 */
	public static void saveScreenshot(
		File gameDirectory,
		@Nullable String fileName,
		Framebuffer framebuffer,
		int downscaleFactor,
		Consumer<Text> messageReceiver
	) {
		takeScreenshot(framebuffer, downscaleFactor, image -> {
			File screenshotsDir = new File(gameDirectory, SCREENSHOTS_DIRECTORY);
			screenshotsDir.mkdir();

			File outputFile = fileName == null
				? getScreenshotFilename(screenshotsDir)
				: new File(screenshotsDir, fileName);

			Util.getIoWorkerExecutor().execute(() -> {
				try (NativeImage capturedImage = image) {
					capturedImage.writeTo(outputFile);
					Text fileLink = Text.literal(outputFile.getName())
						.formatted(Formatting.UNDERLINE)
						.styled(style -> style.withClickEvent(new ClickEvent.OpenFile(outputFile.getAbsoluteFile())));
					messageReceiver.accept(Text.translatable("screenshot.success", fileLink));
				} catch (Exception exception) {
					LOGGER.warn("Couldn't save screenshot", exception);
					messageReceiver.accept(Text.translatable("screenshot.failure", exception.getMessage()));
				}
			});
		});
	}

	public static void takeScreenshot(Framebuffer framebuffer, Consumer<NativeImage> callback) {
		takeScreenshot(framebuffer, 1, callback);
	}

	/**
	 * Захватывает содержимое фреймбуфера в {@link NativeImage} с опциональным уменьшением.
	 * При {@code downscaleFactor > 1} усредняет пиксели блоками для антиалиасинга.
	 * Изображение переворачивается по вертикали (OpenGL хранит строки снизу вверх).
	 *
	 * @param framebuffer     фреймбуфер для захвата
	 * @param downscaleFactor коэффициент уменьшения; размеры должны делиться на него нацело
	 * @param callback        получатель готового изображения
	 * @throws IllegalStateException    если фреймбуфер неполный
	 * @throws IllegalArgumentException если размеры не делятся на коэффициент
	 */
	public static void takeScreenshot(Framebuffer framebuffer, int downscaleFactor, Consumer<NativeImage> callback) {
		int textureWidth = framebuffer.textureWidth;
		int textureHeight = framebuffer.textureHeight;
		GpuTexture colorAttachment = framebuffer.getColorAttachment();

		if (colorAttachment == null) {
			throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
		}

		if (textureWidth % downscaleFactor != 0 || textureHeight % downscaleFactor != 0) {
			throw new IllegalArgumentException("Image size is not divisible by downscale factor");
		}

		GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(
			() -> "Screenshot buffer",
			9,
			(long) textureWidth * textureHeight * colorAttachment.getFormat().pixelSize()
		);

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(
			colorAttachment,
			gpuBuffer,
			0L,
			() -> {
				try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
					int outputWidth = textureWidth / downscaleFactor;
					int outputHeight = textureHeight / downscaleFactor;
					NativeImage image = new NativeImage(outputWidth, outputHeight, false);
					int pixelSize = colorAttachment.getFormat().pixelSize();

					for (int row = 0; row < outputHeight; row++) {
						for (int col = 0; col < outputWidth; col++) {
							if (downscaleFactor == 1) {
								int packedColor = mappedView.data().getInt((col + row * textureWidth) * pixelSize);
								image.setColor(col, textureHeight - row - 1, packedColor | 0xFF000000);
							} else {
								int totalRed = 0;
								int totalGreen = 0;
								int totalBlue = 0;

								for (int sampleX = 0; sampleX < downscaleFactor; sampleX++) {
									for (int sampleY = 0; sampleY < downscaleFactor; sampleY++) {
										int srcX = col * downscaleFactor + sampleX;
										int srcY = row * downscaleFactor + sampleY;
										int packedColor = mappedView.data().getInt((srcX + srcY * textureWidth) * pixelSize);
										totalRed += ColorHelper.getRed(packedColor);
										totalGreen += ColorHelper.getGreen(packedColor);
										totalBlue += ColorHelper.getBlue(packedColor);
									}
								}

								int sampleCount = downscaleFactor * downscaleFactor;
								image.setColor(
									col,
									outputHeight - row - 1,
									ColorHelper.getArgb(255, totalRed / sampleCount, totalGreen / sampleCount, totalBlue / sampleCount)
								);
							}
						}
					}

					callback.accept(image);
				}

				gpuBuffer.close();
			},
			0
		);
	}

	private static File getScreenshotFilename(File directory) {
		String timestamp = Util.getFormattedCurrentTime();
		int counter = 1;

		while (true) {
			File file = new File(directory, timestamp + (counter == 1 ? "" : "_" + counter) + ".png");
			if (!file.exists()) {
				return file;
			}

			counter++;
		}
	}
}
