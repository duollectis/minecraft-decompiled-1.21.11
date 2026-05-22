package net.minecraft.client.option;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Привязка клавиши к игровому действию.
 * <p>
 * Хранит текущую и дефолтную клавишу, счётчик нажатий для обработки событий
 * через {@link #wasPressed()}, а также поддерживает глобальный реестр всех привязок
 * для быстрого поиска по коду клавиши.
 */
@Environment(EnvType.CLIENT)
public class KeyBinding implements Comparable<KeyBinding> {

	private static final Map<String, KeyBinding> KEYS_BY_ID = Maps.newHashMap();
	private static final Map<InputUtil.Key, List<KeyBinding>> KEY_TO_BINDINGS = Maps.newHashMap();

	private final String id;
	private final InputUtil.Key defaultKey;
	private final KeyBinding.Category category;
	private final int sortOrder;
	public InputUtil.Key boundKey;
	private boolean pressed;
	private int timesPressed;

	/**
	 * Уведомляет все привязки, связанные с данной клавишей, о её нажатии.
	 * Увеличивает счётчик {@code timesPressed} для последующей обработки через {@link #wasPressed()}.
	 */
	public static void onKeyPressed(InputUtil.Key key) {
		forAllKeyBinds(key, binding -> binding.timesPressed++);
	}

	public static void setKeyPressed(InputUtil.Key key, boolean pressed) {
		forAllKeyBinds(key, binding -> binding.setPressed(pressed));
	}

	private static void forAllKeyBinds(InputUtil.Key key, Consumer<KeyBinding> keyConsumer) {
		List<KeyBinding> bindings = KEY_TO_BINDINGS.get(key);

		if (bindings == null || bindings.isEmpty()) {
			return;
		}

		for (KeyBinding binding : bindings) {
			keyConsumer.accept(binding);
		}
	}

	/**
	 * Обновляет состояние {@code pressed} для всех клавиатурных привязок типа {@link InputUtil.Type#KEYSYM},
	 * опрашивая текущее состояние GLFW-окна.
	 */
	public static void updatePressedStates() {
		Window window = MinecraftClient.getInstance().getWindow();

		for (KeyBinding binding : KEYS_BY_ID.values()) {
			if (binding.shouldSetOnGameFocus()) {
				binding.setPressed(InputUtil.isKeyPressed(window, binding.boundKey.getCode()));
			}
		}
	}

	public static void unpressAll() {
		for (KeyBinding binding : KEYS_BY_ID.values()) {
			binding.reset();
		}
	}

	public static void restoreToggleStates() {
		for (KeyBinding binding : KEYS_BY_ID.values()) {
			if (binding instanceof StickyKeyBinding stickyBinding && stickyBinding.shouldRestoreOnScreenClose()) {
				stickyBinding.setPressed(true);
			}
		}
	}

	public static void untoggleStickyKeys() {
		for (KeyBinding binding : KEYS_BY_ID.values()) {
			if (binding instanceof StickyKeyBinding stickyBinding) {
				stickyBinding.untoggle();
			}
		}
	}

	/**
	 * Перестраивает индекс {@link #KEY_TO_BINDINGS} после изменения привязок клавиш.
	 * Должен вызываться каждый раз, когда пользователь меняет раскладку в настройках.
	 */
	public static void updateKeysByCode() {
		KEY_TO_BINDINGS.clear();

		for (KeyBinding binding : KEYS_BY_ID.values()) {
			binding.registerBinding(binding.boundKey);
		}
	}

	public KeyBinding(String id, int code, KeyBinding.Category category) {
		this(id, InputUtil.Type.KEYSYM, code, category);
	}

	public KeyBinding(String id, InputUtil.Type type, int code, KeyBinding.Category category) {
		this(id, type, code, category, 0);
	}

	public KeyBinding(String id, InputUtil.Type type, int code, KeyBinding.Category category, int sortOrder) {
		this.id = id;
		boundKey = type.createFromCode(code);
		defaultKey = boundKey;
		this.category = category;
		this.sortOrder = sortOrder;
		KEYS_BY_ID.put(id, this);
		registerBinding(boundKey);
	}

	public boolean isPressed() {
		return pressed;
	}

	public KeyBinding.Category getCategory() {
		return category;
	}

	/**
	 * Возвращает {@code true} и уменьшает счётчик нажатий, если клавиша была нажата
	 * хотя бы один раз с момента последнего вызова. Используется для обработки
	 * дискретных нажатий (не удержания).
	 */
	public boolean wasPressed() {
		if (timesPressed == 0) {
			return false;
		}

		timesPressed--;
		return true;
	}

	protected void reset() {
		timesPressed = 0;
		setPressed(false);
	}

	/**
	 * Определяет, нужно ли обновлять состояние этой привязки при наличии фокуса игры.
	 * Возвращает {@code true} только для клавиатурных привязок с известным кодом клавиши.
	 */
	protected boolean shouldSetOnGameFocus() {
		return boundKey.getCategory() == InputUtil.Type.KEYSYM
				&& boundKey.getCode() != InputUtil.UNKNOWN_KEY.getCode();
	}

	public String getId() {
		return id;
	}

	public InputUtil.Key getDefaultKey() {
		return defaultKey;
	}

	public void setBoundKey(InputUtil.Key boundKey) {
		this.boundKey = boundKey;
	}

	@Override
	public int compareTo(KeyBinding other) {
		if (category == other.category) {
			return sortOrder == other.sortOrder
					? I18n.translate(id).compareTo(I18n.translate(other.id))
					: Integer.compare(sortOrder, other.sortOrder);
		}

		return Integer.compare(
				KeyBinding.Category.CATEGORIES.indexOf(category),
				KeyBinding.Category.CATEGORIES.indexOf(other.category)
		);
	}

	public static Supplier<Text> getLocalizedName(String id) {
		KeyBinding binding = KEYS_BY_ID.get(id);
		return binding == null ? () -> Text.translatable(id) : binding::getBoundKeyLocalizedText;
	}

	public boolean equals(KeyBinding other) {
		return boundKey.equals(other.boundKey);
	}

	public boolean isUnbound() {
		return boundKey.equals(InputUtil.UNKNOWN_KEY);
	}

	/**
	 * Проверяет, соответствует ли данная привязка нажатой клавише.
	 * Учитывает как KEYSYM (виртуальный код), так и SCANCODE (физическая позиция).
	 */
	public boolean matchesKey(KeyInput key) {
		return key.key() == InputUtil.UNKNOWN_KEY.getCode()
				? boundKey.getCategory() == InputUtil.Type.SCANCODE && boundKey.getCode() == key.scancode()
				: boundKey.getCategory() == InputUtil.Type.KEYSYM && boundKey.getCode() == key.key();
	}

	public boolean matchesMouse(Click click) {
		return boundKey.getCategory() == InputUtil.Type.MOUSE && boundKey.getCode() == click.button();
	}

	public Text getBoundKeyLocalizedText() {
		return boundKey.getLocalizedText();
	}

	public boolean isDefault() {
		return boundKey.equals(defaultKey);
	}

	public String getBoundKeyTranslationKey() {
		return boundKey.getTranslationKey();
	}

	public void setPressed(boolean pressed) {
		this.pressed = pressed;
	}

	private void registerBinding(InputUtil.Key key) {
		KEY_TO_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(this);
	}

	public static @Nullable KeyBinding byId(String id) {
		return KEYS_BY_ID.get(id);
	}

	/**
	 * Категория привязки клавиш, используемая для группировки в экране настроек управления.
	 * Категории регистрируются в глобальном списке {@link #CATEGORIES} в порядке создания,
	 * что определяет их порядок отображения.
	 */
	@Environment(EnvType.CLIENT)
	public record Category(Identifier id) {

		static final List<KeyBinding.Category> CATEGORIES = new ArrayList<>();
		public static final KeyBinding.Category MOVEMENT = create("movement");
		public static final KeyBinding.Category MISC = create("misc");
		public static final KeyBinding.Category MULTIPLAYER = create("multiplayer");
		public static final KeyBinding.Category GAMEPLAY = create("gameplay");
		public static final KeyBinding.Category INVENTORY = create("inventory");
		public static final KeyBinding.Category CREATIVE = create("creative");
		public static final KeyBinding.Category SPECTATOR = create("spectator");
		public static final KeyBinding.Category DEBUG = create("debug");

		private static KeyBinding.Category create(String name) {
			return create(Identifier.ofVanilla(name));
		}

		public static KeyBinding.Category create(Identifier id) {
			KeyBinding.Category category = new KeyBinding.Category(id);

			if (CATEGORIES.contains(category)) {
				throw new IllegalArgumentException(String.format(
						Locale.ROOT,
						"Category '%s' is already registered.",
						id
				));
			}

			CATEGORIES.add(category);
			return category;
		}

		public Text getLabel() {
			return Text.translatable(id.toTranslationKey("key.category"));
		}
	}
}
