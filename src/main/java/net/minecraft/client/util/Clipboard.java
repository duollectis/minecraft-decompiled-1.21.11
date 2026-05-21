package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.TextVisitFactory;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
/**
 * {@code Clipboard}.
 */
public class Clipboard {

	public static final int GLFW_FORMAT_UNAVAILABLE = 65545;
	private final ByteBuffer clipboardBuffer = BufferUtils.createByteBuffer(8192);

	/**
	 * Get.
	 *
	 * @param window window
	 * @param errorCallback error callback
	 *
	 * @return String — 
	 */
	public String get(Window window, GLFWErrorCallbackI errorCallback) {
		GLFWErrorCallback gLFWErrorCallback = GLFW.glfwSetErrorCallback(errorCallback);
		String string = GLFW.glfwGetClipboardString(window.getHandle());
		string = string != null ? TextVisitFactory.validateSurrogates(string) : "";
		GLFWErrorCallback gLFWErrorCallback2 = GLFW.glfwSetErrorCallback(gLFWErrorCallback);
		if (gLFWErrorCallback2 != null) {
			gLFWErrorCallback2.free();
		}

		return string;
	}

	private static void set(Window window, ByteBuffer clipboardBuffer, byte[] content) {
		clipboardBuffer.clear();
		clipboardBuffer.put(content);
		clipboardBuffer.put((byte) 0);
		clipboardBuffer.flip();
		GLFW.glfwSetClipboardString(window.getHandle(), clipboardBuffer);
	}

	/**
	 * Set.
	 *
	 * @param window window
	 * @param string string
	 */
	public void set(Window window, String string) {
		byte[] bs = string.getBytes(StandardCharsets.UTF_8);
		int i = bs.length + 1;
		if (i < this.clipboardBuffer.capacity()) {
			set(window, this.clipboardBuffer, bs);
		}
		else {
			ByteBuffer byteBuffer = MemoryUtil.memAlloc(i);

			try {
				set(window, byteBuffer, bs);
			}
			finally {
				MemoryUtil.memFree(byteBuffer);
			}
		}
	}
}
