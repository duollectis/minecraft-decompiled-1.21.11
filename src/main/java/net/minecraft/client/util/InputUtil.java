package net.minecraft.client.util;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Утилитарный класс для работы с вводом через GLFW.
 * Содержит константы кодов клавиш/кнопок мыши, методы регистрации коллбэков
 * и вложенные типы {@link Key} и {@link Type} для типобезопасной работы с вводом.
 */
@Environment(EnvType.CLIENT)
public class InputUtil {

	private static final @Nullable MethodHandle GLFW_RAW_MOUSE_MOTION_SUPPORTED_HANDLE;
	private static final int GLFW_RAW_MOUSE_MOTION;

	public static final int GLFW_KEY_0 = 48;
	public static final int GLFW_KEY_1 = 49;
	public static final int GLFW_KEY_2 = 50;
	public static final int GLFW_KEY_3 = 51;
	public static final int GLFW_KEY_4 = 52;
	public static final int GLFW_KEY_5 = 53;
	public static final int GLFW_KEY_6 = 54;
	public static final int GLFW_KEY_7 = 55;
	public static final int GLFW_KEY_8 = 56;
	public static final int GLFW_KEY_9 = 57;
	public static final int GLFW_KEY_A = 65;
	public static final int GLFW_KEY_B = 66;
	public static final int GLFW_KEY_C = 67;
	public static final int GLFW_KEY_D = 68;
	public static final int GLFW_KEY_E = 69;
	public static final int GLFW_KEY_F = 70;
	public static final int GLFW_KEY_G = 71;
	public static final int GLFW_KEY_H = 72;
	public static final int GLFW_KEY_I = 73;
	public static final int GLFW_KEY_J = 74;
	public static final int GLFW_KEY_K = 75;
	public static final int GLFW_KEY_L = 76;
	public static final int GLFW_KEY_M = 77;
	public static final int GLFW_KEY_N = 78;
	public static final int GLFW_KEY_O = 79;
	public static final int GLFW_KEY_P = 80;
	public static final int GLFW_KEY_Q = 81;
	public static final int GLFW_KEY_R = 82;
	public static final int GLFW_KEY_S = 83;
	public static final int GLFW_KEY_T = 84;
	public static final int GLFW_KEY_U = 85;
	public static final int GLFW_KEY_V = 86;
	public static final int GLFW_KEY_W = 87;
	public static final int GLFW_KEY_X = 88;
	public static final int GLFW_KEY_Y = 89;
	public static final int GLFW_KEY_Z = 90;
	public static final int GLFW_KEY_F1 = 290;
	public static final int GLFW_KEY_F2 = 291;
	public static final int GLFW_KEY_F3 = 292;
	public static final int GLFW_KEY_F4 = 293;
	public static final int GLFW_KEY_F5 = 294;
	public static final int GLFW_KEY_F6 = 295;
	public static final int GLFW_KEY_F7 = 296;
	public static final int GLFW_KEY_F8 = 297;
	public static final int GLFW_KEY_F9 = 298;
	public static final int GLFW_KEY_F10 = 299;
	public static final int GLFW_KEY_F11 = 300;
	public static final int GLFW_KEY_F12 = 301;
	public static final int GLFW_KEY_F13 = 302;
	public static final int GLFW_KEY_F14 = 303;
	public static final int GLFW_KEY_F15 = 304;
	public static final int GLFW_KEY_F16 = 305;
	public static final int GLFW_KEY_F17 = 306;
	public static final int GLFW_KEY_F18 = 307;
	public static final int GLFW_KEY_F19 = 308;
	public static final int GLFW_KEY_F20 = 309;
	public static final int GLFW_KEY_F21 = 310;
	public static final int GLFW_KEY_F22 = 311;
	public static final int GLFW_KEY_F23 = 312;
	public static final int GLFW_KEY_F24 = 313;
	public static final int GLFW_KEY_F25 = 314;
	public static final int GLFW_KEY_NUM_LOCK = 282;
	public static final int GLFW_KEY_KP_0 = 320;
	public static final int GLFW_KEY_KP_1 = 321;
	public static final int GLFW_KEY_KP_2 = 322;
	public static final int GLFW_KEY_KP_3 = 323;
	public static final int GLFW_KEY_KP_4 = 324;
	public static final int GLFW_KEY_KP_5 = 325;
	public static final int GLFW_KEY_KP_6 = 326;
	public static final int GLFW_KEY_KP_7 = 327;
	public static final int GLFW_KEY_KP_8 = 328;
	public static final int GLFW_KEY_KP_9 = 329;
	public static final int GLFW_KEY_KP_DECIMAL = 330;
	public static final int GLFW_KEY_KP_DIVIDE = 331;
	public static final int GLFW_KEY_KP_MULTIPLY = 332;
	public static final int GLFW_KEY_KP_SUBTRACT = 333;
	public static final int GLFW_KEY_KP_ADD = 334;
	public static final int GLFW_KEY_KP_ENTER = 335;
	public static final int GLFW_KEY_KP_EQUAL = 336;
	public static final int GLFW_KEY_DOWN = 264;
	public static final int GLFW_KEY_LEFT = 263;
	public static final int GLFW_KEY_RIGHT = 262;
	public static final int GLFW_KEY_UP = 265;
	public static final int GLFW_KEY_APOSTROPHE = 39;
	public static final int GLFW_KEY_BACKSLASH = 92;
	public static final int GLFW_KEY_COMMA = 44;
	public static final int GLFW_KEY_EQUAL = 61;
	public static final int GLFW_KEY_GRAVE_ACCENT = 96;
	public static final int GLFW_KEY_LEFT_BRACKET = 91;
	public static final int GLFW_KEY_MINUS = 45;
	public static final int GLFW_KEY_PERIOD = 46;
	public static final int GLFW_KEY_RIGHT_BRACKET = 93;
	public static final int GLFW_KEY_SEMICOLON = 59;
	public static final int GLFW_KEY_SLASH = 47;
	public static final int GLFW_KEY_SPACE = 32;
	public static final int GLFW_KEY_TAB = 258;
	public static final int GLFW_KEY_LEFT_ALT = 342;
	public static final int GLFW_KEY_LEFT_CONTROL = 341;
	public static final int GLFW_KEY_LEFT_SHIFT = 340;
	public static final int GLFW_KEY_LEFT_SUPER = 343;
	public static final int GLFW_KEY_RIGHT_ALT = 346;
	public static final int GLFW_KEY_RIGHT_CONTROL = 345;
	public static final int GLFW_KEY_RIGHT_SHIFT = 344;
	public static final int GLFW_KEY_RIGHT_SUPER = 347;
	public static final int GLFW_KEY_ENTER = 257;
	public static final int GLFW_KEY_ESCAPE = 256;
	public static final int GLFW_KEY_BACKSPACE = 259;
	public static final int GLFW_KEY_DELETE = 261;
	public static final int GLFW_KEY_END = 269;
	public static final int GLFW_KEY_HOME = 268;
	public static final int GLFW_KEY_INSERT = 260;
	public static final int GLFW_KEY_PAGE_DOWN = 267;
	public static final int GLFW_KEY_PAGE_UP = 266;
	public static final int GLFW_KEY_CAPS_LOCK = 280;
	public static final int GLFW_KEY_PAUSE = 284;
	public static final int GLFW_KEY_SCROLL_LOCK = 281;
	public static final int GLFW_KEY_PRINT_SCREEN = 283;
	public static final int GLFW_KEY_MENU = 348;
	public static final int GLFW_KEY_WORLD_1 = 161;
	public static final int GLFW_KEY_WORLD_2 = 162;
	public static final int GLFW_PRESS = 1;
	public static final int GLFW_RELEASE = 0;
	public static final int GLFW_REPEAT = 2;
	public static final int GLFW_MOUSE_BUTTON_LEFT = 0;
	public static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
	public static final int GLFW_MOUSE_BUTTON_MIDDLE = 2;
	public static final int GLFW_MOUSE_BUTTON_4 = 3;
	public static final int GLFW_MOUSE_BUTTON_5 = 4;
	public static final int GLFW_MOUSE_BUTTON_6 = 5;
	public static final int GLFW_MOUSE_BUTTON_7 = 6;
	public static final int GLFW_MOUSE_BUTTON_1 = 0;
	public static final int GLFW_MOD_SHIFT = 1;
	public static final int GLFW_MOD_CONTROL = 2;
	public static final int GLFW_MOD_ALT = 4;
	public static final int GLFW_MOD_SUPER = 8;
	public static final int GLFW_MOD_CAPS_LOCK = 16;
	public static final int GLFW_MOD_NUM_LOCK = 32;
	public static final int GLFW_CURSOR = 208897;
	public static final int GLFW_CURSOR_DISABLED = 212995;
	public static final int GLFW_CURSOR_NORMAL = 212993;

	public static final Key UNKNOWN_KEY;

	/**
	 * Преобразует {@link KeyInput} в {@link Key}, предпочитая scancode при отсутствии keysym.
	 *
	 * @param key входное событие клавиши
	 * @return соответствующий {@link Key}
	 */
	public static Key fromKeyCode(KeyInput key) {
		return key.key() == -1
			? Type.SCANCODE.createFromCode(key.scancode())
			: Type.KEYSYM.createFromCode(key.key());
	}

	/**
	 * Разбирает строку вида {@code "key.keyboard.a"} или {@code "key.mouse.left"} в {@link Key}.
	 *
	 * @param translationKey строковый ключ перевода
	 * @return соответствующий {@link Key}
	 * @throws IllegalArgumentException если ключ не распознан
	 */
	public static Key fromTranslationKey(String translationKey) {
		if (Key.KEYS.containsKey(translationKey)) {
			return Key.KEYS.get(translationKey);
		}

		for (Type type : Type.values()) {
			if (!translationKey.startsWith(type.name)) {
				continue;
			}

			String codeString = translationKey.substring(type.name.length() + 1);
			int code = Integer.parseInt(codeString);
			if (type == Type.MOUSE) {
				code--;
			}

			return type.createFromCode(code);
		}

		throw new IllegalArgumentException("Unknown key name: " + translationKey);
	}

	public static boolean isKeyPressed(Window window, int code) {
		return GLFW.glfwGetKey(window.getHandle(), code) == GLFW_PRESS;
	}

	public static void setKeyboardCallbacks(
		Window window,
		GLFWKeyCallbackI keyCallback,
		GLFWCharModsCallbackI charModsCallback
	) {
		GLFW.glfwSetKeyCallback(window.getHandle(), keyCallback);
		GLFW.glfwSetCharModsCallback(window.getHandle(), charModsCallback);
	}

	public static void setMouseCallbacks(
		Window window,
		GLFWCursorPosCallbackI cursorPosCallback,
		GLFWMouseButtonCallbackI mouseButtonCallback,
		GLFWScrollCallbackI scrollCallback,
		GLFWDropCallbackI dropCallback
	) {
		GLFW.glfwSetCursorPosCallback(window.getHandle(), cursorPosCallback);
		GLFW.glfwSetMouseButtonCallback(window.getHandle(), mouseButtonCallback);
		GLFW.glfwSetScrollCallback(window.getHandle(), scrollCallback);
		GLFW.glfwSetDropCallback(window.getHandle(), dropCallback);
	}

	public static void setCursorParameters(Window window, int inputModeValue, double x, double y) {
		GLFW.glfwSetCursorPos(window.getHandle(), x, y);
		GLFW.glfwSetInputMode(window.getHandle(), GLFW_CURSOR, inputModeValue);
	}

	/**
	 * Проверяет поддержку raw mouse motion через рефлексию — метод появился в GLFW 3.3,
	 * поэтому используется MethodHandle для совместимости со старыми версиями.
	 *
	 * @return {@code true}, если raw mouse motion поддерживается
	 */
	public static boolean isRawMouseMotionSupported() {
		try {
			return GLFW_RAW_MOUSE_MOTION_SUPPORTED_HANDLE != null
				&& (boolean) GLFW_RAW_MOUSE_MOTION_SUPPORTED_HANDLE.invokeExact();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public static void setRawMouseMotionMode(Window window, boolean enabled) {
		if (isRawMouseMotionSupported()) {
			GLFW.glfwSetInputMode(window.getHandle(), GLFW_RAW_MOUSE_MOTION, enabled ? 1 : 0);
		}
	}

	static {
		Lookup lookup = MethodHandles.lookup();
		MethodType booleanMethod = MethodType.methodType(boolean.class);
		MethodHandle supportedHandle = null;
		int rawMouseMotionConstant = 0;

		try {
			supportedHandle = lookup.findStatic(GLFW.class, "glfwRawMouseMotionSupported", booleanMethod);
			MethodHandle rawMotionGetter = lookup.findStaticGetter(GLFW.class, "GLFW_RAW_MOUSE_MOTION", int.class);
			rawMouseMotionConstant = (int) rawMotionGetter.invokeExact();
		} catch (NoSuchFieldException | NoSuchMethodException ignored) {
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}

		GLFW_RAW_MOUSE_MOTION_SUPPORTED_HANDLE = supportedHandle;
		GLFW_RAW_MOUSE_MOTION = rawMouseMotionConstant;
		UNKNOWN_KEY = Type.KEYSYM.createFromCode(-1);
	}

	@Environment(EnvType.CLIENT)
	public static final class Key {

		static final Map<String, Key> KEYS = Maps.newHashMap();

		private final String translationKey;
		private final Type type;
		private final int code;
		private final Supplier<Text> localizedText;

		Key(String translationKey, Type type, int code) {
			this.translationKey = translationKey;
			this.type = type;
			this.code = code;
			localizedText = Suppliers.memoize(() -> type.textTranslator.apply(code, translationKey));
			KEYS.put(translationKey, this);
		}

		public Type getCategory() {
			return type;
		}

		public int getCode() {
			return code;
		}

		public String getTranslationKey() {
			return translationKey;
		}

		public Text getLocalizedText() {
			return localizedText.get();
		}

		/**
		 * Возвращает цифровое значение клавиши, если это цифровая клавиша (0–9) или numpad (0–9).
		 *
		 * @return {@link OptionalInt} с цифрой или пустой, если клавиша не цифровая
		 */
		public OptionalInt toInt() {
			if (code >= GLFW_KEY_0 && code <= GLFW_KEY_9) {
				return OptionalInt.of(code - GLFW_KEY_0);
			}

			return code >= GLFW_KEY_KP_0 && code <= GLFW_KEY_KP_9
				? OptionalInt.of(code - GLFW_KEY_KP_0)
				: OptionalInt.empty();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Key key = (Key) o;
			return code == key.code && type == key.type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, code);
		}

		@Override
		public String toString() {
			return translationKey;
		}
	}

	@Retention(RetentionPolicy.CLASS)
	@Target({
		ElementType.FIELD,
		ElementType.PARAMETER,
		ElementType.LOCAL_VARIABLE,
		ElementType.METHOD,
		ElementType.TYPE_USE
	})
	@Environment(EnvType.CLIENT)
	public @interface Keycode {
	}

	@Environment(EnvType.CLIENT)
	public enum Type {
		KEYSYM(
			"key.keyboard",
			(keyCode, translationKey) -> {
				if ("key.keyboard.unknown".equals(translationKey)) {
					return Text.translatable(translationKey);
				}

				String keyName = GLFW.glfwGetKeyName(keyCode, -1);
				return keyName != null
					? Text.literal(keyName.toUpperCase(Locale.ROOT))
					: Text.translatable(translationKey);
			}
		),
		SCANCODE(
			"scancode",
			(scanCode, translationKey) -> {
				String keyName = GLFW.glfwGetKeyName(-1, scanCode);
				return keyName != null ? Text.literal(keyName) : Text.translatable(translationKey);
			}
		),
		MOUSE(
			"key.mouse",
			(buttonCode, translationKey) -> Language.getInstance().hasTranslation(translationKey)
				? Text.translatable(translationKey)
				: Text.translatable("key.mouse", buttonCode + 1)
		);

		private static final String UNKNOWN_TRANSLATION_KEY = "key.keyboard.unknown";

		private final Int2ObjectMap<Key> map = new Int2ObjectOpenHashMap();
		final String name;
		final BiFunction<Integer, String, Text> textTranslator;

		Type(String name, BiFunction<Integer, String, Text> textTranslator) {
			this.name = name;
			this.textTranslator = textTranslator;
		}

		/**
		 * Возвращает или создаёт {@link Key} для заданного кода.
		 * Для мыши код смещается на +1 в строковом представлении (GLFW нумерует с 0, UI — с 1).
		 *
		 * @param code числовой код клавиши/кнопки
		 * @return соответствующий {@link Key}
		 */
		public Key createFromCode(int code) {
			return (Key) map.computeIfAbsent(code, rawCode -> {
				int displayCode = (this == MOUSE) ? rawCode + 1 : rawCode;
				String keyTranslationKey = name + "." + displayCode;
				return new Key(keyTranslationKey, this, rawCode);
			});
		}

		private static void mapKey(Type type, String translationKey, int keyCode) {
			Key key = new Key(translationKey, type, keyCode);
			type.map.put(keyCode, key);
		}

		static {
			mapKey(KEYSYM, "key.keyboard.unknown", -1);
			mapKey(MOUSE, "key.mouse.left", 0);
			mapKey(MOUSE, "key.mouse.right", 1);
			mapKey(MOUSE, "key.mouse.middle", 2);
			mapKey(MOUSE, "key.mouse.4", 3);
			mapKey(MOUSE, "key.mouse.5", 4);
			mapKey(MOUSE, "key.mouse.6", 5);
			mapKey(MOUSE, "key.mouse.7", 6);
			mapKey(MOUSE, "key.mouse.8", 7);
			mapKey(KEYSYM, "key.keyboard.0", GLFW_KEY_0);
			mapKey(KEYSYM, "key.keyboard.1", GLFW_KEY_1);
			mapKey(KEYSYM, "key.keyboard.2", GLFW_KEY_2);
			mapKey(KEYSYM, "key.keyboard.3", GLFW_KEY_3);
			mapKey(KEYSYM, "key.keyboard.4", GLFW_KEY_4);
			mapKey(KEYSYM, "key.keyboard.5", GLFW_KEY_5);
			mapKey(KEYSYM, "key.keyboard.6", GLFW_KEY_6);
			mapKey(KEYSYM, "key.keyboard.7", GLFW_KEY_7);
			mapKey(KEYSYM, "key.keyboard.8", GLFW_KEY_8);
			mapKey(KEYSYM, "key.keyboard.9", GLFW_KEY_9);
			mapKey(KEYSYM, "key.keyboard.a", GLFW_KEY_A);
			mapKey(KEYSYM, "key.keyboard.b", GLFW_KEY_B);
			mapKey(KEYSYM, "key.keyboard.c", GLFW_KEY_C);
			mapKey(KEYSYM, "key.keyboard.d", GLFW_KEY_D);
			mapKey(KEYSYM, "key.keyboard.e", GLFW_KEY_E);
			mapKey(KEYSYM, "key.keyboard.f", GLFW_KEY_F);
			mapKey(KEYSYM, "key.keyboard.g", GLFW_KEY_G);
			mapKey(KEYSYM, "key.keyboard.h", GLFW_KEY_H);
			mapKey(KEYSYM, "key.keyboard.i", GLFW_KEY_I);
			mapKey(KEYSYM, "key.keyboard.j", GLFW_KEY_J);
			mapKey(KEYSYM, "key.keyboard.k", GLFW_KEY_K);
			mapKey(KEYSYM, "key.keyboard.l", GLFW_KEY_L);
			mapKey(KEYSYM, "key.keyboard.m", GLFW_KEY_M);
			mapKey(KEYSYM, "key.keyboard.n", GLFW_KEY_N);
			mapKey(KEYSYM, "key.keyboard.o", GLFW_KEY_O);
			mapKey(KEYSYM, "key.keyboard.p", GLFW_KEY_P);
			mapKey(KEYSYM, "key.keyboard.q", GLFW_KEY_Q);
			mapKey(KEYSYM, "key.keyboard.r", GLFW_KEY_R);
			mapKey(KEYSYM, "key.keyboard.s", GLFW_KEY_S);
			mapKey(KEYSYM, "key.keyboard.t", GLFW_KEY_T);
			mapKey(KEYSYM, "key.keyboard.u", GLFW_KEY_U);
			mapKey(KEYSYM, "key.keyboard.v", GLFW_KEY_V);
			mapKey(KEYSYM, "key.keyboard.w", GLFW_KEY_W);
			mapKey(KEYSYM, "key.keyboard.x", GLFW_KEY_X);
			mapKey(KEYSYM, "key.keyboard.y", GLFW_KEY_Y);
			mapKey(KEYSYM, "key.keyboard.z", GLFW_KEY_Z);
			mapKey(KEYSYM, "key.keyboard.f1", GLFW_KEY_F1);
			mapKey(KEYSYM, "key.keyboard.f2", GLFW_KEY_F2);
			mapKey(KEYSYM, "key.keyboard.f3", GLFW_KEY_F3);
			mapKey(KEYSYM, "key.keyboard.f4", GLFW_KEY_F4);
			mapKey(KEYSYM, "key.keyboard.f5", GLFW_KEY_F5);
			mapKey(KEYSYM, "key.keyboard.f6", GLFW_KEY_F6);
			mapKey(KEYSYM, "key.keyboard.f7", GLFW_KEY_F7);
			mapKey(KEYSYM, "key.keyboard.f8", GLFW_KEY_F8);
			mapKey(KEYSYM, "key.keyboard.f9", GLFW_KEY_F9);
			mapKey(KEYSYM, "key.keyboard.f10", GLFW_KEY_F10);
			mapKey(KEYSYM, "key.keyboard.f11", GLFW_KEY_F11);
			mapKey(KEYSYM, "key.keyboard.f12", GLFW_KEY_F12);
			mapKey(KEYSYM, "key.keyboard.f13", GLFW_KEY_F13);
			mapKey(KEYSYM, "key.keyboard.f14", GLFW_KEY_F14);
			mapKey(KEYSYM, "key.keyboard.f15", GLFW_KEY_F15);
			mapKey(KEYSYM, "key.keyboard.f16", GLFW_KEY_F16);
			mapKey(KEYSYM, "key.keyboard.f17", GLFW_KEY_F17);
			mapKey(KEYSYM, "key.keyboard.f18", GLFW_KEY_F18);
			mapKey(KEYSYM, "key.keyboard.f19", GLFW_KEY_F19);
			mapKey(KEYSYM, "key.keyboard.f20", GLFW_KEY_F20);
			mapKey(KEYSYM, "key.keyboard.f21", GLFW_KEY_F21);
			mapKey(KEYSYM, "key.keyboard.f22", GLFW_KEY_F22);
			mapKey(KEYSYM, "key.keyboard.f23", GLFW_KEY_F23);
			mapKey(KEYSYM, "key.keyboard.f24", GLFW_KEY_F24);
			mapKey(KEYSYM, "key.keyboard.f25", GLFW_KEY_F25);
			mapKey(KEYSYM, "key.keyboard.num.lock", GLFW_KEY_NUM_LOCK);
			mapKey(KEYSYM, "key.keyboard.keypad.0", GLFW_KEY_KP_0);
			mapKey(KEYSYM, "key.keyboard.keypad.1", GLFW_KEY_KP_1);
			mapKey(KEYSYM, "key.keyboard.keypad.2", GLFW_KEY_KP_2);
			mapKey(KEYSYM, "key.keyboard.keypad.3", GLFW_KEY_KP_3);
			mapKey(KEYSYM, "key.keyboard.keypad.4", GLFW_KEY_KP_4);
			mapKey(KEYSYM, "key.keyboard.keypad.5", GLFW_KEY_KP_5);
			mapKey(KEYSYM, "key.keyboard.keypad.6", GLFW_KEY_KP_6);
			mapKey(KEYSYM, "key.keyboard.keypad.7", GLFW_KEY_KP_7);
			mapKey(KEYSYM, "key.keyboard.keypad.8", GLFW_KEY_KP_8);
			mapKey(KEYSYM, "key.keyboard.keypad.9", GLFW_KEY_KP_9);
			mapKey(KEYSYM, "key.keyboard.keypad.add", GLFW_KEY_KP_ADD);
			mapKey(KEYSYM, "key.keyboard.keypad.decimal", GLFW_KEY_KP_DECIMAL);
			mapKey(KEYSYM, "key.keyboard.keypad.enter", GLFW_KEY_KP_ENTER);
			mapKey(KEYSYM, "key.keyboard.keypad.equal", GLFW_KEY_KP_EQUAL);
			mapKey(KEYSYM, "key.keyboard.keypad.multiply", GLFW_KEY_KP_MULTIPLY);
			mapKey(KEYSYM, "key.keyboard.keypad.divide", GLFW_KEY_KP_DIVIDE);
			mapKey(KEYSYM, "key.keyboard.keypad.subtract", GLFW_KEY_KP_SUBTRACT);
			mapKey(KEYSYM, "key.keyboard.down", GLFW_KEY_DOWN);
			mapKey(KEYSYM, "key.keyboard.left", GLFW_KEY_LEFT);
			mapKey(KEYSYM, "key.keyboard.right", GLFW_KEY_RIGHT);
			mapKey(KEYSYM, "key.keyboard.up", GLFW_KEY_UP);
			mapKey(KEYSYM, "key.keyboard.apostrophe", GLFW_KEY_APOSTROPHE);
			mapKey(KEYSYM, "key.keyboard.backslash", GLFW_KEY_BACKSLASH);
			mapKey(KEYSYM, "key.keyboard.comma", GLFW_KEY_COMMA);
			mapKey(KEYSYM, "key.keyboard.equal", GLFW_KEY_EQUAL);
			mapKey(KEYSYM, "key.keyboard.grave.accent", GLFW_KEY_GRAVE_ACCENT);
			mapKey(KEYSYM, "key.keyboard.left.bracket", GLFW_KEY_LEFT_BRACKET);
			mapKey(KEYSYM, "key.keyboard.minus", GLFW_KEY_MINUS);
			mapKey(KEYSYM, "key.keyboard.period", GLFW_KEY_PERIOD);
			mapKey(KEYSYM, "key.keyboard.right.bracket", GLFW_KEY_RIGHT_BRACKET);
			mapKey(KEYSYM, "key.keyboard.semicolon", GLFW_KEY_SEMICOLON);
			mapKey(KEYSYM, "key.keyboard.slash", GLFW_KEY_SLASH);
			mapKey(KEYSYM, "key.keyboard.space", GLFW_KEY_SPACE);
			mapKey(KEYSYM, "key.keyboard.tab", GLFW_KEY_TAB);
			mapKey(KEYSYM, "key.keyboard.left.alt", GLFW_KEY_LEFT_ALT);
			mapKey(KEYSYM, "key.keyboard.left.control", GLFW_KEY_LEFT_CONTROL);
			mapKey(KEYSYM, "key.keyboard.left.shift", GLFW_KEY_LEFT_SHIFT);
			mapKey(KEYSYM, "key.keyboard.left.win", GLFW_KEY_LEFT_SUPER);
			mapKey(KEYSYM, "key.keyboard.right.alt", GLFW_KEY_RIGHT_ALT);
			mapKey(KEYSYM, "key.keyboard.right.control", GLFW_KEY_RIGHT_CONTROL);
			mapKey(KEYSYM, "key.keyboard.right.shift", GLFW_KEY_RIGHT_SHIFT);
			mapKey(KEYSYM, "key.keyboard.right.win", GLFW_KEY_RIGHT_SUPER);
			mapKey(KEYSYM, "key.keyboard.enter", GLFW_KEY_ENTER);
			mapKey(KEYSYM, "key.keyboard.escape", GLFW_KEY_ESCAPE);
			mapKey(KEYSYM, "key.keyboard.backspace", GLFW_KEY_BACKSPACE);
			mapKey(KEYSYM, "key.keyboard.delete", GLFW_KEY_DELETE);
			mapKey(KEYSYM, "key.keyboard.end", GLFW_KEY_END);
			mapKey(KEYSYM, "key.keyboard.home", GLFW_KEY_HOME);
			mapKey(KEYSYM, "key.keyboard.insert", GLFW_KEY_INSERT);
			mapKey(KEYSYM, "key.keyboard.page.down", GLFW_KEY_PAGE_DOWN);
			mapKey(KEYSYM, "key.keyboard.page.up", GLFW_KEY_PAGE_UP);
			mapKey(KEYSYM, "key.keyboard.caps.lock", GLFW_KEY_CAPS_LOCK);
			mapKey(KEYSYM, "key.keyboard.pause", GLFW_KEY_PAUSE);
			mapKey(KEYSYM, "key.keyboard.scroll.lock", GLFW_KEY_SCROLL_LOCK);
			mapKey(KEYSYM, "key.keyboard.menu", GLFW_KEY_MENU);
			mapKey(KEYSYM, "key.keyboard.print.screen", GLFW_KEY_PRINT_SCREEN);
			mapKey(KEYSYM, "key.keyboard.world.1", GLFW_KEY_WORLD_1);
			mapKey(KEYSYM, "key.keyboard.world.2", GLFW_KEY_WORLD_2);
		}
	}
}
