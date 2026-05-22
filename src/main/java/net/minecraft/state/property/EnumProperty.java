package net.minecraft.state.property;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Свойство, значениями которого являются константы перечисления {@link Enum},
 * реализующего {@link StringIdentifiable}.
 *
 * <p>Для ускорения {@link #ordinal(Enum)} хранит массив {@link #enumOrdinalToPropertyOrdinal},
 * который отображает порядковый номер константы в enum (по {@link Enum#ordinal()})
 * на порядковый номер в отфильтрованном списке значений этого свойства.
 * Это позволяет избежать линейного поиска при каждом переходе состояния.
 *
 * @param <T> тип перечисления
 */
public final class EnumProperty<T extends Enum<T> & StringIdentifiable> extends Property<T> {

	private final List<T> values;
	private final Map<String, T> byName;

	/**
	 * Массив размером {@code T[].length}, где индекс — это {@link Enum#ordinal()} константы,
	 * а значение — её порядковый номер в {@link #values} данного свойства.
	 * Значение {@code -1} означает, что константа не входит в список значений свойства.
	 */
	private final int[] enumOrdinalToPropertyOrdinal;

	private EnumProperty(String name, Class<T> type, List<T> values) {
		super(name, type);
		if (values.isEmpty()) {
			throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
		}

		this.values = List.copyOf(values);

		T[] enumConstants = type.getEnumConstants();
		enumOrdinalToPropertyOrdinal = new int[enumConstants.length];

		for (T constant : enumConstants) {
			enumOrdinalToPropertyOrdinal[constant.ordinal()] = values.indexOf(constant);
		}

		ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();

		for (T value : values) {
			builder.put(value.asString(), value);
		}

		byName = builder.buildOrThrow();
	}

	@Override
	public List<T> getValues() {
		return values;
	}

	@Override
	public Optional<T> parse(String name) {
		return Optional.ofNullable(byName.get(name));
	}

	@Override
	public String name(T value) {
		return value.asString();
	}

	@Override
	public int ordinal(T value) {
		return enumOrdinalToPropertyOrdinal[value.ordinal()];
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof EnumProperty<?> enumProperty
			&& super.equals(other)
			&& values.equals(enumProperty.values);
	}

	@Override
	public int computeHashCode() {
		return 31 * super.computeHashCode() + values.hashCode();
	}

	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(String name, Class<T> type) {
		return of(name, type, constant -> true);
	}

	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(
		String name,
		Class<T> type,
		Predicate<T> filter
	) {
		return of(
			name,
			type,
			Arrays.stream(type.getEnumConstants()).filter(filter).collect(Collectors.toList())
		);
	}

	@SafeVarargs
	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(
		String name,
		Class<T> type,
		T... values
	) {
		return of(name, type, List.of(values));
	}

	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(
		String name,
		Class<T> type,
		List<T> values
	) {
		return new EnumProperty<>(name, type, values);
	}
}
