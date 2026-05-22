package net.minecraft.state.property;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.state.State;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Абстрактное свойство блока или другого объекта, имеющего состояния.
 *
 * <p>Свойство описывает одно измерение пространства состояний: его имя, тип значений
 * и полный список допустимых значений. Каждое свойство является неизменяемым объектом
 * и идентифицируется по имени и типу.
 *
 * <p>Хэш-код кэшируется при первом вычислении, поскольку свойства используются
 * как ключи в {@link it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap} и
 * вычисление хэша происходит очень часто.
 *
 * @param <T> тип значения свойства, должен реализовывать {@link Comparable}
 */
public abstract class Property<T extends Comparable<T>> {

	private final Class<T> type;
	private final String name;
	private @Nullable Integer hashCodeCache;

	/**
	 * Кодек для сериализации значений свойства через строковое представление.
	 * При декодировании вызывает {@link #parse(String)}, при кодировании — {@link #name(Comparable)}.
	 */
	private final Codec<T> codec = Codec.STRING.comapFlatMap(
		value -> parse(value)
			.<DataResult<T>>map(DataResult::success)
			.orElseGet(() -> DataResult.error(() -> "Unable to read property: " + this + " with value: " + value)),
		this::name
	);

	private final Codec<Property.Value<T>> valueCodec = codec.xmap(this::createValue, Property.Value::value);

	protected Property(String name, Class<T> type) {
		this.type = type;
		this.name = name;
	}

	public Property.Value<T> createValue(T value) {
		return new Property.Value<>(this, value);
	}

	public Property.Value<T> createValue(State<?, ?> state) {
		return new Property.Value<>(this, state.get(this));
	}

	public Stream<Property.Value<T>> stream() {
		return getValues().stream().map(this::createValue);
	}

	public Codec<T> getCodec() {
		return codec;
	}

	public Codec<Property.Value<T>> getValueCodec() {
		return valueCodec;
	}

	public String getName() {
		return name;
	}

	public Class<T> getType() {
		return type;
	}

	/** Возвращает упорядоченный список всех допустимых значений этого свойства. */
	public abstract List<T> getValues();

	/** Возвращает строковое представление значения для сериализации. */
	public abstract String name(T value);

	/**
	 * Разбирает строковое представление значения.
	 *
	 * @param name строка для разбора
	 * @return {@link Optional} с распознанным значением, или {@link Optional#empty()} при ошибке
	 */
	public abstract Optional<T> parse(String name);

	/**
	 * Возвращает порядковый номер значения в списке {@link #getValues()}.
	 * Используется для O(1)-индексации в кэше переходов состояний.
	 *
	 * @param value значение свойства
	 * @return индекс в списке значений, или {@code -1} если значение недопустимо
	 */
	public abstract int ordinal(T value);

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("name", name)
			.add("clazz", type)
			.add("values", getValues())
			.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof Property<?> property
			&& type.equals(property.type)
			&& name.equals(property.name);
	}

	@Override
	public final int hashCode() {
		if (hashCodeCache == null) {
			hashCodeCache = computeHashCode();
		}

		return hashCodeCache;
	}

	/**
	 * Вычисляет хэш-код свойства. Переопределяется в подклассах для учёта
	 * дополнительных полей (например, списка допустимых значений в {@link EnumProperty}).
	 */
	public int computeHashCode() {
		return 31 * type.hashCode() + name.hashCode();
	}

	/**
	 * Декодирует значение свойства из произвольного формата данных и применяет его к состоянию.
	 *
	 * @param ops   операции над форматом данных
	 * @param state исходное состояние
	 * @param input входные данные для декодирования
	 * @param <U>   тип формата данных
	 * @param <S>   тип состояния
	 * @return {@link DataResult} с обновлённым состоянием
	 */
	public <U, S extends State<?, S>> DataResult<S> parse(DynamicOps<U> ops, S state, U input) {
		DataResult<T> result = codec.parse(ops, input);
		return result.map(value -> state.with(this, value)).setPartial(state);
	}

	/**
	 * Конкретное значение свойства, связанное с самим свойством.
	 * Используется в кодеках для типобезопасной передачи пары (свойство, значение).
	 *
	 * @param <T> тип значения
	 */
	public record Value<T extends Comparable<T>>(Property<T> property, T value) {

		public Value(Property<T> property, T value) {
			if (!property.getValues().contains(value)) {
				throw new IllegalArgumentException("Value " + value + " does not belong to property " + property);
			}

			this.property = property;
			this.value = value;
		}

		@Override
		public String toString() {
			return property.getName() + "=" + property.name(value);
		}
	}
}
