package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;

/**
 * Разобранный селектор сущностей (например, {@code @a}, {@code @p[distance=..5]}).
 * Хранит исходную строку и скомпилированный {@link EntitySelector} для выполнения запросов.
 * Равенство определяется только по исходной строке.
 */
public record ParsedSelector(String raw, EntitySelector selector) {

	public static final Codec<ParsedSelector> CODEC =
		Codec.STRING.comapFlatMap(ParsedSelector::parse, ParsedSelector::raw);

	/**
	 * Разбирает строку селектора сущностей.
	 *
	 * @param selector строка селектора (например, {@code @a} или {@code @e[type=zombie]})
	 * @return успешный результат с {@link ParsedSelector} или ошибка с описанием
	 */
	public static DataResult<ParsedSelector> parse(String selector) {
		try {
			EntitySelectorReader reader = new EntitySelectorReader(new StringReader(selector), true);
			return DataResult.success(new ParsedSelector(selector, reader.read()));
		} catch (CommandSyntaxException e) {
			return DataResult.error(() -> "Invalid selector component: " + selector + ": " + e.getMessage());
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ParsedSelector other && raw.equals(other.raw);
	}

	@Override
	public int hashCode() {
		return raw.hashCode();
	}

	@Override
	public String toString() {
		return raw;
	}
}
