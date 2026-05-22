package net.minecraft.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.state.property.Property;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Управляет полным пространством состояний объекта-владельца.
 *
 * <p>При создании {@code StateManager} строит декартово произведение всех допустимых
 * значений зарегистрированных свойств, создавая по одному экземпляру {@link State}
 * на каждую уникальную комбинацию. Затем для каждого состояния инициализируется
 * кэш переходов ({@link State#createWithMap}), что делает вызов
 * {@link State#with(Property, Comparable)} операцией O(1).
 *
 * @param <O> тип владельца (например, {@code Block})
 * @param <S> конкретный подтип состояния
 */
public class StateManager<O, S extends State<O, S>> {

	static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

	private final O owner;
	private final ImmutableSortedMap<String, Property<?>> properties;
	private final ImmutableList<S> states;

	/**
	 * Строит менеджер состояний, перебирая все комбинации значений свойств.
	 *
	 * <p>Алгоритм: начиная с пустого списка пар, для каждого свойства расширяет
	 * поток через {@code flatMap}, добавляя все допустимые значения. В итоге
	 * поток содержит по одному списку пар на каждую возможную комбинацию.
	 *
	 * @param defaultStateGetter функция получения дефолтного состояния по владельцу
	 * @param owner              владелец состояний
	 * @param factory            фабрика для создания конкретных экземпляров состояний
	 * @param propertiesMap      карта имя → свойство
	 */
	protected StateManager(
		Function<O, S> defaultStateGetter,
		O owner,
		StateManager.Factory<O, S> factory,
		Map<String, Property<?>> propertiesMap
	) {
		this.owner = owner;
		this.properties = ImmutableSortedMap.copyOf(propertiesMap);

		Supplier<S> defaultState = () -> defaultStateGetter.apply(owner);
		MapCodec<S> mapCodec = MapCodec.of(Encoder.empty(), Decoder.unit(defaultState));

		for (Map.Entry<String, Property<?>> entry : this.properties.entrySet()) {
			mapCodec = addFieldToMapCodec(mapCodec, defaultState, entry.getKey(), entry.getValue());
		}

		MapCodec<S> finalCodec = mapCodec;
		Map<Map<Property<?>, Comparable<?>>, S> statePool = Maps.newLinkedHashMap();
		List<S> stateList = Lists.newArrayList();
		Stream<List<Pair<Property<?>, Comparable<?>>>> stream = Stream.of(Collections.emptyList());

		for (Property<?> property : this.properties.values()) {
			stream = stream.flatMap(
				pairs -> property.getValues().stream().map(value -> {
					List<Pair<Property<?>, Comparable<?>>> extended = Lists.newArrayList(pairs);
					extended.add(Pair.of(property, value));
					return extended;
				})
			);
		}

		stream.forEach(pairs -> {
			Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap =
				new Reference2ObjectArrayMap<>(pairs.size());

			for (Pair<Property<?>, Comparable<?>> pair : pairs) {
				propertyMap.put((Property) pair.getFirst(), (Comparable) pair.getSecond());
			}

			S state = factory.create(owner, propertyMap, finalCodec);
			statePool.put(propertyMap, state);
			stateList.add(state);
		});

		for (S state : stateList) {
			state.createWithMap(statePool);
		}

		this.states = ImmutableList.copyOf(stateList);
	}

	/**
	 * Расширяет составной {@link MapCodec} новым полем для указанного свойства.
	 *
	 * <p>Использует {@code Codec.mapPair} для объединения текущего кодека с кодеком
	 * нового свойства, затем через {@code xmap} связывает пару (состояние, значение)
	 * с вызовом {@link State#with(Property, Comparable)}.
	 *
	 * @param mapCodec          текущий составной кодек
	 * @param defaultState      поставщик дефолтного состояния (для fallback при отсутствии поля)
	 * @param key               имя поля в сериализованном представлении
	 * @param property          свойство, для которого добавляется поле
	 * @param <S>               тип состояния
	 * @param <T>               тип значения свойства
	 * @return расширенный кодек
	 */
	@SuppressWarnings("unchecked")
	private static <S extends State<?, S>, T extends Comparable<T>> MapCodec<S> addFieldToMapCodec(
		MapCodec<S> mapCodec,
		Supplier<S> defaultState,
		String key,
		Property<T> property
	) {
		return (MapCodec<S>) Codec
			.mapPair(
				mapCodec,
				property
					.getValueCodec()
					.fieldOf(key)
					.orElseGet(value -> {}, () -> property.createValue(defaultState.get()))
			)
			.xmap(
				pair -> ((S) pair.getFirst()).with(property, ((Property.Value<T>) pair.getSecond()).value()),
				state -> Pair.of(state, property.createValue((State<?, ?>) state))
			);
	}

	public ImmutableList<S> getStates() {
		return states;
	}

	public S getDefaultState() {
		return states.get(0);
	}

	public O getOwner() {
		return owner;
	}

	public Collection<Property<?>> getProperties() {
		return properties.values();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("block", owner)
			.add("properties", properties.values().stream().map(Property::getName).collect(Collectors.toList()))
			.toString();
	}

	public @Nullable Property<?> getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Строитель для пошаговой регистрации свойств перед созданием {@link StateManager}.
	 *
	 * @param <O> тип владельца
	 * @param <S> тип состояния
	 */
	public static class Builder<O, S extends State<O, S>> {

		private final O owner;
		private final Map<String, Property<?>> namedProperties = Maps.newHashMap();

		public Builder(O owner) {
			this.owner = owner;
		}

		/**
		 * Регистрирует одно или несколько свойств в этом строителе.
		 * Каждое свойство проходит валидацию имени и значений.
		 *
		 * @param properties свойства для регистрации
		 * @return {@code this} для цепочки вызовов
		 * @throws IllegalArgumentException если имя свойства или его значений не соответствует
		 *                                  паттерну {@link StateManager#VALID_NAME_PATTERN},
		 *                                  если у свойства менее двух значений,
		 *                                  или если свойство с таким именем уже зарегистрировано
		 */
		public StateManager.Builder<O, S> add(Property<?>... properties) {
			for (Property<?> property : properties) {
				validate(property);
				namedProperties.put(property.getName(), property);
			}

			return this;
		}

		private <T extends Comparable<T>> void validate(Property<T> property) {
			String propertyName = property.getName();
			if (!StateManager.VALID_NAME_PATTERN.matcher(propertyName).matches()) {
				throw new IllegalArgumentException(owner + " has invalidly named property: " + propertyName);
			}

			Collection<T> values = property.getValues();
			if (values.size() <= 1) {
				throw new IllegalArgumentException(
					owner + " attempted use property " + propertyName + " with <= 1 possible values"
				);
			}

			for (T value : values) {
				String valueName = property.name(value);
				if (!StateManager.VALID_NAME_PATTERN.matcher(valueName).matches()) {
					throw new IllegalArgumentException(
						owner + " has property: " + propertyName + " with invalidly named value: " + valueName
					);
				}
			}

			if (namedProperties.containsKey(propertyName)) {
				throw new IllegalArgumentException(owner + " has duplicate property: " + propertyName);
			}
		}

		public StateManager<O, S> build(Function<O, S> defaultStateGetter, StateManager.Factory<O, S> factory) {
			return new StateManager<>(defaultStateGetter, owner, factory, namedProperties);
		}
	}

	/**
	 * Фабрика для создания конкретных экземпляров состояний.
	 * Реализуется каждым конкретным подтипом {@link State}.
	 *
	 * @param <O> тип владельца
	 * @param <S> тип состояния
	 */
	public interface Factory<O, S> {

		S create(O owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<S> codec);
	}
}
