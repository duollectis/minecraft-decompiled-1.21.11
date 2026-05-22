package net.minecraft.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.state.property.Property;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Базовый абстрактный класс, представляющий состояние объекта (блока, флюида и т.д.)
 * как неизменяемый набор пар {@link Property} → значение.
 *
 * <p>Каждый экземпляр {@code State} является уникальным объектом в пуле всех возможных
 * состояний, построенном {@link StateManager}. Переходы между состояниями осуществляются
 * через {@link #with(Property, Comparable)}, который возвращает уже существующий объект
 * из пула, а не создаёт новый.
 *
 * @param <O> тип владельца состояния (например, {@code Block})
 * @param <S> конкретный подтип состояния (self-referential generic)
 */
public abstract class State<O, S> {

	public static final String NAME = "Name";
	public static final String PROPERTIES = "Properties";

	/**
	 * Форматирует одну запись карты свойств в строку вида {@code "имя=значение"}.
	 * Используется в {@link #toString()} для отладочного вывода.
	 */
	private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER =
		entry -> {
			if (entry == null) {
				return "<NULL>";
			}

			Property<?> property = entry.getKey();
			return property.getName() + "=" + nameValue(property, entry.getValue());
		};

	protected final O owner;
	private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap;
	private Map<Property<?>, S[]> withMap;
	protected final MapCodec<S> codec;

	protected State(O owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<S> codec) {
		this.owner = owner;
		this.propertyMap = propertyMap;
		this.codec = codec;
	}

	/**
	 * Возвращает следующее состояние, в котором значение указанного свойства
	 * циклически сдвинуто на одну позицию вперёд по списку допустимых значений.
	 *
	 * @param property свойство, значение которого нужно сдвинуть
	 * @param <T>      тип значения свойства
	 * @return следующее состояние с обновлённым значением свойства
	 */
	public <T extends Comparable<T>> S cycle(Property<T> property) {
		return with(property, getNext(property.getValues(), get(property)));
	}

	/**
	 * Возвращает следующий элемент списка после {@code value}, циклически
	 * возвращаясь к первому элементу по достижении конца.
	 *
	 * @param values список допустимых значений
	 * @param value  текущее значение
	 * @param <T>    тип элемента
	 * @return следующий элемент в цикле
	 */
	protected static <T> T getNext(java.util.List<T> values, T value) {
		int nextIndex = values.indexOf(value) + 1;
		return nextIndex == values.size() ? values.getFirst() : values.get(nextIndex);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(owner);

		if (getEntries().isEmpty()) {
			return builder.toString();
		}

		builder.append('[');
		builder.append(
			getEntries()
				.entrySet()
				.stream()
				.map(PROPERTY_MAP_PRINTER)
				.collect(Collectors.joining(","))
		);
		builder.append(']');

		return builder.toString();
	}

	@Override
	public final boolean equals(Object object) {
		return super.equals(object);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public Collection<Property<?>> getProperties() {
		return Collections.unmodifiableCollection(propertyMap.keySet());
	}

	public boolean contains(Property<?> property) {
		return propertyMap.containsKey(property);
	}

	/**
	 * Возвращает текущее значение указанного свойства.
	 *
	 * @param property свойство для чтения
	 * @param <T>      тип значения
	 * @return текущее значение свойства
	 * @throws IllegalArgumentException если свойство не принадлежит этому состоянию
	 */
	public <T extends Comparable<T>> T get(Property<T> property) {
		Comparable<?> value = propertyMap.get(property);
		if (value == null) {
			throw new IllegalArgumentException(
				"Cannot get property " + property + " as it does not exist in " + owner
			);
		}

		return property.getType().cast(value);
	}

	/**
	 * Возвращает значение свойства, обёрнутое в {@link Optional},
	 * или {@link Optional#empty()}, если свойство не принадлежит этому состоянию.
	 */
	public <T extends Comparable<T>> Optional<T> getOrEmpty(Property<T> property) {
		return Optional.ofNullable(getNullable(property));
	}

	/**
	 * Возвращает значение свойства, или {@code fallback}, если свойство
	 * не принадлежит этому состоянию.
	 */
	public <T extends Comparable<T>> T get(Property<T> property, T fallback) {
		return Objects.requireNonNullElse(getNullable(property), fallback);
	}

	private <T extends Comparable<T>> @Nullable T getNullable(Property<T> property) {
		Comparable<?> value = propertyMap.get(property);
		return value == null ? null : property.getType().cast(value);
	}

	/**
	 * Возвращает состояние, в котором указанное свойство имеет новое значение.
	 * Возвращает объект из заранее построенного пула состояний — новый объект не создаётся.
	 *
	 * @param property свойство для изменения
	 * @param value    новое значение
	 * @param <T>      тип значения
	 * @param <V>      подтип значения
	 * @return состояние с обновлённым свойством
	 * @throws IllegalArgumentException если свойство не принадлежит этому состоянию
	 *                                  или значение недопустимо
	 */
	public <T extends Comparable<T>, V extends T> S with(Property<T> property, V value) {
		Comparable<?> current = propertyMap.get(property);
		if (current == null) {
			throw new IllegalArgumentException(
				"Cannot set property " + property + " as it does not exist in " + owner
			);
		}

		return withInternal(property, value, current);
	}

	/**
	 * Аналог {@link #with(Property, Comparable)}, но не бросает исключение,
	 * если свойство не принадлежит этому состоянию — в таком случае возвращает {@code this}.
	 */
	public <T extends Comparable<T>, V extends T> S withIfExists(Property<T> property, V value) {
		Comparable<?> current = propertyMap.get(property);
		return current == null ? (S) this : withInternal(property, value, current);
	}

	/**
	 * Внутренняя реализация перехода состояния. Если новое значение совпадает
	 * с текущим — возвращает {@code this}. Иначе ищет нужное состояние в пуле
	 * через предварительно построенный массив {@link #withMap}.
	 */
	private <T extends Comparable<T>, V extends T> S withInternal(
		Property<T> property,
		V newValue,
		Comparable<?> oldValue
	) {
		if (oldValue.equals(newValue)) {
			return (S) this;
		}

		int ordinal = property.ordinal((T) newValue);
		if (ordinal < 0) {
			throw new IllegalArgumentException(
				"Cannot set property " + property + " to " + newValue + " on " + owner
					+ ", it is not an allowed value"
			);
		}

		return withMap.get(property)[ordinal];
	}

	/**
	 * Инициализирует внутренний кэш переходов между состояниями.
	 * Для каждого свойства строится массив состояний, индексированный по порядковому
	 * номеру значения свойства. Вызывается единожды из {@link StateManager} после
	 * создания всего пула состояний.
	 *
	 * @param states полный пул всех возможных состояний, ключ — карта свойств
	 * @throws IllegalStateException если метод вызван повторно
	 */
	public void createWithMap(Map<Map<Property<?>, Comparable<?>>, S> states) {
		if (withMap != null) {
			throw new IllegalStateException();
		}

		Map<Property<?>, S[]> result = new Reference2ObjectArrayMap<>(propertyMap.size());

		for (Map.Entry<Property<?>, Comparable<?>> entry : propertyMap.entrySet()) {
			Property<?> property = entry.getKey();
			result.put(
				property,
				(S[]) property
					.getValues()
					.stream()
					.map(value -> states.get(toMapWith(property, value)))
					.toArray()
			);
		}

		withMap = result;
	}

	private Map<Property<?>, Comparable<?>> toMapWith(Property<?> property, Comparable<?> value) {
		Map<Property<?>, Comparable<?>> map = new Reference2ObjectArrayMap<>(propertyMap);
		map.put(property, value);
		return map;
	}

	public Map<Property<?>, Comparable<?>> getEntries() {
		return propertyMap;
	}

	/**
	 * Создаёт {@link Codec} для сериализации/десериализации состояний.
	 * Кодек диспетчеризует по полю {@value NAME}: сначала декодирует владельца,
	 * затем опционально читает поле {@value PROPERTIES} для восстановления конкретного состояния.
	 *
	 * @param ownerCodec          кодек для сериализации владельца
	 * @param ownerToStateFunction функция получения дефолтного состояния по владельцу
	 * @param <O>                 тип владельца
	 * @param <S>                 тип состояния
	 * @return кодек для состояний
	 */
	protected static <O, S extends State<O, S>> Codec<S> createCodec(
		Codec<O> ownerCodec,
		Function<O, S> ownerToStateFunction
	) {
		return ownerCodec.dispatch(
			NAME,
			state -> state.owner,
			owner -> {
				S state = ownerToStateFunction.apply((O) owner);
				return state.getEntries().isEmpty()
					? MapCodec.unit(state)
					: state.codec
						.codec()
						.lenientOptionalFieldOf(PROPERTIES)
						.xmap(opt -> opt.orElse(state), Optional::of);
			}
		);
	}

	private static <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
		return property.name((T) value);
	}
}
