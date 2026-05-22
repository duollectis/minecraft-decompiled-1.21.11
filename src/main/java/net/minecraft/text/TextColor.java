package net.minecraft.text;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Цвет текстового компонента, представленный 24-битным RGB-значением.
 * Может быть создан из {@link Formatting} (именованные цвета) или произвольного RGB-числа.
 * Именованные цвета хранят строковое имя; произвольные — сериализуются как {@code #RRGGBB}.
 */
public final class TextColor {

	/** Маска для ограничения значения 24-битным диапазоном (0x000000–0xFFFFFF). */
	private static final int RGB_MASK = 0xFFFFFF;

	private static final String RGB_PREFIX = "#";

	public static final Codec<TextColor> CODEC = Codec.STRING.comapFlatMap(TextColor::parse, TextColor::getName);

	private static final Map<Formatting, TextColor> FORMATTING_TO_COLOR = Stream.of(Formatting.values())
		.filter(Formatting::isColor)
		.collect(ImmutableMap.toImmutableMap(
			Function.identity(),
			formatting -> new TextColor(formatting.getColorValue(), formatting.getName())
		));

	private static final Map<String, TextColor> BY_NAME = FORMATTING_TO_COLOR.values()
		.stream()
		.collect(ImmutableMap.toImmutableMap(
			textColor -> textColor.name,
			Function.identity()
		));

	private final int rgb;
	private final @Nullable String name;

	private TextColor(int rgb, String name) {
		this.rgb = rgb & RGB_MASK;
		this.name = name;
	}

	private TextColor(int rgb) {
		this.rgb = rgb & RGB_MASK;
		this.name = null;
	}

	public int getRgb() {
		return rgb;
	}

	public String getName() {
		return name != null ? name : getHexCode();
	}

	public String getHexCode() {
		return String.format(Locale.ROOT, "#%06X", rgb);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		return rgb == ((TextColor) o).rgb;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rgb, name);
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Возвращает цвет, соответствующий форматированию Minecraft, или {@code null},
	 * если форматирование не является цветовым.
	 */
	public static @Nullable TextColor fromFormatting(Formatting formatting) {
		return FORMATTING_TO_COLOR.get(formatting);
	}

	public static TextColor fromRgb(int rgb) {
		return new TextColor(rgb);
	}

	/**
	 * Разбирает цвет из строки: либо {@code #RRGGBB} (hex), либо именованный цвет Minecraft.
	 * Hex-значение должно быть в диапазоне {@code 0x000000}–{@code 0xFFFFFF}.
	 *
	 * @param name строковое представление цвета
	 * @return успешный результат с {@link TextColor} или ошибка с описанием
	 */
	public static DataResult<TextColor> parse(String name) {
		if (name.startsWith(RGB_PREFIX)) {
			try {
				int rgb = Integer.parseInt(name.substring(1), 16);
				return rgb >= 0 && rgb <= RGB_MASK
					? DataResult.success(fromRgb(rgb), Lifecycle.stable())
					: DataResult.error(() -> "Color value out of range: " + name);
			} catch (NumberFormatException e) {
				return DataResult.error(() -> "Invalid color value: " + name);
			}
		}

		TextColor textColor = BY_NAME.get(name);
		return textColor == null
			? DataResult.error(() -> "Invalid color name: " + name)
			: DataResult.success(textColor, Lifecycle.stable());
	}
}
