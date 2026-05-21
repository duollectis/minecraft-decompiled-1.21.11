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

@Environment(EnvType.CLIENT)
/**
 * {@code SimpleMultipartModelSelector}.
 */
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
		List<Predicate<S>> list = new ArrayList<>(this.tests.size());
		this.tests.forEach((property, terms) -> list.add(init(stateManager, property, terms)));
		return Util.allOf(list);
	}

	private static <O, S extends State<O, S>> Predicate<S> init(
			StateManager<O, S> stateManager,
			String property,
			SimpleMultipartModelSelector.Terms terms
	) {
		Property<?> property2 = stateManager.getProperty(property);
		if (property2 == null) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"Unknown property '%s' on '%s'",
					property,
					stateManager.getOwner()
			));
		}
		else {
			return terms.instantiate(stateManager.getOwner(), property2);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Term}.
	 */
	public record Term(String value, boolean negated) {

		private static final String NEGATED_PREFIX = "!";

		public Term(String value, boolean negated) {
			if (value.isEmpty()) {
				throw new IllegalArgumentException("Empty term");
			}
			else {
				this.value = value;
				this.negated = negated;
			}
		}

		public static SimpleMultipartModelSelector.Term parse(String value) {
			return value.startsWith("!") ? new SimpleMultipartModelSelector.Term(value.substring(1), true)
			                             : new SimpleMultipartModelSelector.Term(value, false);
		}

		@Override
		public String toString() {
			return this.negated ? "!" + this.value : this.value;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Terms}.
	 */
	public record Terms(List<SimpleMultipartModelSelector.Term> entries) {

		private static final char DELIMITER = '|';
		private static final Joiner JOINER = Joiner.on('|');
		private static final Splitter SPLITTER = Splitter.on('|');
		private static final Codec<String> CODEC = Codec.either(Codec.INT, Codec.BOOL)
		                                                .flatComapMap(
				                                                either -> (String) either.map(
						                                                String::valueOf,
						                                                String::valueOf
				                                                ),
				                                                string -> DataResult.error(() -> "This codec can't be used for encoding")
		                                                );
		public static final Codec<SimpleMultipartModelSelector.Terms>
				VALUE_CODEC =
				Codec.withAlternative(Codec.STRING, CODEC)
				     .comapFlatMap(
						     SimpleMultipartModelSelector.Terms::tryParse,
						     SimpleMultipartModelSelector.Terms::toString
				     );

		public Terms(List<SimpleMultipartModelSelector.Term> entries) {
			if (entries.isEmpty()) {
				throw new IllegalArgumentException("Empty value for property");
			}
			else {
				this.entries = entries;
			}
		}

		public static DataResult<SimpleMultipartModelSelector.Terms> tryParse(String terms) {
			List<SimpleMultipartModelSelector.Term>
					list =
					SPLITTER.splitToStream(terms).map(SimpleMultipartModelSelector.Term::parse).toList();
			if (list.isEmpty()) {
				return DataResult.error(() -> "Empty value for property");
			}
			else {
				for (SimpleMultipartModelSelector.Term term : list) {
					if (term.value.isEmpty()) {
						return DataResult.error(() -> "Empty term in value '" + terms + "'");
					}
				}

				return DataResult.success(new SimpleMultipartModelSelector.Terms(list));
			}
		}

		@Override
		public String toString() {
			return JOINER.join(this.entries);
		}

		public <O, S extends State<O, S>, T extends Comparable<T>> Predicate<S> instantiate(
				O object,
				Property<T> property
		) {
			Predicate<T>
					predicate =
					Util.anyOf(Lists.transform(this.entries, term -> this.instantiate(object, property, term)));
			List<T> list = new ArrayList<>(property.getValues());
			int i = list.size();
			list.removeIf(predicate.negate());
			int j = list.size();
			if (j == 0) {
				SimpleMultipartModelSelector.LOGGER.warn(
						"Condition {} for property {} on {} is always false",
						new Object[]{this, property.getName(), object}
				);
				return state -> false;
			}
			else {
				int k = i - j;
				if (k == 0) {
					SimpleMultipartModelSelector.LOGGER.warn(
							"Condition {} for property {} on {} is always true",
							new Object[]{this, property.getName(), object}
					);
					return state -> true;
				}
				else {
					boolean bl;
					List<T> list2;
					if (j <= k) {
						bl = false;
						list2 = list;
					}
					else {
						bl = true;
						List<T> list3 = new ArrayList<>(property.getValues());
						list3.removeIf(predicate);
						list2 = list3;
					}

					if (list2.size() == 1) {
						T comparable = (T) list2.getFirst();
						return state -> {
							T comparable2 = state.get(property);
							return comparable.equals(comparable2) ^ bl;
						};
					}
					else {
						return state -> {
							T comparablex = state.get(property);
							return list2.contains(comparablex) ^ bl;
						};
					}
				}
			}
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
			else {
				return optional.get();
			}
		}

		private <T extends Comparable<T>> Predicate<T> instantiate(
				Object object,
				Property<T> property,
				SimpleMultipartModelSelector.Term term
		) {
			T comparable = this.parseValue(object, property, term.value);
			return term.negated ? value -> !value.equals(comparable) : value -> value.equals(comparable);
		}
	}
}
