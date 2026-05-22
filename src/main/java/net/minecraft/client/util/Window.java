package net.minecraft.client.util;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Управляет GLFW-окном игры: создание, полноэкранный режим, масштабирование GUI,
 * иконки, VSync и обработка событий изменения размера/позиции.
 */
@Environment(EnvType.CLIENT)
public final class Window implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final int FRAMEBUFFER_WIDTH_SCALE = 320;
	public static final int FRAMEBUFFER_HEIGHT_SCALE = 240;

	// GLFW platform constants
	private static final int GLFW_PLATFORM_WIN32 = 393217;
	private static final int GLFW_PLATFORM_COCOA = 393218;
	private static final int GLFW_PLATFORM_WAYLAND = 393219;
	private static final int GLFW_PLATFORM_X11 = 393220;
	private static final int GLFW_PLATFORM_NULL = 393221;

	// GLFW window hint constants
	private static final int GLFW_CONTEXT_VERSION_MAJOR_HINT = 139266;
	private static final int GLFW_CONTEXT_VERSION_MINOR_HINT = 139267;
	private static final int GLFW_OPENGL_PROFILE_HINT = 139272;
	private static final int GLFW_OPENGL_FORWARD_COMPAT_HINT = 139270;
	private static final int GLFW_CLIENT_API_HINT = 139265;
	private static final int GLFW_OPENGL_DEBUG_CONTEXT_HINT = 139275;
	private static final int GLFW_CLIENT_API_OPENGL = 196609;
	private static final int GLFW_OPENGL_DEBUG_CONTEXT_VALUE = 221185;
	private static final int GLFW_CONTEXT_VERSION_MAJOR_VALUE = 3;
	private static final int GLFW_CONTEXT_VERSION_MINOR_VALUE = 3;
	private static final int GLFW_OPENGL_CORE_PROFILE = 204801;
	private static final int GLFW_TRUE = 1;

	// Default color bits per channel for fullscreen mode
	private static final int DEFAULT_COLOR_BITS = 8;
	private static final int DEFAULT_REFRESH_RATE = 60;
	private static final float MIN_LINE_WIDTH = 2.5F;
	private static final int REFERENCE_WIDTH_FOR_LINE = 1920;

	private final GLFWErrorCallback errorCallback = GLFWErrorCallback.create(this::logGlError);
	private final WindowEventHandler eventHandler;
	private final MonitorTracker monitorTracker;
	private final long handle;
	private int windowedX;
	private int windowedY;
	private int windowedWidth;
	private int windowedHeight;
	private Optional<VideoMode> fullscreenVideoMode;
	private boolean fullscreen;
	private boolean currentFullscreen;
	private int x;
	private int y;
	private int width;
	private int height;
	private int framebufferWidth;
	private int framebufferHeight;
	private int scaledWidth;
	private int scaledHeight;
	private int scaleFactor;
	private String phase = "";
	private boolean fullscreenVideoModeDirty;
	private boolean vsync;
	private boolean minimized;
	private boolean zeroWidthOrHeight;
	private boolean allowCursorChanges;
	private Cursor cursor = Cursor.DEFAULT;

	public Window(
		WindowEventHandler eventHandler,
		MonitorTracker monitorTracker,
		WindowSettings settings,
		@Nullable String fullscreenVideoModeString,
		String title
	) {
		this.monitorTracker = monitorTracker;
		throwOnGlError();
		setPhase("Pre startup");
		this.eventHandler = eventHandler;

		Optional<VideoMode> parsedVideoMode = VideoMode.fromString(fullscreenVideoModeString);
		if (parsedVideoMode.isPresent()) {
			fullscreenVideoMode = parsedVideoMode;
		} else if (settings.fullscreenWidth().isPresent() && settings.fullscreenHeight().isPresent()) {
			fullscreenVideoMode = Optional.of(new VideoMode(
				settings.fullscreenWidth().getAsInt(),
				settings.fullscreenHeight().getAsInt(),
				DEFAULT_COLOR_BITS,
				DEFAULT_COLOR_BITS,
				DEFAULT_COLOR_BITS,
				DEFAULT_REFRESH_RATE
			));
		} else {
			fullscreenVideoMode = Optional.empty();
		}

		currentFullscreen = fullscreen = settings.fullscreen();
		Monitor primaryMonitor = monitorTracker.getMonitor(GLFW.glfwGetPrimaryMonitor());
		windowedWidth = width = Math.max(settings.width(), 1);
		windowedHeight = height = Math.max(settings.height(), 1);

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW_CLIENT_API_HINT, GLFW_CLIENT_API_OPENGL);
		GLFW.glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT_HINT, GLFW_OPENGL_DEBUG_CONTEXT_VALUE);
		GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR_HINT, GLFW_CONTEXT_VERSION_MAJOR_VALUE);
		GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR_HINT, GLFW_CONTEXT_VERSION_MINOR_VALUE);
		GLFW.glfwWindowHint(GLFW_OPENGL_PROFILE_HINT, GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT_HINT, GLFW_TRUE);

		handle = GLFW.glfwCreateWindow(
			width,
			height,
			title,
			fullscreen && primaryMonitor != null ? primaryMonitor.getHandle() : 0L,
			0L
		);

		if (primaryMonitor != null) {
			VideoMode videoMode = primaryMonitor.findClosestVideoMode(fullscreen ? fullscreenVideoMode : Optional.empty());
			windowedX = x = primaryMonitor.getViewportX() + videoMode.getWidth() / 2 - width / 2;
			windowedY = y = primaryMonitor.getViewportY() + videoMode.getHeight() / 2 - height / 2;
		} else {
			int[] xPos = new int[1];
			int[] yPos = new int[1];
			GLFW.glfwGetWindowPos(handle, xPos, yPos);
			windowedX = x = xPos[0];
			windowedY = y = yPos[0];
		}

		updateWindowRegion();
		updateFramebufferSize();
		GLFW.glfwSetFramebufferSizeCallback(handle, this::onFramebufferSizeChanged);
		GLFW.glfwSetWindowPosCallback(handle, this::onWindowPosChanged);
		GLFW.glfwSetWindowSizeCallback(handle, this::onWindowSizeChanged);
		GLFW.glfwSetWindowFocusCallback(handle, this::onWindowFocusChanged);
		GLFW.glfwSetCursorEnterCallback(handle, this::onCursorEnterChanged);
		GLFW.glfwSetWindowIconifyCallback(handle, this::onMinimizeChanged);
	}

	public static String getGlfwPlatform() {
		int platform = GLFW.glfwGetPlatform();
		return switch (platform) {
			case 0 -> "<error>";
			case GLFW_PLATFORM_WIN32 -> "win32";
			case GLFW_PLATFORM_COCOA -> "cocoa";
			case GLFW_PLATFORM_WAYLAND -> "wayland";
			case GLFW_PLATFORM_X11 -> "x11";
			case GLFW_PLATFORM_NULL -> "null";
			default -> String.format(Locale.ROOT, "unknown (%08X)", platform);
		};
	}

	public int getRefreshRate() {
		RenderSystem.assertOnRenderThread();
		return GLX._getRefreshRate(this);
	}

	public boolean shouldClose() {
		return GLX._shouldClose(this);
	}

	public static void acceptError(BiConsumer<Integer, String> consumer) {
		try (MemoryStack memoryStack = MemoryStack.stackPush()) {
			PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
			int errorCode = GLFW.glfwGetError(pointerBuffer);
			if (errorCode != 0) {
				long descriptionPtr = pointerBuffer.get();
				String description = descriptionPtr == 0L ? "" : MemoryUtil.memUTF8(descriptionPtr);
				consumer.accept(errorCode, description);
			}
		}
	}

	/**
	 * Устанавливает иконку окна в зависимости от платформы.
	 * На Win32/X11 загружает набор PNG-иконок разных размеров.
	 * На macOS устанавливает .icns-файл через {@link MacWindowUtil}.
	 * На Wayland/null иконки не поддерживаются.
	 *
	 * @param resourcePack пак ресурсов с иконками
	 * @param icons        набор иконок для текущего типа сборки
	 * @throws IOException если иконка не найдена в паке ресурсов
	 */
	public void setIcon(ResourcePack resourcePack, Icons icons) throws IOException {
		int platform = GLFW.glfwGetPlatform();
		switch (platform) {
			case GLFW_PLATFORM_WIN32:
			case GLFW_PLATFORM_X11:
				List<InputSupplier<InputStream>> iconSuppliers = icons.getIcons(resourcePack);
				List<ByteBuffer> iconBuffers = new ArrayList<>(iconSuppliers.size());
				try {
					try (MemoryStack memoryStack = MemoryStack.stackPush()) {
						Buffer glfwImages = GLFWImage.malloc(iconSuppliers.size(), memoryStack);
						for (int index = 0; index < iconSuppliers.size(); index++) {
							try (NativeImage nativeImage = NativeImage.read(iconSuppliers.get(index).get())) {
								ByteBuffer pixelBuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
								iconBuffers.add(pixelBuffer);
								pixelBuffer.asIntBuffer().put(nativeImage.copyPixelsAbgr());
								glfwImages.position(index);
								glfwImages.width(nativeImage.getWidth());
								glfwImages.height(nativeImage.getHeight());
								glfwImages.pixels(pixelBuffer);
							}
						}

						GLFW.glfwSetWindowIcon(handle, (Buffer) glfwImages.position(0));
					}
				} finally {
					iconBuffers.forEach(MemoryUtil::memFree);
				}
				break;
			case GLFW_PLATFORM_COCOA:
				MacWindowUtil.setApplicationIconImage(icons.getMacIcon(resourcePack));
				break;
			case GLFW_PLATFORM_WAYLAND:
			case GLFW_PLATFORM_NULL:
				break;
			default:
				LOGGER.warn("Not setting icon for unrecognized platform: {}", platform);
		}
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	private void throwOnGlError() {
		GLFW.glfwSetErrorCallback(Window::throwGlError);
	}

	private static void throwGlError(int error, long description) {
		String message = "GLFW error " + error + ": " + MemoryUtil.memUTF8(description);
		TinyFileDialogs.tinyfd_messageBox(
			"Minecraft",
			message + ".\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).",
			"ok",
			"error",
			false
		);
		throw new GlErroredException(message);
	}

	public void logGlError(int error, long description) {
		RenderSystem.assertOnRenderThread();
		String descriptionText = MemoryUtil.memUTF8(description);
		LOGGER.error("########## GL ERROR ##########");
		LOGGER.error("@ {}", phase);
		LOGGER.error("{}: {}", error, descriptionText);
	}

	public void logOnGlError() {
		GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(errorCallback);
		if (previousCallback != null) {
			previousCallback.free();
		}
	}

	public void setVsync(boolean vsync) {
		RenderSystem.assertOnRenderThread();
		this.vsync = vsync;
		GLFW.glfwSwapInterval(vsync ? 1 : 0);
	}

	@Override
	public void close() {
		RenderSystem.assertOnRenderThread();
		Callbacks.glfwFreeCallbacks(handle);
		errorCallback.close();
		GLFW.glfwDestroyWindow(handle);
		GLFW.glfwTerminate();
	}

	private void onWindowPosChanged(long window, int newX, int newY) {
		x = newX;
		y = newY;
	}

	private void onFramebufferSizeChanged(long window, int newWidth, int newHeight) {
		if (window != handle) {
			return;
		}

		int previousWidth = getFramebufferWidth();
		int previousHeight = getFramebufferHeight();

		if (newWidth == 0 || newHeight == 0) {
			zeroWidthOrHeight = true;
			return;
		}

		zeroWidthOrHeight = false;
		framebufferWidth = newWidth;
		framebufferHeight = newHeight;

		if (getFramebufferWidth() == previousWidth && getFramebufferHeight() == previousHeight) {
			return;
		}

		try {
			eventHandler.onResolutionChanged();
		} catch (Exception exception) {
			CrashReport crashReport = CrashReport.create(exception, "Window resize");
			CrashReportSection section = crashReport.addElement("Window Dimensions");
			section.add("Old", previousWidth + "x" + previousHeight);
			section.add("New", newWidth + "x" + newHeight);
			throw new CrashException(crashReport);
		}
	}

	private void updateFramebufferSize() {
		int[] widthArr = new int[1];
		int[] heightArr = new int[1];
		GLFW.glfwGetFramebufferSize(handle, widthArr, heightArr);
		framebufferWidth = widthArr[0] > 0 ? widthArr[0] : 1;
		framebufferHeight = heightArr[0] > 0 ? heightArr[0] : 1;
	}

	private void onWindowSizeChanged(long window, int newWidth, int newHeight) {
		width = newWidth;
		height = newHeight;
	}

	private void onWindowFocusChanged(long window, boolean focused) {
		if (window == handle) {
			eventHandler.onWindowFocusChanged(focused);
		}
	}

	private void onCursorEnterChanged(long window, boolean entered) {
		if (entered) {
			eventHandler.onCursorEnterChanged();
		}
	}

	private void onMinimizeChanged(long window, boolean isMinimized) {
		minimized = isMinimized;
	}

	public void swapBuffers(@Nullable TracyFrameCapturer capturer) {
		RenderSystem.flipFrame(this, capturer);
		if (fullscreen != currentFullscreen) {
			currentFullscreen = fullscreen;
			updateFullscreen(vsync, capturer);
		}
	}

	public Optional<VideoMode> getFullscreenVideoMode() {
		return fullscreenVideoMode;
	}

	public void setFullscreenVideoMode(Optional<VideoMode> newVideoMode) {
		boolean changed = !newVideoMode.equals(fullscreenVideoMode);
		fullscreenVideoMode = newVideoMode;
		if (changed) {
			fullscreenVideoModeDirty = true;
		}
	}

	public void applyFullscreenVideoMode() {
		if (!fullscreen || !fullscreenVideoModeDirty) {
			return;
		}

		fullscreenVideoModeDirty = false;
		updateWindowRegion();
		eventHandler.onResolutionChanged();
	}

	private void updateWindowRegion() {
		boolean wasFullscreen = GLFW.glfwGetWindowMonitor(handle) != 0L;
		if (fullscreen) {
			Monitor monitor = monitorTracker.getMonitor(this);
			if (monitor == null) {
				LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
				fullscreen = false;
				return;
			}

			if (MacWindowUtil.IS_MAC) {
				MacWindowUtil.toggleFullscreen(this);
			}

			VideoMode videoMode = monitor.findClosestVideoMode(fullscreenVideoMode);
			if (!wasFullscreen) {
				windowedX = x;
				windowedY = y;
				windowedWidth = width;
				windowedHeight = height;
			}

			x = 0;
			y = 0;
			width = videoMode.getWidth();
			height = videoMode.getHeight();
			GLFW.glfwSetWindowMonitor(handle, monitor.getHandle(), x, y, width, height, videoMode.getRefreshRate());

			if (MacWindowUtil.IS_MAC) {
				MacWindowUtil.fixStyleMask(this);
			}
		} else {
			x = windowedX;
			y = windowedY;
			width = windowedWidth;
			height = windowedHeight;
			GLFW.glfwSetWindowMonitor(handle, 0L, x, y, width, height, -1);
		}
	}

	public void toggleFullscreen() {
		fullscreen = !fullscreen;
	}

	public void setWindowedSize(int newWidth, int newHeight) {
		windowedWidth = newWidth;
		windowedHeight = newHeight;
		fullscreen = false;
		updateWindowRegion();
	}

	private void updateFullscreen(boolean enableVsync, @Nullable TracyFrameCapturer capturer) {
		RenderSystem.assertOnRenderThread();
		try {
			updateWindowRegion();
			eventHandler.onResolutionChanged();
			setVsync(enableVsync);
			swapBuffers(capturer);
		} catch (Exception exception) {
			LOGGER.error("Couldn't toggle fullscreen", exception);
		}
	}

	/**
	 * Вычисляет оптимальный масштаб GUI, при котором интерфейс умещается в фреймбуфер.
	 * При {@code forceUnicodeFont} масштаб округляется до чётного для корректного рендеринга шрифта.
	 *
	 * @param guiScale       желаемый масштаб (0 = авто)
	 * @param forceUnicodeFont принудительно чётный масштаб для unicode-шрифта
	 * @return итоговый масштаб
	 */
	public int calculateScaleFactor(int guiScale, boolean forceUnicodeFont) {
		int scale = 1;
		while (
			scale != guiScale
				&& scale < framebufferWidth
				&& scale < framebufferHeight
				&& framebufferWidth / (scale + 1) >= FRAMEBUFFER_WIDTH_SCALE
				&& framebufferHeight / (scale + 1) >= FRAMEBUFFER_HEIGHT_SCALE
		) {
			scale++;
		}

		if (forceUnicodeFont && scale % 2 != 0) {
			scale++;
		}

		return scale;
	}

	public void setScaleFactor(int newScaleFactor) {
		scaleFactor = newScaleFactor;
		double scale = newScaleFactor;
		int rawScaledWidth = (int) (framebufferWidth / scale);
		scaledWidth = framebufferWidth / scale > rawScaledWidth ? rawScaledWidth + 1 : rawScaledWidth;
		int rawScaledHeight = (int) (framebufferHeight / scale);
		scaledHeight = framebufferHeight / scale > rawScaledHeight ? rawScaledHeight + 1 : rawScaledHeight;
	}

	public void setTitle(String title) {
		GLFW.glfwSetWindowTitle(handle, title);
	}

	public long getHandle() {
		return handle;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public boolean isMinimized() {
		return minimized;
	}

	public int getFramebufferWidth() {
		return framebufferWidth;
	}

	public int getFramebufferHeight() {
		return framebufferHeight;
	}

	public void setFramebufferWidth(int framebufferWidth) {
		this.framebufferWidth = framebufferWidth;
	}

	public void setFramebufferHeight(int framebufferHeight) {
		this.framebufferHeight = framebufferHeight;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getScaledWidth() {
		return scaledWidth;
	}

	public int getScaledHeight() {
		return scaledHeight;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getScaleFactor() {
		return scaleFactor;
	}

	public @Nullable Monitor getMonitor() {
		return monitorTracker.getMonitor(this);
	}

	public void setRawMouseMotion(boolean rawMouseMotion) {
		InputUtil.setRawMouseMotionMode(this, rawMouseMotion);
	}

	public void setCloseCallback(Runnable callback) {
		GLFWWindowCloseCallback previousCallback = GLFW.glfwSetWindowCloseCallback(handle, window -> callback.run());
		if (previousCallback != null) {
			previousCallback.free();
		}
	}

	public boolean hasZeroWidthOrHeight() {
		return zeroWidthOrHeight;
	}

	public void setAllowCursorChanges(boolean allowCursorChanges) {
		this.allowCursorChanges = allowCursorChanges;
	}

	public void setCursor(Cursor newCursor) {
		Cursor effectiveCursor = allowCursorChanges ? newCursor : Cursor.DEFAULT;
		if (cursor != effectiveCursor) {
			cursor = effectiveCursor;
			effectiveCursor.applyTo(this);
		}
	}

	public float getMinimumLineWidth() {
		return Math.max(MIN_LINE_WIDTH, getFramebufferWidth() / (float) REFERENCE_WIDTH_FOR_LINE * MIN_LINE_WIDTH);
	}

	@Environment(EnvType.CLIENT)
	public static class GlErroredException extends GlException {

		GlErroredException(String message) {
			super(message);
		}
	}
}
