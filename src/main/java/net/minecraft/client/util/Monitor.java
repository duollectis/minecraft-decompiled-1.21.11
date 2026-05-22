package net.minecraft.client.util;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Представляет физический монитор с набором поддерживаемых видеорежимов.
 * Фильтрует режимы с глубиной цвета менее {@code MIN_COLOR_BITS} бит на канал.
 */
@Environment(EnvType.CLIENT)
public final class Monitor {

	private static final int MIN_COLOR_BITS = 8;

	private final long handle;
	private final List<VideoMode> videoModes;
	private VideoMode currentVideoMode;
	private int x;
	private int y;

	public Monitor(long handle) {
		this.handle = handle;
		videoModes = Lists.newArrayList();
		populateVideoModes();
	}

	/**
	 * Обновляет список доступных видеорежимов и текущую позицию монитора.
	 * Фильтрует режимы с глубиной цвета менее 8 бит на канал.
	 */
	public void populateVideoModes() {
		videoModes.clear();
		Buffer buffer = GLFW.glfwGetVideoModes(handle);

		for (int index = buffer.limit() - 1; index >= 0; index--) {
			buffer.position(index);
			VideoMode videoMode = new VideoMode(buffer);
			if (videoMode.getRedBits() >= MIN_COLOR_BITS
				&& videoMode.getGreenBits() >= MIN_COLOR_BITS
				&& videoMode.getBlueBits() >= MIN_COLOR_BITS
			) {
				videoModes.add(videoMode);
			}
		}

		int[] xPos = new int[1];
		int[] yPos = new int[1];
		GLFW.glfwGetMonitorPos(handle, xPos, yPos);
		x = xPos[0];
		y = yPos[0];

		GLFWVidMode currentMode = GLFW.glfwGetVideoMode(handle);
		currentVideoMode = new VideoMode(currentMode);
	}

	/**
	 * Ищет видеорежим, совпадающий с запрошенным, или возвращает текущий режим монитора.
	 *
	 * @param requested запрошенный видеорежим (может быть пустым)
	 * @return найденный или текущий видеорежим
	 */
	public VideoMode findClosestVideoMode(Optional<VideoMode> requested) {
		if (requested.isPresent()) {
			VideoMode target = requested.get();
			for (VideoMode mode : videoModes) {
				if (mode.equals(target)) {
					return mode;
				}
			}
		}

		return getCurrentVideoMode();
	}

	public int findClosestVideoModeIndex(VideoMode videoMode) {
		return videoModes.indexOf(videoMode);
	}

	public VideoMode getCurrentVideoMode() {
		return currentVideoMode;
	}

	public int getViewportX() {
		return x;
	}

	public int getViewportY() {
		return y;
	}

	public VideoMode getVideoMode(int index) {
		return videoModes.get(index);
	}

	public int getVideoModeCount() {
		return videoModes.size();
	}

	public long getHandle() {
		return handle;
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "Monitor[%s %sx%s %s]", handle, x, y, currentVideoMode);
	}
}
