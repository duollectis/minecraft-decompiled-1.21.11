package net.minecraft.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Предикат для проверки свойств состояния блока или жидкости.
 * Каждое условие ({@link Condition}) проверяет одно свойство по имени.
 * Поддерживает точное совпадение ({@link ExactValueMatcher}) и диапазон ({@link RangedValueMatcher}).
 */
public record StatePredicate(List<StatePredicate.Condition> conditions) {

	/**
	 * Codec для преобразования карты {@code {имя_свойства -> матчер}} в список условий.
	 * Декомпилятор добавлял лишние касты {@code (String)} и {@code (ValueMatcher)} — убраны.
	 */
	private static final Codec<List<StatePredicate.Condition>> CONDITION_LIST_CODEC =
			Codec.unboundedMap(Codec.STRING, StatePredicate.ValueMatcher.CODEC)
					.xmap(
							states -> states.entrySet()
									.stream()
									.map(entry -> new StatePredicate.Condition(entry.getKey(), entry.getValue()))
									.toList(),
							conditions -> conditions.stream()
									.collect(Collectors.toMap(
											StatePredicate.Condition::key,
											StatePredicate.Condition::valueMatcher
									))
					);

	public static final Codec<StatePredicate> CODEC =
			CONDITION_LIST_CODEC.xmap(StatePredicate::new, StatePredicate::conditions);
	public static final PacketCodec<ByteBuf, StatePredicate> PACKET_CODEC = StatePredicate.Condition.PACKET_CODEC
			.collect(PacketCodecs.toList())
			.xmap(StatePredicate::new, StatePredicate::conditions);

	public <S extends State<?, S>> boolean test(StateManager<?, S> stateManager, S container) {
		for (StatePredicate.Condition condition : conditions) {
			if (!condition.test(stateManager, container)) {
				return false;
			}
		}

		return true;
	}

	public boolean test(BlockState state) {
		return test(state.getBlock().getStateManager(), state);
	}

	public boolean test(FluidState state) {
		return test(state.getFluid().getStateManager(), state);
	}

	public Optional<String> findMissing(StateManager<?, ?> stateManager) {
		for (StatePredicate.Condition condition : conditions) {
			Optional<String> missing = condition.reportMissing(stateManager);

			if (missing.isPresent()) {
				return missing;
			}
		}

		return Optional.empty();
	}

	public static class Builder {

		private final ImmutableList.Builder<StatePredicate.Condition> conditions = ImmutableList.builder();

		private Builder() {
		}

		public static StatePredicate.Builder create() {
			return new StatePredicate.Builder();
		}

		public StatePredicate.Builder exactMatch(Property<?> property, String valueName) {
			conditions.add(new StatePredicate.Condition(
					property.getName(),
					new StatePredicate.ExactValueMatcher(valueName)
			));
			return this;
		}

		public StatePredicate.Builder exactMatch(Property<Integer> property, int value) {
			return exactMatch(property, Integer.toString(value));
		}

		public StatePredicate.Builder exactMatch(Property<Boolean> property, boolean value) {
			return exactMatch(property, Boolean.toString(value));
		}

		public <T extends Comparable<T> & StringIdentifiable> StatePredicate.Builder exactMatch(
				Property<T> property,
				T value
		) {
			return exactMatch(property, value.asString());
		}

		public Optional<StatePredicate> build() {
			return Optional.of(new StatePredicate(conditions.build()));
		}
	}

	record Condition(String key, StatePredicate.ValueMatcher valueMatcher) {

		public static final PacketCodec<ByteBuf, StatePredicate.Condition> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, StatePredicate.Condition::key,
				StatePredicate.ValueMatcher.PACKET_CODEC, StatePredicate.Condition::valueMatcher,
				StatePredicate.Condition::new
		);

		public <S extends State<?, S>> boolean test(StateManager<?, S> stateManager, S state) {
			Property<?> property = stateManager.getProperty(key);
			return property != null && valueMatcher.test(state, property);
		}

		public Optional<String> reportMissing(StateManager<?, ?> factory) {
			Property<?> property = factory.getProperty(key);
			return property == null ? Optional.of(key) : Optional.empty();
		}
	}

	record ExactValueMatcher(String value) implements StatePredicate.ValueMatcher {

		public static final Codec<StatePredicate.ExactValueMatcher> CODEC = Codec.STRING
				.xmap(StatePredicate.ExactValueMatcher::new, StatePredicate.ExactValueMatcher::value);
		public static final PacketCodec<ByteBuf, StatePredicate.ExactValueMatcher> PACKET_CODEC = PacketCodecs.STRING
				.xmap(StatePredicate.ExactValueMatcher::new, StatePredicate.ExactValueMatcher::value);

		@Override
		public <T extends Comparable<T>> boolean test(State<?, ?> state, Property<T> property) {
			T current = state.get(property);
			Optional<T> parsed = property.parse(value);
			return parsed.isPresent() && current.compareTo(parsed.get()) == 0;
		}
	}

	record RangedValueMatcher(Optional<String> min, Optional<String> max) implements StatePredicate.ValueMatcher {

		public static final Codec<StatePredicate.RangedValueMatcher> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.STRING.optionalFieldOf("min").forGetter(StatePredicate.RangedValueMatcher::min),
						Codec.STRING.optionalFieldOf("max").forGetter(StatePredicate.RangedValueMatcher::max)
				)
				.apply(instance, StatePredicate.RangedValueMatcher::new)
		);
		public static final PacketCodec<ByteBuf, StatePredicate.RangedValueMatcher> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.optional(PacketCodecs.STRING), StatePredicate.RangedValueMatcher::min,
				PacketCodecs.optional(PacketCodecs.STRING), StatePredicate.RangedValueMatcher::max,
				StatePredicate.RangedValueMatcher::new
		);

		@Override
		public <T extends Comparable<T>> boolean test(State<?, ?> state, Property<T> property) {
			T current = state.get(property);

			if (min.isPresent()) {
				Optional<T> parsedMin = property.parse(min.get());

				if (parsedMin.isEmpty() || current.compareTo(parsedMin.get()) < 0) {
					return false;
				}
			}

			if (max.isPresent()) {
				Optional<T> parsedMax = property.parse(max.get());

				if (parsedMax.isEmpty() || current.compareTo(parsedMax.get()) > 0) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Интерфейс для сопоставления значения свойства состояния.
	 * Реализации: {@link ExactValueMatcher} и {@link RangedValueMatcher}.
	 */
	interface ValueMatcher {

		Codec<StatePredicate.ValueMatcher> CODEC =
				Codec.either(StatePredicate.ExactValueMatcher.CODEC, StatePredicate.RangedValueMatcher.CODEC)
						.xmap(
								Either::unwrap,
								valueMatcher -> {
									if (valueMatcher instanceof StatePredicate.ExactValueMatcher exact) {
										return Either.left(exact);
									}

									if (valueMatcher instanceof StatePredicate.RangedValueMatcher ranged) {
										return Either.right(ranged);
									}

									throw new UnsupportedOperationException();
								}
						);

		PacketCodec<ByteBuf, StatePredicate.ValueMatcher> PACKET_CODEC = PacketCodecs.either(
				StatePredicate.ExactValueMatcher.PACKET_CODEC,
				StatePredicate.RangedValueMatcher.PACKET_CODEC
		)
		.xmap(
				Either::unwrap,
				valueMatcher -> {
					if (valueMatcher instanceof StatePredicate.ExactValueMatcher exact) {
						return Either.left(exact);
					}

					if (valueMatcher instanceof StatePredicate.RangedValueMatcher ranged) {
						return Either.right(ranged);
					}

					throw new UnsupportedOperationException();
				}
		);

		<T extends Comparable<T>> boolean test(State<?, ?> state, Property<T> property);
	}
}
