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
 * {@code TextColor}.
 */
public final class TextColor {

	private static final String RGB_PREFIX = "#";
	public static final Codec<TextColor> CODEC = Codec.STRING.comapFlatMap(TextColor::parse, TextColor::getName);
	private static final Map<Formatting, TextColor> FORMATTING_TO_COLOR = Stream.of(Formatting.values())
	                                                                            .filter(Formatting::isColor)
	                                                                            .collect(ImmutableMap.toImmutableMap(
			                                                                            Function.identity(),
			                                                                            formatting -> new TextColor(
					                                                                            formatting.getColorValue(),
					                                                                            formatting.getName()
			                                                                            )
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
		this.rgb = rgb & 16777215;
		this.name = name;
	}

	private TextColor(int rgb) {
		this.rgb = rgb & 16777215;
		this.name = null;
	}

	public int getRgb() {
		return this.rgb;
	}

	public String getName() {
		return this.name != null ? this.name : this.getHexCode();
	}

	public final String getHexCode() {
		return String.format(Locale.ROOT, "#%06X", this.rgb);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o != null && this.getClass() == o.getClass()) {
			TextColor textColor = (TextColor) o;
			return this.rgb == textColor.rgb;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.rgb, this.name);
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public static @Nullable TextColor fromFormatting(Formatting formatting) {
		return FORMATTING_TO_COLOR.get(formatting);
	}

	public static TextColor fromRgb(int rgb) {
		return new TextColor(rgb);
	}

	public static DataResult<TextColor> parse(String name) {
		if (name.startsWith("#")) {
			try {
				int i = Integer.parseInt(name.substring(1), 16);
				return i >= 0 && i <= 16777215 ? DataResult.success(fromRgb(i), Lifecycle.stable())
				                               : DataResult.error(() -> "Color value out of range: " + name);
			}
			catch (NumberFormatException var2) {
				return DataResult.error(() -> "Invalid color value: " + name);
			}
		}
		else {
			TextColor textColor = BY_NAME.get(name);
			return textColor == null ? DataResult.error(() -> "Invalid color name: " + name)
			                         : DataResult.success(textColor, Lifecycle.stable());
		}
	}
}
