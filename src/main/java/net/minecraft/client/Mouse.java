package net.minecraft.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.Scroller;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Smoother;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Обработчик событий мыши клиента.
 * Управляет кликами, прокруткой, перетаскиванием файлов и движением курсора.
 */
@Environment(EnvType.CLIENT)
public class Mouse {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Максимальный интервал между кликами для регистрации двойного клика (мс). */
	public static final long DOUBLE_CLICK_INTERVAL_MS = 250L;

	/** Смещение курсора по Y при отображении координат мыши в отладочном HUD. */
	private static final double DEBUG_CURSOR_Y_OFFSET = 8.0;

	/** Базовый множитель чувствительности мыши. */
	private static final double SENSITIVITY_BASE = 0.6;
	/** Смещение чувствительности мыши. */
	private static final double SENSITIVITY_OFFSET = 0.2;
	/** Множитель для режима подзорной трубы. */
	private static final double SPYGLASS_SENSITIVITY_FACTOR = 8.0;

	/** Максимальная скорость полёта в режиме спектатора при прокрутке. */
	private static final float MAX_FLY_SPEED = 0.2F;
	/** Шаг изменения скорости полёта при прокрутке. */
	private static final float FLY_SPEED_SCROLL_STEP = 0.005F;

	private final MinecraftClient client;
	private boolean leftButtonClicked;
	private boolean middleButtonClicked;
	private boolean rightButtonClicked;
	private double x;
	private double y;
	private @Nullable MouseClickTime lastMouseClick;
	@MouseInput.ButtonCode
	protected int lastMouseButton;
	private int controlLeftClicks;
	private @Nullable MouseInput activeButton;
	private boolean hasResolutionChanged = true;
	private int touchHoldTime;
	private double glfwTime;
	private final Smoother cursorXSmoother = new Smoother();
	private final Smoother cursorYSmoother = new Smoother();
	private double cursorDeltaX;
	private double cursorDeltaY;
	private final Scroller scroller;
	private double lastTickTime = Double.MIN_VALUE;
	private boolean cursorLocked;

	public Mouse(MinecraftClient client) {
		this.client = client;
		scroller = new Scroller();
	}

	private void onMouseButton(long window, MouseInput input, @MouseInput.MouseAction int action) {
		Window clientWindow = client.getWindow();
		if (window != clientWindow.getHandle()) {
			return;
		}

		client.getInactivityFpsLimiter().onInput();

		if (client.currentScreen != null) {
			client.setNavigationType(GuiNavigationType.MOUSE);
		}

		boolean pressed = action == InputUtil.GLFW_PRESS;
		MouseInput effectiveInput = modifyMouseInput(input, pressed);

		if (pressed) {
			if (client.options.getTouchscreen().getValue() && touchHoldTime++ > 0) {
				return;
			}

			activeButton = effectiveInput;
			glfwTime = GlfwUtil.getTime();
		} else if (activeButton != null) {
			if (client.options.getTouchscreen().getValue() && --touchHoldTime > 0) {
				return;
			}

			activeButton = null;
		}

		if (client.getOverlay() != null) {
			return;
		}

		if (client.currentScreen == null) {
			if (!cursorLocked && pressed) {
				lockCursor();
				return;
			}

			InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(effectiveInput.button());
			KeyBinding.setKeyPressed(key, pressed);

			if (pressed) {
				KeyBinding.onKeyPressed(key);
			}

			if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
				leftButtonClicked = pressed;
			} else if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_MIDDLE) {
				middleButtonClicked = pressed;
			} else if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT) {
				rightButtonClicked = pressed;
			}

			return;
		}

		double scaledX = getScaledX(clientWindow);
		double scaledY = getScaledY(clientWindow);
		Screen screen = client.currentScreen;
		Click click = new Click(scaledX, scaledY, effectiveInput);

		if (pressed) {
			screen.applyMousePressScrollNarratorDelay();

			try {
				long now = Util.getMeasuringTimeMs();
				boolean isDoubleClick = lastMouseClick != null
					&& now - lastMouseClick.time() < DOUBLE_CLICK_INTERVAL_MS
					&& lastMouseClick.screen() == screen
					&& lastMouseButton == click.button();

				if (screen.mouseClicked(click, isDoubleClick)) {
					lastMouseClick = new MouseClickTime(now, screen);
					lastMouseButton = effectiveInput.button();
					return;
				}
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "mouseClicked event handler");
				screen.addCrashReportSection(crashReport);
				CrashReportSection section = crashReport.addElement("Mouse");
				addCrashReportSection(section, clientWindow);
				section.add("Button", click.button());
				throw new CrashException(crashReport);
			}
		} else {
			try {
				if (screen.mouseReleased(click)) {
					return;
				}
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "mouseReleased event handler");
				screen.addCrashReportSection(crashReport);
				CrashReportSection section = crashReport.addElement("Mouse");
				addCrashReportSection(section, clientWindow);
				section.add("Button", click.button());
				throw new CrashException(crashReport);
			}
		}

		if (client.currentScreen != null || client.getOverlay() != null) {
			return;
		}

		if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
			leftButtonClicked = pressed;
		} else if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_MIDDLE) {
			middleButtonClicked = pressed;
		} else if (effectiveInput.button() == InputUtil.GLFW_MOUSE_BUTTON_RIGHT) {
			rightButtonClicked = pressed;
		}

		InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(effectiveInput.button());
		KeyBinding.setKeyPressed(key, pressed);

		if (pressed) {
			KeyBinding.onKeyPressed(key);
		}
	}

	/**
	 * Преобразует левый клик с зажатым Ctrl в правый клик на платформах с длинным нажатием.
	 * Используется для поддержки однокнопочных мышей (macOS).
	 *
	 * @param input   исходное событие мыши
	 * @param pressed {@code true} при нажатии, {@code false} при отпускании
	 * @return модифицированное или исходное событие мыши
	 */
	private MouseInput modifyMouseInput(MouseInput input, boolean pressed) {
		if (!SystemKeycodes.USE_LONG_LEFT_PRESS || input.button() != InputUtil.GLFW_MOUSE_BUTTON_LEFT) {
			return input;
		}

		if (pressed) {
			if ((input.modifiers() & InputUtil.GLFW_MOD_CONTROL) == InputUtil.GLFW_MOD_CONTROL) {
				controlLeftClicks++;
				return new MouseInput(InputUtil.GLFW_MOUSE_BUTTON_RIGHT, input.modifiers());
			}
		} else if (controlLeftClicks > 0) {
			controlLeftClicks--;
			return new MouseInput(InputUtil.GLFW_MOUSE_BUTTON_RIGHT, input.modifiers());
		}

		return input;
	}

	public void addCrashReportSection(CrashReportSection section, Window window) {
		section.add(
			"Mouse location",
			() -> String.format(
				Locale.ROOT,
				"Scaled: (%f, %f). Absolute: (%f, %f)",
				scaleX(window, x),
				scaleY(window, y),
				x,
				y
			)
		);
		section.add(
			"Screen size",
			() -> String.format(
				Locale.ROOT,
				"Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d",
				window.getScaledWidth(),
				window.getScaledHeight(),
				window.getFramebufferWidth(),
				window.getFramebufferHeight(),
				window.getScaleFactor()
			)
		);
	}

	private void onMouseScroll(long window, double horizontal, double vertical) {
		if (window != client.getWindow().getHandle()) {
			return;
		}

		client.getInactivityFpsLimiter().onInput();

		boolean discrete = client.options.getDiscreteMouseScroll().getValue();
		double sensitivity = client.options.getMouseWheelSensitivity().getValue();
		double scaledHorizontal = (discrete ? Math.signum(horizontal) : horizontal) * sensitivity;
		double scaledVertical = (discrete ? Math.signum(vertical) : vertical) * sensitivity;

		if (client.getOverlay() != null) {
			return;
		}

		if (client.currentScreen != null) {
			double scaledX = getScaledX(client.getWindow());
			double scaledY = getScaledY(client.getWindow());
			client.currentScreen.mouseScrolled(scaledX, scaledY, scaledHorizontal, scaledVertical);
			client.currentScreen.applyMousePressScrollNarratorDelay();
			return;
		}

		if (client.player == null) {
			return;
		}

		Vector2i scrollDelta = scroller.update(scaledHorizontal, scaledVertical);
		if (scrollDelta.x == 0 && scrollDelta.y == 0) {
			return;
		}

		int scrollAmount = scrollDelta.y == 0 ? -scrollDelta.x : scrollDelta.y;

		if (client.player.isSpectator()) {
			if (client.inGameHud.getSpectatorHud().isOpen()) {
				client.inGameHud.getSpectatorHud().cycleSlot(-scrollAmount);
			} else {
				float newFlySpeed = MathHelper.clamp(
					client.player.getAbilities().getFlySpeed() + scrollDelta.y * FLY_SPEED_SCROLL_STEP,
					0.0F,
					MAX_FLY_SPEED
				);
				client.player.getAbilities().setFlySpeed(newFlySpeed);
			}
		} else {
			PlayerInventory inventory = client.player.getInventory();
			inventory.setSelectedSlot(Scroller.scrollCycling(
				scrollAmount,
				inventory.getSelectedSlot(),
				PlayerInventory.getHotbarSize()
			));
		}
	}

	private void onFilesDropped(long window, List<Path> paths, int invalidFilesCount) {
		client.getInactivityFpsLimiter().onInput();

		if (client.currentScreen != null) {
			client.currentScreen.onFilesDropped(paths);
		}

		if (invalidFilesCount > 0) {
			SystemToast.addFileDropFailure(client, invalidFilesCount);
		}
	}

	/**
	 * Регистрирует GLFW-коллбэки мыши: позиция курсора, кнопки, прокрутка, перетаскивание файлов.
	 *
	 * @param window окно, для которого устанавливаются коллбэки
	 */
	public void setup(Window window) {
		InputUtil.setMouseCallbacks(
			window,
			(handle, posX, posY) -> client.execute(() -> onCursorPos(handle, posX, posY)),
			(handle, button, action, modifiers) -> {
				MouseInput mouseInput = new MouseInput(button, modifiers);
				client.execute(() -> onMouseButton(handle, mouseInput, action));
			},
			(handle, offsetX, offsetY) -> client.execute(() -> onMouseScroll(handle, offsetX, offsetY)),
			(handle, count, names) -> {
				List<Path> paths = new ArrayList<>(count);
				int invalidCount = 0;

				for (int index = 0; index < count; index++) {
					String pathString = GLFWDropCallback.getName(names, index);

					try {
						paths.add(Paths.get(pathString));
					} catch (InvalidPathException exception) {
						invalidCount++;
						LOGGER.error("Failed to parse path '{}'", pathString, exception);
					}
				}

				if (!paths.isEmpty()) {
					int finalInvalidCount = invalidCount;
					client.execute(() -> onFilesDropped(handle, paths, finalInvalidCount));
				}
			}
		);
	}

	private void onCursorPos(long window, double newX, double newY) {
		if (window != client.getWindow().getHandle()) {
			return;
		}

		if (hasResolutionChanged) {
			x = newX;
			y = newY;
			hasResolutionChanged = false;
			return;
		}

		if (client.isWindowFocused()) {
			cursorDeltaX += newX - x;
			cursorDeltaY += newY - y;
		}

		x = newX;
		y = newY;
	}

	/**
	 * Обновляет состояние мыши за тик: обрабатывает движение курсора, перетаскивание и вращение камеры.
	 */
	public void tick() {
		double now = GlfwUtil.getTime();
		double timeDelta = now - lastTickTime;
		lastTickTime = now;

		if (!client.isWindowFocused()) {
			cursorDeltaX = 0.0;
			cursorDeltaY = 0.0;
			return;
		}

		Screen screen = client.currentScreen;
		boolean hasCursorMoved = cursorDeltaX != 0.0 || cursorDeltaY != 0.0;

		if (hasCursorMoved) {
			client.getInactivityFpsLimiter().onInput();
		}

		if (screen != null && client.getOverlay() == null && hasCursorMoved) {
			Window window = client.getWindow();
			double scaledX = getScaledX(window);
			double scaledY = getScaledY(window);

			try {
				screen.mouseMoved(scaledX, scaledY);
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "mouseMoved event handler");
				screen.addCrashReportSection(crashReport);
				CrashReportSection section = crashReport.addElement("Mouse");
				addCrashReportSection(section, window);
				throw new CrashException(crashReport);
			}

			if (activeButton != null && glfwTime > 0.0) {
				double dragDeltaX = scaleX(window, cursorDeltaX);
				double dragDeltaY = scaleY(window, cursorDeltaY);

				try {
					screen.mouseDragged(new Click(scaledX, scaledY, activeButton), dragDeltaX, dragDeltaY);
				} catch (Throwable throwable) {
					CrashReport crashReport = CrashReport.create(throwable, "mouseDragged event handler");
					screen.addCrashReportSection(crashReport);
					CrashReportSection section = crashReport.addElement("Mouse");
					addCrashReportSection(section, window);
					throw new CrashException(crashReport);
				}
			}

			screen.applyMouseMoveNarratorDelay();
		}

		if (isCursorLocked() && client.player != null) {
			updateMouse(timeDelta);
		}

		cursorDeltaX = 0.0;
		cursorDeltaY = 0.0;
	}

	public static double scaleX(Window window, double rawX) {
		return rawX * window.getScaledWidth() / window.getWidth();
	}

	public double getScaledX(Window window) {
		return scaleX(window, x);
	}

	public static double scaleY(Window window, double rawY) {
		return rawY * window.getScaledHeight() / window.getHeight();
	}

	public double getScaledY(Window window) {
		return scaleY(window, y);
	}

	/**
	 * Применяет накопленное смещение курсора к направлению взгляда игрока.
	 * Поддерживает сглаживание мыши и режим подзорной трубы.
	 *
	 * @param timeDelta время с последнего тика в секундах
	 */
	private void updateMouse(double timeDelta) {
		double rawSensitivity = client.options.getMouseSensitivity().getValue() * SENSITIVITY_BASE + SENSITIVITY_OFFSET;
		double cubicSensitivity = rawSensitivity * rawSensitivity * rawSensitivity;
		double fullSensitivity = cubicSensitivity * SPYGLASS_SENSITIVITY_FACTOR;

		double lookX;
		double lookY;

		if (client.options.smoothCameraEnabled) {
			lookX = cursorXSmoother.smooth(cursorDeltaX * fullSensitivity, timeDelta * fullSensitivity);
			lookY = cursorYSmoother.smooth(cursorDeltaY * fullSensitivity, timeDelta * fullSensitivity);
		} else if (client.options.getPerspective().isFirstPerson() && client.player.isUsingSpyglass()) {
			cursorXSmoother.clear();
			cursorYSmoother.clear();
			lookX = cursorDeltaX * cubicSensitivity;
			lookY = cursorDeltaY * cubicSensitivity;
		} else {
			cursorXSmoother.clear();
			cursorYSmoother.clear();
			lookX = cursorDeltaX * fullSensitivity;
			lookY = cursorDeltaY * fullSensitivity;
		}

		client.getTutorialManager().onUpdateMouse(lookX, lookY);

		if (client.player != null) {
			client.player.changeLookDirection(
				client.options.getInvertMouseX().getValue() ? -lookX : lookX,
				client.options.getInvertMouseY().getValue() ? -lookY : lookY
			);
		}
	}

	public boolean wasLeftButtonClicked() {
		return leftButtonClicked;
	}

	public boolean wasMiddleButtonClicked() {
		return middleButtonClicked;
	}

	public boolean wasRightButtonClicked() {
		return rightButtonClicked;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void onResolutionChanged() {
		hasResolutionChanged = true;
	}

	public boolean isCursorLocked() {
		return cursorLocked;
	}

	/**
	 * Захватывает курсор мыши: скрывает его и блокирует в центре окна.
	 * Вызывается при переходе в игровой режим (закрытие экрана).
	 */
	public void lockCursor() {
		if (!client.isWindowFocused() || cursorLocked) {
			return;
		}

		if (SystemKeycodes.UPDATE_PRESSED_STATE_ON_MOUSE_GRAB) {
			KeyBinding.updatePressedStates();
		}

		cursorLocked = true;
		x = client.getWindow().getWidth() / 2.0;
		y = client.getWindow().getHeight() / 2.0;
		InputUtil.setCursorParameters(client.getWindow(), InputUtil.GLFW_CURSOR_DISABLED, x, y);
		client.setScreen(null);
		client.attackCooldown = 10000;
		hasResolutionChanged = true;
	}

	/**
	 * Освобождает курсор мыши: делает его видимым и возвращает в центр окна.
	 */
	public void unlockCursor() {
		if (!cursorLocked) {
			return;
		}

		cursorLocked = false;
		x = client.getWindow().getWidth() / 2.0;
		y = client.getWindow().getHeight() / 2.0;
		InputUtil.setCursorParameters(client.getWindow(), InputUtil.GLFW_CURSOR_NORMAL, x, y);
	}

	public void setResolutionChanged() {
		hasResolutionChanged = true;
	}

	public void drawScaledPos(TextRenderer textRenderer, DrawContext context) {
		Window window = client.getWindow();
		double scaledX = getScaledX(window);
		double scaledY = getScaledY(window) - DEBUG_CURSOR_Y_OFFSET;
		String coords = String.format(Locale.ROOT, "%.0f,%.0f", scaledX, scaledY);
		context.drawTextWithShadow(textRenderer, coords, (int) scaledX, (int) scaledY, -1);
	}

	@Environment(EnvType.CLIENT)
	record MouseClickTime(long time, Screen screen) {
	}
}
