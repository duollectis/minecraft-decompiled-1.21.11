package net.minecraft.state.property;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code EnumProperty}.
 */
public final class EnumProperty<T extends Enum<T> & StringIdentifiable> extends Property<T> {

	private final List<T> values;
	private final Map<String, T> byName;
	private final int[] enumOrdinalToPropertyOrdinal;

	private EnumProperty(String name, Class<T> type, List<T> values) {
		super(name, type);
		if (values.isEmpty()) {
			throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
		}
		else {
			this.values = List.copyOf(values);
			T[] enums = type.getEnumConstants();
			this.enumOrdinalToPropertyOrdinal = new int[enums.length];

			for (T enum_ : enums) {
				this.enumOrdinalToPropertyOrdinal[enum_.ordinal()] = values.indexOf(enum_);
			}

			Builder<String, T> builder = ImmutableMap.builder();

			for (T enum2 : values) {
				String string = enum2.asString();
				builder.put(string, enum2);
			}

			this.byName = builder.buildOrThrow();
		}
	}

	@Override
	public List<T> getValues() {
		return this.values;
	}

	@Override
	public Optional<T> parse(String name) {
		return Optional.ofNullable(this.byName.get(name));
	}

	public String name(T enum_) {
		return enum_.asString();
	}

	public int ordinal(T enum_) {
		return this.enumOrdinalToPropertyOrdinal[enum_.ordinal()];
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		else {
			return object instanceof EnumProperty<?> enumProperty && super.equals(object) ? this.values.equals(
					enumProperty.values) : false;
		}
	}

	@Override
	public int computeHashCode() {
		int i = super.computeHashCode();
		return 31 * i + this.values.hashCode();
	}

	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(String name, Class<T> type) {
		return of(name, type, enum_ -> true);
	}

	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(
			String name,
			Class<T> type,
			Predicate<T> filter
	) {
		return of(name, type, Arrays.<T>stream(type.getEnumConstants()).filter(filter).collect(Collectors.toList()));
	}

	@SafeVarargs
	public static <T extends Enum<T> & StringIdentifiable> EnumProperty<T> of(String name, Class<T> type, T... values) {
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
