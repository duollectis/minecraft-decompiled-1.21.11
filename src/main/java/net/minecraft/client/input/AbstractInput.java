package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Базовый интерфейс для всех типов ввода: клавиатурного ({@link KeyInput})
 * и мышиного ({@link MouseInput}). Предоставляет набор утилитарных методов
 * для проверки нажатых клавиш и модификаторов на основе GLFW-кодов.
 */
@Environment(EnvType.CLIENT)
public interface AbstractInput {

	// ─── GLFW keycodes ──────────────────────────────────────────────────────────
	int KEY_ESCAPE = 256;
	int KEY_ENTER = 257;
	int KEY_TAB = 258;
	int KEY_RIGHT = 262;
	int KEY_LEFT = 263;
	int KEY_DOWN = 264;
	int KEY_UP = 265;
	int KEY_KP_ENTER = 335;
	int KEY_A = 65;
	int KEY_C = 67;
	int KEY_V = 86;
	int KEY_X = 88;
	int KEY_SPACE = 32;
	int KEY_0 = 48;
	int KEY_9 = 57;

	// ─── GLFW modifier bits ──────────────────────────────────────────────────────
	int MOD_SHIFT = 1;
	int MOD_CTRL = 2;
	int MOD_ALT = 4;

	int NOT_A_NUMBER = -1;

	@InputUtil.Keycode
	int getKeycode();

	@AbstractInput.Modifier
	int modifiers();

	default boolean isEnterOrSpace() {
		int key = getKeycode();
		return key == KEY_ENTER || key == KEY_SPACE || key == KEY_KP_ENTER;
	}

	default boolean isEnter() {
		int key = getKeycode();
		return key == KEY_ENTER || key == KEY_KP_ENTER;
	}

	default boolean isEscape() {
		return getKeycode() == KEY_ESCAPE;
	}

	default boolean isLeft() {
		return getKeycode() == KEY_LEFT;
	}

	default boolean isRight() {
		return getKeycode() == KEY_RIGHT;
	}

	default boolean isUp() {
		return getKeycode() == KEY_UP;
	}

	default boolean isDown() {
		return getKeycode() == KEY_DOWN;
	}

	default boolean isTab() {
		return getKeycode() == KEY_TAB;
	}

	/**
	 * Возвращает цифру 0–9, если нажата соответствующая клавиша, иначе {@link #NOT_A_NUMBER}.
	 */
	default int asNumber() {
		int digit = getKeycode() - KEY_0;
		return digit >= 0 && digit <= 9 ? digit : NOT_A_NUMBER;
	}

	default boolean hasAlt() {
		return (modifiers() & MOD_ALT) != 0;
	}

	default boolean hasShift() {
		return (modifiers() & MOD_SHIFT) != 0;
	}

	default boolean hasCtrl() {
		return (modifiers() & MOD_CTRL) != 0;
	}

	default boolean hasCtrlOrCmd() {
		return (modifiers() & SystemKeycodes.CTRL_MOD) != 0;
	}

	default boolean isSelectAll() {
		return getKeycode() == KEY_A && hasCtrlOrCmd() && !hasShift() && !hasAlt();
	}

	default boolean isCopy() {
		return getKeycode() == KEY_C && hasCtrlOrCmd() && !hasShift() && !hasAlt();
	}

	default boolean isPaste() {
		return getKeycode() == KEY_V && hasCtrlOrCmd() && !hasShift() && !hasAlt();
	}

	default boolean isCut() {
		return getKeycode() == KEY_X && hasCtrlOrCmd() && !hasShift() && !hasAlt();
	}

	/** Аннотация-маркер для значений битовой маски модификаторов GLFW. */
	@Retention(RetentionPolicy.CLASS)
	@Target(
			{
					ElementType.FIELD,
					ElementType.PARAMETER,
					ElementType.LOCAL_VARIABLE,
					ElementType.METHOD,
					ElementType.TYPE_USE
			}
	)
	@Environment(EnvType.CLIENT)
	@interface Modifier {
	}
}
