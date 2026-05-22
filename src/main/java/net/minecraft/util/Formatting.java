package net.minecraft.util;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Форматирование текста в Minecraft через управляющие коды (§-коды).
 * <p>
 * Включает 16 цветов (BLACK–WHITE), 5 модификаторов (BOLD, ITALIC и т.д.)
 * и специальный код RESET для сброса форматирования.
 * Каждый элемент имеет символьный код, используемый в строках вида {@code §a}.
 */
public enum Formatting implements StringIdentifiable {
	BLACK("BLACK", '0', 0, 0),
	DARK_BLUE("DARK_BLUE", '1', 1, 170),
	DARK_GREEN("DARK_GREEN", '2', 2, 43520),
	DARK_AQUA("DARK_AQUA", '3', 3, 43690),
	DARK_RED("DARK_RED", '4', 4, 11141120),
	DARK_PURPLE("DARK_PURPLE", '5', 5, 11141290),
	GOLD("GOLD", '6', 6, 16755200),
	GRAY("GRAY", '7', 7, 11184810),
	DARK_GRAY("DARK_GRAY", '8', 8, 5592405),
	BLUE("BLUE", '9', 9, 5592575),
	GREEN("GREEN", 'a', 10, 5635925),
	AQUA("AQUA", 'b', 11, 5636095),
	RED("RED", 'c', 12, 16733525),
	LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13, 16733695),
	YELLOW("YELLOW", 'e', 14, 16777045),
	WHITE("WHITE", 'f', 15, 16777215),
	OBFUSCATED("OBFUSCATED", 'k', true),
	BOLD("BOLD", 'l', true),
	STRIKETHROUGH("STRIKETHROUGH", 'm', true),
	UNDERLINE("UNDERLINE", 'n', true),
	ITALIC("ITALIC", 'o', true),
	RESET("RESET", 'r', -1, null);

	public static final Codec<Formatting> CODEC = StringIdentifiable.createCodec(Formatting::values);
	public static final Codec<Formatting> COLOR_CODEC = CODEC.validate(
		formatting -> formatting.isModifier()
			? DataResult.error(() -> "Formatting was not a valid color: " + formatting)
			: DataResult.success(formatting)
	);
	public static final char FORMATTING_CODE_PREFIX = '§';

	private static final Map<String, Formatting> BY_NAME = Arrays.stream(values())
		.collect(Collectors.toMap(f -> sanitize(f.name), f -> f));
	private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

	private final String name;
	private final char code;
	private final boolean modifier;
	private final String stringValue;
	private final int colorIndex;
	private final @Nullable Integer colorValue;

	private static String sanitize(String name) {
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
	}

	Formatting(String name, char code, int colorIndex, Integer colorValue) {
		this(name, code, false, colorIndex, colorValue);
	}

	Formatting(String name, char code, boolean modifier) {
		this(name, code, modifier, -1, null);
	}

	Formatting(String name, char code, boolean modifier, int colorIndex, @Nullable Integer colorValue) {
		this.name = name;
		this.code = code;
		this.modifier = modifier;
		this.colorIndex = colorIndex;
		this.colorValue = colorValue;
		this.stringValue = "§" + code;
	}

	public char getCode() {
		return code;
	}

	public int getColorIndex() {
		return colorIndex;
	}

	public boolean isModifier() {
		return modifier;
	}

	public boolean isColor() {
		return !modifier && this != RESET;
	}

	public @Nullable Integer getColorValue() {
		return colorValue;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return stringValue;
	}

	/**
	 * Удаляет все управляющие коды форматирования из строки.
	 *
	 * @param string исходная строка, может быть {@code null}
	 * @return строка без кодов форматирования, или {@code null} если входная строка {@code null}
	 */
	@Contract("!null->!null;_->_")
	public static @Nullable String strip(@Nullable String string) {
		return string == null ? null : FORMATTING_CODE_PATTERN.matcher(string).replaceAll("");
	}

	/**
	 * Ищет форматирование по имени (регистронезависимо, без спецсимволов).
	 *
	 * @param name имя форматирования
	 * @return найденное форматирование или {@code null}
	 */
	public static @Nullable Formatting byName(@Nullable String name) {
		return name == null ? null : BY_NAME.get(sanitize(name));
	}

	/**
	 * Ищет цветовое форматирование по числовому индексу цвета (0–15).
	 * Отрицательный индекс возвращает {@link #RESET}.
	 *
	 * @param colorIndex числовой индекс цвета
	 * @return найденное форматирование или {@code null}
	 */
	public static @Nullable Formatting byColorIndex(int colorIndex) {
		if (colorIndex < 0) {
			return RESET;
		}

		for (Formatting formatting : values()) {
			if (formatting.getColorIndex() == colorIndex) {
				return formatting;
			}
		}

		return null;
	}

	/**
	 * Ищет форматирование по символьному коду (регистронезависимо).
	 *
	 * @param code символьный код (например, {@code 'a'} для зелёного)
	 * @return найденное форматирование или {@code null}
	 */
	public static @Nullable Formatting byCode(char code) {
		char lower = Character.toLowerCase(code);

		for (Formatting formatting : values()) {
			if (formatting.code == lower) {
				return formatting;
			}
		}

		return null;
	}

	/**
	 * Возвращает коллекцию имён форматирований с фильтрацией по типу.
	 *
	 * @param colors    включать ли цветовые форматирования
	 * @param modifiers включать ли модификаторы (жирный, курсив и т.д.)
	 * @return коллекция имён подходящих форматирований
	 */
	public static Collection<String> getNames(boolean colors, boolean modifiers) {
		List<String> names = Lists.newArrayList();

		for (Formatting formatting : values()) {
			if ((!formatting.isColor() || colors) && (!formatting.isModifier() || modifiers)) {
				names.add(formatting.getName());
			}
		}

		return names;
	}

	@Override
	public String asString() {
		return getName();
	}
}
