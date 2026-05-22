package net.minecraft.client.render.model.json;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

/**
 * Условие мультипарт-модели на основе простых проверок свойств блока.
 * Хранит карту {@code propertyName → Terms}, где каждый {@link Terms} — список
 * допустимых значений (с поддержкой отрицания через префикс {@code !} и
 * перечисления через разделитель {@code |}).
 */
@Environment(EnvType.CLIENT)
public record SimpleMultipartModelSelector(Map<String, SimpleMultipartModelSelector.Terms> tests) implements MultipartModelCondition {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<SimpleMultipartModelSelector> CODEC = Codecs.nonEmptyMap(
			Codec.unboundedMap(Codec.STRING, SimpleMultipartModelSelector.Terms.VALUE_CODEC)
	)
	.xmap(
			SimpleMultipartModelSelector::new,
			SimpleMultipartModelSelector::tests
	);

	@Override
	public <O, S extends State<O, S>> Predicate<S> instantiate(StateManager<O, S> stateManager) {
		List<Predicate<S>> predicates = new ArrayList<>(tests.size());
		tests.forEach((property, terms) -> predicates.add(init(stateManager, property, terms)));
		return Util.allOf(predicates);
	}

	private static <O, S extends State<O, S>> Predicate<S> init(
			StateManager<O, S> stateManager,
			String property,
			SimpleMultipartModelSelector.Terms terms
	) {
		Property<?> resolvedProperty = stateManager.getProperty(property);
		if (resolvedProperty == null) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"Unknown property '%s' on '%s'",
					property,
					stateManager.getOwner()
			));
		}

		return terms.instantiate(stateManager.getOwner(), resolvedProperty);
	}

	/**
	 * Одиночный терм условия: значение свойства и флаг отрицания.
	 * Парсится из строки: {@code "!value"} → negated=true, {@code "value"} → negated=false.
	 */
	@Environment(EnvType.CLIENT)
	public record Term(String value, boolean negated) {

		private static final String NEGATED_PREFIX = "!";

		public Term(String value, boolean negated) {
			if (value.isEmpty()) {
				throw new IllegalArgumentException("Empty term");
			}

			this.value = value;
			this.negated = negated;
		}

		public static SimpleMultipartModelSelector.Term parse(String value) {
			return value.startsWith("!")
					? new SimpleMultipartModelSelector.Term(value.substring(1), true)
					: new SimpleMultipartModelSelector.Term(value, false);
		}

		@Override
		public String toString() {
			return negated ? "!" + value : value;
		}
	}

	/**
	 * Список термов для одного свойства блока, разделённых символом {@code |}.
	 * Метод {@link #instantiate} строит оптимальный предикат: если совпадающих значений
	 * меньше, чем несовпадающих — проверяет вхождение в список; иначе инвертирует логику
	 * и проверяет исключение из списка несовпадающих значений.
	 */
	@Environment(EnvType.CLIENT)
	public record Terms(List<SimpleMultipartModelSelector.Term> entries) {

		private static final char DELIMITER = '|';
		private static final Joiner JOINER = Joiner.on('|');
		private static final Splitter SPLITTER = Splitter.on('|');
		private static final Codec<String> CODEC = Codec.either(Codec.INT, Codec.BOOL)
				.flatComapMap(
						either -> (String) either.map(String::valueOf, String::valueOf),
						string -> DataResult.error(() -> "This codec can't be used for encoding")
				);
		public static final Codec<SimpleMultipartModelSelector.Terms> VALUE_CODEC =
				Codec.withAlternative(Codec.STRING, CODEC)
				     .comapFlatMap(
						     SimpleMultipartModelSelector.Terms::tryParse,
						     SimpleMultipartModelSelector.Terms::toString
				     );

		public Terms(List<SimpleMultipartModelSelector.Term> entries) {
			if (entries.isEmpty()) {
				throw new IllegalArgumentException("Empty value for property");
			}

			this.entries = entries;
		}

		public static DataResult<SimpleMultipartModelSelector.Terms> tryParse(String terms) {
			List<SimpleMultipartModelSelector.Term> parsed =
					SPLITTER.splitToStream(terms).map(SimpleMultipartModelSelector.Term::parse).toList();
			if (parsed.isEmpty()) {
				return DataResult.error(() -> "Empty value for property");
			}

			for (SimpleMultipartModelSelector.Term term : parsed) {
				if (term.value().isEmpty()) {
					return DataResult.error(() -> "Empty term in value '" + terms + "'");
				}
			}

			return DataResult.success(new SimpleMultipartModelSelector.Terms(parsed));
		}

		@Override
		public String toString() {
			return JOINER.join(entries);
		}

		public <O, S extends State<O, S>, T extends Comparable<T>> Predicate<S> instantiate(
				O object,
				Property<T> property
		) {
			Predicate<T> predicate =
					Util.anyOf(Lists.transform(entries, term -> this.instantiate(object, property, term)));
			List<T> allValues = new ArrayList<>(property.getValues());
			int totalCount = allValues.size();
			allValues.removeIf(predicate.negate());
			int matchCount = allValues.size();

			if (matchCount == 0) {
				SimpleMultipartModelSelector.LOGGER.warn(
						"Condition {} for property {} on {} is always false",
						new Object[]{this, property.getName(), object}
				);
				return state -> false;
			}

			int excludeCount = totalCount - matchCount;
			if (excludeCount == 0) {
				SimpleMultipartModelSelector.LOGGER.warn(
						"Condition {} for property {} on {} is always true",
						new Object[]{this, property.getName(), object}
				);
				return state -> true;
			}

			boolean useNegation;
			List<T> matchingValues;
			if (matchCount <= excludeCount) {
				useNegation = false;
				matchingValues = allValues;
			}
			else {
				useNegation = true;
				List<T> excludedValues = new ArrayList<>(property.getValues());
				excludedValues.removeIf(predicate);
				matchingValues = excludedValues;
			}

			if (matchingValues.size() == 1) {
				T singleValue = matchingValues.getFirst();
				return state -> {
					T stateValue = state.get(property);
					return singleValue.equals(stateValue) ^ useNegation;
				};
			}

			return state -> {
				T stateValue = state.get(property);
				return matchingValues.contains(stateValue) ^ useNegation;
			};
		}

		private <T extends Comparable<T>> T parseValue(Object object, Property<T> property, String value) {
			Optional<T> optional = property.parse(value);
			if (optional.isEmpty()) {
				throw new RuntimeException(String.format(
						Locale.ROOT,
						"Unknown value '%s' for property '%s' on '%s' in '%s'",
						value,
						property,
						object,
						this
				));
			}

			return optional.get();
		}

		private <T extends Comparable<T>> Predicate<T> instantiate(
				Object object,
				Property<T> property,
				SimpleMultipartModelSelector.Term term
		) {
			T comparable = parseValue(object, property, term.value());
			return term.negated()
					? value -> !value.equals(comparable)
					: value -> value.equals(comparable);
		}
	}
}
