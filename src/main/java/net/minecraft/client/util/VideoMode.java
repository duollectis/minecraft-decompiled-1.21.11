package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Описывает видеорежим монитора: разрешение, глубину цвета и частоту обновления.
 * Поддерживает парсинг из строки вида {@code "1920x1080@60:24"}.
 */
@Environment(EnvType.CLIENT)
public final class VideoMode {

	private static final Pattern PATTERN = Pattern.compile("(\\d+)x(\\d+)(?:@(\\d+)(?::(\\d+))?)?");
	private static final int DEFAULT_REFRESH_RATE = 60;
	private static final int DEFAULT_BIT_DEPTH = 24;

	private final int width;
	private final int height;
	private final int redBits;
	private final int greenBits;
	private final int blueBits;
	private final int refreshRate;

	public VideoMode(int width, int height, int redBits, int greenBits, int blueBits, int refreshRate) {
		this.width = width;
		this.height = height;
		this.redBits = redBits;
		this.greenBits = greenBits;
		this.blueBits = blueBits;
		this.refreshRate = refreshRate;
	}

	public VideoMode(Buffer buffer) {
		width = buffer.width();
		height = buffer.height();
		redBits = buffer.redBits();
		greenBits = buffer.greenBits();
		blueBits = buffer.blueBits();
		refreshRate = buffer.refreshRate();
	}

	public VideoMode(GLFWVidMode vidMode) {
		width = vidMode.width();
		height = vidMode.height();
		redBits = vidMode.redBits();
		greenBits = vidMode.greenBits();
		blueBits = vidMode.blueBits();
		refreshRate = vidMode.refreshRate();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getRedBits() {
		return redBits;
	}

	public int getGreenBits() {
		return greenBits;
	}

	public int getBlueBits() {
		return blueBits;
	}

	public int getRefreshRate() {
		return refreshRate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		VideoMode other = (VideoMode) o;
		return width == other.width
			&& height == other.height
			&& redBits == other.redBits
			&& greenBits == other.greenBits
			&& blueBits == other.blueBits
			&& refreshRate == other.refreshRate;
	}

	@Override
	public int hashCode() {
		return Objects.hash(width, height, redBits, greenBits, blueBits, refreshRate);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%sx%s@%s (%sbit)", width, height, refreshRate, redBits + greenBits + blueBits);
	}

	/**
	 * Разбирает строку вида {@code "1920x1080@60:24"} в {@link VideoMode}.
	 * Частота обновления по умолчанию — 60 Гц, глубина цвета — 24 бит.
	 *
	 * @param string строка с параметрами видеорежима или {@code null}
	 * @return распознанный видеорежим или пустой {@link Optional}
	 */
	public static Optional<VideoMode> fromString(@Nullable String string) {
		if (string == null) {
			return Optional.empty();
		}

		try {
			Matcher matcher = PATTERN.matcher(string);
			if (!matcher.matches()) {
				return Optional.empty();
			}

			int parsedWidth = Integer.parseInt(matcher.group(1));
			int parsedHeight = Integer.parseInt(matcher.group(2));
			String refreshGroup = matcher.group(3);
			int parsedRefreshRate = refreshGroup == null ? DEFAULT_REFRESH_RATE : Integer.parseInt(refreshGroup);
			String bitDepthGroup = matcher.group(4);
			int bitDepth = bitDepthGroup == null ? DEFAULT_BIT_DEPTH : Integer.parseInt(bitDepthGroup);
			int bitsPerChannel = bitDepth / 3;
			return Optional.of(new VideoMode(parsedWidth, parsedHeight, bitsPerChannel, bitsPerChannel, bitsPerChannel, parsedRefreshRate));
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	public String asString() {
		return String.format(Locale.ROOT, "%sx%s@%s:%s", width, height, refreshRate, redBits + greenBits + blueBits);
	}
}
