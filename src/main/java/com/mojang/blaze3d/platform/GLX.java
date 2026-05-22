package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Утилитарный класс для работы с GLFW и системной информацией.
 * Предоставляет методы инициализации OpenGL-контекста, получения информации
 * о мониторе и процессоре, а также вспомогательные фабричные методы.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class GLX {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static @Nullable String cpuInfo;

	/**
	 * Возвращает частоту обновления монитора, на котором находится окно.
	 * Если окно не привязано к монитору, используется основной монитор.
	 *
	 * @param window окно приложения
	 * @return частота обновления в Гц, или 0 если монитор недоступен
	 */
	public static int _getRefreshRate(Window window) {
		RenderSystem.assertOnRenderThread();
		long monitorHandle = GLFW.glfwGetWindowMonitor(window.getHandle());

		if (monitorHandle == 0L) {
			monitorHandle = GLFW.glfwGetPrimaryMonitor();
		}

		GLFWVidMode videoMode = monitorHandle == 0L ? null : GLFW.glfwGetVideoMode(monitorHandle);
		return videoMode == null ? 0 : videoMode.refreshRate();
	}

	public static String _getLWJGLVersion() {
		return Version.getVersion();
	}

	/**
	 * Инициализирует GLFW и возвращает поставщик времени в наносекундах.
	 * Все ошибки, возникшие до и во время инициализации, логируются.
	 *
	 * @return поставщик текущего времени в наносекундах на основе {@code glfwGetTime}
	 * @throws IllegalStateException если инициализация GLFW завершилась неудачей
	 */
	public static LongSupplier _initGlfw() {
		Window.acceptError((code, message) -> {
			throw new IllegalStateException(
				String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", code, message)
			);
		});

		List<String> errors = Lists.newArrayList();
		GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback((code, pointer) -> {
			String message = pointer == 0L ? "" : MemoryUtil.memUTF8(pointer);
			errors.add(String.format(Locale.ROOT, "GLFW error during init: [0x%X]%s", code, message));
		});

		if (!GLFW.glfwInit()) {
			throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(errors));
		}

		LongSupplier timeSupplier = () -> (long) (GLFW.glfwGetTime() * 1.0E9);

		for (String error : errors) {
			LOGGER.error("GLFW error collected during initialization: {}", error);
		}

		RenderSystem.setErrorCallback(previousCallback);
		return timeSupplier;
	}

	/**
	 * Устанавливает новый колбэк ошибок GLFW, освобождая предыдущий если он существует.
	 *
	 * @param callback новый колбэк ошибок
	 */
	public static void _setGlfwErrorCallback(GLFWErrorCallbackI callback) {
		GLFWErrorCallback previous = GLFW.glfwSetErrorCallback(callback);

		if (previous != null) {
			previous.free();
		}
	}

	public static boolean _shouldClose(Window window) {
		return GLFW.glfwWindowShouldClose(window.getHandle());
	}

	/**
	 * Возвращает строку с информацией о процессоре.
	 * Результат кэшируется после первого вызова.
	 * При ошибке получения информации возвращает {@code "<unknown>"}.
	 */
	public static String _getCpuInfo() {
		if (cpuInfo != null) {
			return cpuInfo;
		}

		cpuInfo = "<unknown>";

		try {
			CentralProcessor processor = new SystemInfo().getHardware().getProcessor();
			cpuInfo = String.format(
				Locale.ROOT,
				"%dx %s",
				processor.getLogicalProcessorCount(),
				processor.getProcessorIdentifier().getName()
			).replaceAll("\\s+", " ");
		} catch (Throwable ignored) {
			// Оставляем значение по умолчанию "<unknown>" при любой ошибке OSHI
		}

		return cpuInfo;
	}

	/** Создаёт объект через фабрику и возвращает его. */
	public static <T> T make(Supplier<T> factory) {
		return factory.get();
	}

	/** Создаёт объект, применяет к нему инициализатор и возвращает его. */
	public static <T> T make(T object, Consumer<T> initializer) {
		initializer.accept(object);
		return object;
	}
}
