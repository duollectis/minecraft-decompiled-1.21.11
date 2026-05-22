package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

/**
 * Утилитарный класс для низкоуровневых операций GLFW.
 */
@Environment(EnvType.CLIENT)
public class GlfwUtil {

	private GlfwUtil() {
	}

	/** Намеренно вызывает крэш JVM через запись по нулевому адресу — только для отладки. */
	public static void makeJvmCrash() {
		MemoryUtil.memSet(0L, 0, 1L);
	}

	public static double getTime() {
		return GLFW.glfwGetTime();
	}
}
