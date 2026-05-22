package net.minecraft.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.slf4j.Logger;

/**
 * Отслеживает подключённые мониторы через GLFW-коллбэк и определяет,
 * на каком мониторе преимущественно расположено окно.
 */
@Environment(EnvType.CLIENT)
public class MonitorTracker {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Long2ObjectMap<Monitor> pointerToMonitorMap = new Long2ObjectOpenHashMap();
	private final MonitorFactory monitorFactory;

	public MonitorTracker(MonitorFactory monitorFactory) {
		this.monitorFactory = monitorFactory;
		GLFW.glfwSetMonitorCallback(this::handleMonitorEvent);

		PointerBuffer monitors = GLFW.glfwGetMonitors();
		if (monitors == null) {
			return;
		}

		for (int index = 0; index < monitors.limit(); index++) {
			long pointer = monitors.get(index);
			pointerToMonitorMap.put(pointer, monitorFactory.createMonitor(pointer));
		}
	}

	private void handleMonitorEvent(long monitorPointer, int event) {
		RenderSystem.assertOnRenderThread();
		if (event == GLFW.GLFW_CONNECTED) {
			pointerToMonitorMap.put(monitorPointer, monitorFactory.createMonitor(monitorPointer));
			LOGGER.debug("Monitor {} connected. Current monitors: {}", monitorPointer, pointerToMonitorMap);
		} else {
			pointerToMonitorMap.remove(monitorPointer);
			LOGGER.debug("Monitor {} disconnected. Current monitors: {}", monitorPointer, pointerToMonitorMap);
		}
	}

	public @Nullable Monitor getMonitor(long pointer) {
		return (Monitor) pointerToMonitorMap.get(pointer);
	}

	/**
	 * Определяет монитор, на котором преимущественно расположено окно, по площади пересечения.
	 * При равной площади предпочитает основной монитор системы.
	 *
	 * @param window окно, для которого ищется монитор
	 * @return монитор с наибольшим перекрытием или {@code null}
	 */
	public @Nullable Monitor getMonitor(Window window) {
		long windowMonitor = GLFW.glfwGetWindowMonitor(window.getHandle());
		if (windowMonitor != 0L) {
			return getMonitor(windowMonitor);
		}

		int windowLeft = window.getX();
		int windowRight = windowLeft + window.getWidth();
		int windowTop = window.getY();
		int windowBottom = windowTop + window.getHeight();
		int bestOverlapArea = -1;
		Monitor bestMonitor = null;
		long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
		LOGGER.debug("Selecting monitor - primary: {}, current monitors: {}", primaryMonitor, pointerToMonitorMap);

		for (Monitor monitor : pointerToMonitorMap.values()) {
			int monitorLeft = monitor.getViewportX();
			int monitorRight = monitorLeft + monitor.getCurrentVideoMode().getWidth();
			int monitorTop = monitor.getViewportY();
			int monitorBottom = monitorTop + monitor.getCurrentVideoMode().getHeight();
			int overlapWidth = Math.max(0, clamp(windowRight, monitorLeft, monitorRight) - clamp(windowLeft, monitorLeft, monitorRight));
			int overlapHeight = Math.max(0, clamp(windowBottom, monitorTop, monitorBottom) - clamp(windowTop, monitorTop, monitorBottom));
			int overlapArea = overlapWidth * overlapHeight;

			if (overlapArea > bestOverlapArea) {
				bestMonitor = monitor;
				bestOverlapArea = overlapArea;
			} else if (overlapArea == bestOverlapArea && primaryMonitor == monitor.getHandle()) {
				LOGGER.debug("Primary monitor {} is preferred to monitor {}", monitor, bestMonitor);
				bestMonitor = monitor;
			}
		}

		LOGGER.debug("Selected monitor: {}", bestMonitor);
		return bestMonitor;
	}

	public static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	public void stop() {
		RenderSystem.assertOnRenderThread();
		GLFWMonitorCallback previousCallback = GLFW.glfwSetMonitorCallback(null);
		if (previousCallback != null) {
			previousCallback.free();
		}
	}
}
