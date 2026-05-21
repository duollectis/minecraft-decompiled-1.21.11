package net.minecraft.client.data;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code PropertiesMap}.
 */
public record PropertiesMap(List<Property.Value<?>> values) {

	public static final PropertiesMap EMPTY = new PropertiesMap(List.of());
	private static final Comparator<Property.Value<?>>
			COMPARATOR =
			Comparator.comparing(value -> value.property().getName());

	/**
	 * With value.
	 *
	 * @param value value
	 *
	 * @return PropertiesMap — результат операции
	 */
	public PropertiesMap withValue(Property.Value<?> value) {
		return new PropertiesMap(Util.withAppended(this.values, value));
	}

	/**
	 * Создаёт копию of.
	 *
	 * @param propertiesMap properties map
	 *
	 * @return PropertiesMap — результат операции
	 */
	public PropertiesMap copyOf(PropertiesMap propertiesMap) {
		return new PropertiesMap(
				ImmutableList.<Property.Value<?>>builder()
				             .addAll(this.values)
				             .addAll(propertiesMap.values)
				             .build()
		);
	}

	/**
	 * With values.
	 *
	 * @param values values
	 *
	 * @return PropertiesMap — результат операции
	 */
	public static PropertiesMap withValues(Property.Value<?>... values) {
		return new PropertiesMap(List.of(values));
	}

	/**
	 * As string.
	 *
	 * @return String — результат операции
	 */
	public String asString() {
		return this.values.stream().sorted(COMPARATOR).map(Property.Value::toString).collect(Collectors.joining(","));
	}

	@Override
	public String toString() {
		return this.asString();
	}
}
