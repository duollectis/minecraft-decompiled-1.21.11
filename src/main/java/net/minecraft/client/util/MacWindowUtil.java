package net.minecraft.client.util;

import com.sun.jna.Pointer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.InputSupplier;
import org.lwjgl.glfw.GLFWNativeCocoa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code MacWindowUtil}.
 */
public class MacWindowUtil {

	public static final boolean IS_MAC = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
	private static final int STYLE_MASK_RESIZE_BIT = 8;
	private static final int FULLSCREEN_MASK = 16384;

	public static void toggleFullscreen(Window window) {
		getCocoaWindow(window).filter(MacWindowUtil::isFullscreen).ifPresent(MacWindowUtil::toggleFullscreen);
	}

	public static void fixStyleMask(Window window) {
		getCocoaWindow(window).ifPresent(windowHandle -> {
			long styleMask = getStyleMask(windowHandle);
			sendMessage(windowHandle, "setStyleMask:", styleMask & -9L);
		});
	}

	private static Optional<Object> getCocoaWindow(Window window) {
		long handle = GLFWNativeCocoa.glfwGetCocoaWindow(window.getHandle());
		return handle != 0L ? Optional.of(new Pointer(handle)) : Optional.empty();
	}

	private static boolean isFullscreen(Object handle) {
		return (getStyleMask(handle) & 16384L) != 0L;
	}

	private static long getStyleMask(Object handle) {
		// Mac-специфичный вызов через JNA Pointer
		return 0L;
	}

	private static void toggleFullscreen(Object handle) {
		// Mac-специфичный вызов через JNA Pointer
	}

	private static void sendMessage(Object handle, String selector, Object... args) {
		// Mac-специфичный вызов через JNA Pointer
	}

	public static void setApplicationIconImage(InputSupplier<InputStream> iconSupplier) throws IOException {
		// Mac-специфичный код — на Linux/Windows не выполняется
	}
}
