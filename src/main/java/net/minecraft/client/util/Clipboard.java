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
public class Clipboard {

	public static final int GLFW_FORMAT_UNAVAILABLE = 65545;

	private static final int CLIPBOARD_BUFFER_CAPACITY = 8192;

	private final ByteBuffer clipboardBuffer = BufferUtils.createByteBuffer(CLIPBOARD_BUFFER_CAPACITY);

	/**
	 * Читает текст из системного буфера обмена.
	 * Временно подменяет GLFW-коллбэк ошибок на переданный, чтобы перехватить
	 * ошибку {@code GLFW_FORMAT_UNAVAILABLE} без крэша.
	 *
	 * @param window        окно, для которого запрашивается буфер обмена
	 * @param errorCallback коллбэк для обработки ошибок GLFW во время чтения
	 * @return содержимое буфера обмена или пустая строка, если буфер пуст
	 */
	public String get(Window window, GLFWErrorCallbackI errorCallback) {
		GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(errorCallback);
		String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
		String result = clipboardText != null ? TextVisitFactory.validateSurrogates(clipboardText) : "";
		GLFWErrorCallback restoredCallback = GLFW.glfwSetErrorCallback(previousCallback);
		if (restoredCallback != null) {
			restoredCallback.free();
		}

		return result;
	}

	/**
	 * Записывает текст в системный буфер обмена.
	 * Для строк, умещающихся в предвыделенный буфер, избегает лишней аллокации.
	 *
	 * @param window окно, для которого устанавливается буфер обмена
	 * @param text   текст для записи
	 */
	public void set(Window window, String text) {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		int requiredSize = bytes.length + 1;
		if (requiredSize < clipboardBuffer.capacity()) {
			writeToClipboard(window, clipboardBuffer, bytes);
		} else {
			ByteBuffer tempBuffer = MemoryUtil.memAlloc(requiredSize);
			try {
				writeToClipboard(window, tempBuffer, bytes);
			} finally {
				MemoryUtil.memFree(tempBuffer);
			}
		}
	}

	private static void writeToClipboard(Window window, ByteBuffer buffer, byte[] content) {
		buffer.clear();
		buffer.put(content);
		buffer.put((byte) 0);
		buffer.flip();
		GLFW.glfwSetClipboardString(window.getHandle(), buffer);
	}
}
