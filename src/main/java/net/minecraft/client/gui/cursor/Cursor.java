package net.minecraft.client.gui.cursor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

/**
 * Обёртка над GLFW-курсором. Хранит нативный дескриптор и применяет курсор к окну.
 */
@Environment(EnvType.CLIENT)
public class Cursor {

	public static final Cursor DEFAULT = new Cursor("default", 0L);

	private final String name;
	private final long handle;

	private Cursor(String name, long handle) {
		this.name = name;
		this.handle = handle;
	}

	public void applyTo(Window window) {
		GLFW.glfwSetCursor(window.getHandle(), handle);
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Создаёт курсор по стандартному GLFW-идентификатору.
	 * Если GLFW не поддерживает данный тип курсора, возвращает {@code fallback}.
	 */
	public static Cursor createStandard(int glfwShape, String name, Cursor fallback) {
		long nativeHandle = GLFW.glfwCreateStandardCursor(glfwShape);
		return nativeHandle == 0L ? fallback : new Cursor(name, nativeHandle);
	}
}
