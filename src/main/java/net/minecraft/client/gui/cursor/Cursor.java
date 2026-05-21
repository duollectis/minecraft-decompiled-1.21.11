package net.minecraft.client.gui.cursor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
/**
 * {@code Cursor}.
 */
public class Cursor {

	public static final Cursor DEFAULT = new Cursor("default", 0L);
	private final String name;
	private final long handle;

	private Cursor(String name, long handle) {
		this.name = name;
		this.handle = handle;
	}

	/**
	 * Применяет to.
	 *
	 * @param window window
	 */
	public void applyTo(Window window) {
		GLFW.glfwSetCursor(window.getHandle(), this.handle);
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Создаёт standard.
	 *
	 * @param handle handle
	 * @param name name
	 * @param fallback fallback
	 *
	 * @return Cursor — результат операции
	 */
	public static Cursor createStandard(int handle, String name, Cursor fallback) {
		long l = GLFW.glfwCreateStandardCursor(handle);
		return l == 0L ? fallback : new Cursor(name, l);
	}
}
