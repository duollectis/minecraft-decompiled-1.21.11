package net.minecraft.client.data;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelVariantOperator;
import net.minecraft.client.render.model.json.WeightedVariant;
import net.minecraft.state.property.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockStateVariantMap}.
 */
public abstract class BlockStateVariantMap<V> {

	private final Map<PropertiesMap, V> variants = new HashMap<>();

	protected void register(PropertiesMap properties, V variant) {
		V object = this.variants.put(properties, variant);
		if (object != null) {
			throw new IllegalStateException("Value " + properties + " is already defined");
		}
	}

	Map<PropertiesMap, V> getVariants() {
		this.validate();
		return Map.copyOf(this.variants);
	}

	private void validate() {
		List<Property<?>> list = this.getProperties();
		Stream<PropertiesMap> stream = Stream.of(PropertiesMap.EMPTY);

		for (Property<?> property : list) {
			stream = stream.flatMap(propertiesMap -> property.stream().map(propertiesMap::withValue));
		}

		List<PropertiesMap> list2 = stream.filter(propertiesMap -> !this.variants.containsKey(propertiesMap)).toList();
		if (!list2.isEmpty()) {
			throw new IllegalStateException("Missing definition for properties: " + list2);
		}
	}

	abstract List<Property<?>> getProperties();

	public static <T1 extends Comparable<T1>> BlockStateVariantMap.SingleProperty<WeightedVariant, T1> models(Property<T1> property) {
		return new BlockStateVariantMap.SingleProperty<>(property);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> BlockStateVariantMap.DoubleProperty<WeightedVariant, T1, T2> models(
			Property<T1> property1, Property<T2> property2
	) {
		return new BlockStateVariantMap.DoubleProperty<>(property1, property2);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> BlockStateVariantMap.TripleProperty<WeightedVariant, T1, T2, T3> models(
			Property<T1> property1, Property<T2> property2, Property<T3> property3
	) {
		return new BlockStateVariantMap.TripleProperty<>(property1, property2, property3);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> BlockStateVariantMap.QuadrupleProperty<WeightedVariant, T1, T2, T3, T4> models(
			Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
	) {
		return new BlockStateVariantMap.QuadrupleProperty<>(property1, property2, property3, property4);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> BlockStateVariantMap.QuintupleProperty<WeightedVariant, T1, T2, T3, T4, T5> models(
			Property<T1> property1,
			Property<T2> property2,
			Property<T3> property3,
			Property<T4> property4,
			Property<T5> property5
	) {
		return new BlockStateVariantMap.QuintupleProperty<>(property1, property2, property3, property4, property5);
	}

	public static <T1 extends Comparable<T1>> BlockStateVariantMap.SingleProperty<ModelVariantOperator, T1> operations(
			Property<T1> property
	) {
		return new BlockStateVariantMap.SingleProperty<>(property);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> BlockStateVariantMap.DoubleProperty<ModelVariantOperator, T1, T2> operations(
			Property<T1> property1, Property<T2> property2
	) {
		return new BlockStateVariantMap.DoubleProperty<>(property1, property2);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> BlockStateVariantMap.TripleProperty<ModelVariantOperator, T1, T2, T3> operations(
			Property<T1> property1, Property<T2> property2, Property<T3> property3
	) {
		return new BlockStateVariantMap.TripleProperty<>(property1, property2, property3);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> BlockStateVariantMap.QuadrupleProperty<ModelVariantOperator, T1, T2, T3, T4> operations(
			Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
	) {
		return new BlockStateVariantMap.QuadrupleProperty<>(property1, property2, property3, property4);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> BlockStateVariantMap.QuintupleProperty<ModelVariantOperator, T1, T2, T3, T4, T5> operations(
			Property<T1> property1,
			Property<T2> property2,
			Property<T3> property3,
			Property<T4> property4,
			Property<T5> property5
	) {
		return new BlockStateVariantMap.QuintupleProperty<>(property1, property2, property3, property4, property5);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DoubleProperty}.
	 */
	public static class DoubleProperty<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends BlockStateVariantMap<V> {

		private final Property<T1> first;
		private final Property<T2> second;

		DoubleProperty(Property<T1> first, Property<T2> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public List<Property<?>> getProperties() {
			return List.of(this.first, this.second);
		}

		public BlockStateVariantMap.DoubleProperty<V, T1, T2> register(T1 firstProperty, T2 secondProperty, V variant) {
			PropertiesMap
					propertiesMap =
					PropertiesMap.withValues(
							this.first.createValue(firstProperty),
							this.second.createValue(secondProperty)
					);
			this.register(propertiesMap, variant);
			return this;
		}

		public BlockStateVariantMap<V> generate(BiFunction<T1, T2, V> variantFactory) {
			this.first
					.getValues()
					.forEach(
							firstValue -> this.second
									.getValues()
									.forEach(secondValue -> this.register(
											(T1) firstValue,
											(T2) secondValue,
											variantFactory.apply((T1) firstValue, (T2) secondValue)
									))
					);
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code QuadrupleProperty}.
	 */
	public static class QuadrupleProperty<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
			extends BlockStateVariantMap<V> {

		private final Property<T1> first;
		private final Property<T2> second;
		private final Property<T3> third;
		private final Property<T4> fourth;

		QuadrupleProperty(Property<T1> first, Property<T2> second, Property<T3> third, Property<T4> fourth) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
		}

		@Override
		public List<Property<?>> getProperties() {
			return List.of(this.first, this.second, this.third, this.fourth);
		}

		public BlockStateVariantMap.QuadrupleProperty<V, T1, T2, T3, T4> register(
				T1 firstProperty, T2 secondProperty, T3 thirdProperty, T4 fourthProperty, V variant
		) {
			PropertiesMap propertiesMap = PropertiesMap.withValues(
					this.first.createValue(firstProperty),
					this.second.createValue(secondProperty),
					this.third.createValue(thirdProperty),
					this.fourth.createValue(fourthProperty)
			);
			this.register(propertiesMap, variant);
			return this;
		}

		public BlockStateVariantMap<V> generate(Function4<T1, T2, T3, T4, V> variantFactory) {
			this.first
					.getValues()
					.forEach(
							firstValue -> this.second
									.getValues()
									.forEach(
											secondValue -> this.third
													.getValues()
													.forEach(
															thirdValue -> this.fourth
																	.getValues()
																	.forEach(
																			fourthValue -> this.register(
																					(T1) firstValue,
																					(T2) secondValue,
																					(T3) thirdValue,
																					(T4) fourthValue,
																					(V) variantFactory.apply(
																							firstValue,
																							secondValue,
																							thirdValue,
																							fourthValue
																					)
																			)
																	)
													)
									)
					);
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code QuintupleProperty}.
	 */
	public static class QuintupleProperty<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
			extends BlockStateVariantMap<V> {

		private final Property<T1> first;
		private final Property<T2> second;
		private final Property<T3> third;
		private final Property<T4> fourth;
		private final Property<T5> fifth;

		QuintupleProperty(
				Property<T1> first,
				Property<T2> second,
				Property<T3> third,
				Property<T4> fourth,
				Property<T5> fifth
		) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
			this.fifth = fifth;
		}

		@Override
		public List<Property<?>> getProperties() {
			return List.of(this.first, this.second, this.third, this.fourth, this.fifth);
		}

		public BlockStateVariantMap.QuintupleProperty<V, T1, T2, T3, T4, T5> register(
				T1 firstProperty, T2 secondProperty, T3 thirdProperty, T4 fourthProperty, T5 fifthProperty, V variant
		) {
			PropertiesMap propertiesMap = PropertiesMap.withValues(
					this.first.createValue(firstProperty),
					this.second.createValue(secondProperty),
					this.third.createValue(thirdProperty),
					this.fourth.createValue(fourthProperty),
					this.fifth.createValue(fifthProperty)
			);
			this.register(propertiesMap, variant);
			return this;
		}

		public BlockStateVariantMap<V> generate(Function5<T1, T2, T3, T4, T5, V> variantFactory) {
			this.first
					.getValues()
					.forEach(
							firstValue -> this.second
									.getValues()
									.forEach(
											secondValue -> this.third
													.getValues()
													.forEach(
															thirdValue -> this.fourth
																	.getValues()
																	.forEach(
																			fourthValue -> this.fifth
																					.getValues()
																					.forEach(
																							fifthValue -> this.register(
																									(T1) firstValue,
																									(T2) secondValue,
																									(T3) thirdValue,
																									(T4) fourthValue,
																									(T5) fifthValue,
																									(V) variantFactory.apply(
																											firstValue,
																											secondValue,
																											thirdValue,
																											fourthValue,
																											fifthValue
																									)
																							)
																					)
																	)
													)
									)
					);
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SingleProperty}.
	 */
	public static class SingleProperty<V, T1 extends Comparable<T1>> extends BlockStateVariantMap<V> {

		private final Property<T1> property;

		SingleProperty(Property<T1> property) {
			this.property = property;
		}

		@Override
		public List<Property<?>> getProperties() {
			return List.of(this.property);
		}

		public BlockStateVariantMap.SingleProperty<V, T1> register(T1 property, V variant) {
			PropertiesMap propertiesMap = PropertiesMap.withValues(this.property.createValue(property));
			this.register(propertiesMap, variant);
			return this;
		}

		public BlockStateVariantMap<V> generate(Function<T1, V> variantFactory) {
			this.property.getValues().forEach(value -> this.register((T1) value, variantFactory.apply((T1) value)));
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TripleProperty}.
	 */
	public static class TripleProperty<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends BlockStateVariantMap<V> {

		private final Property<T1> first;
		private final Property<T2> second;
		private final Property<T3> third;

		TripleProperty(Property<T1> first, Property<T2> second, Property<T3> third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		@Override
		public List<Property<?>> getProperties() {
			return List.of(this.first, this.second, this.third);
		}

		public BlockStateVariantMap.TripleProperty<V, T1, T2, T3> register(
				T1 firstProperty,
				T2 secondProperty,
				T3 thirdProperty,
				V variant
		) {
			PropertiesMap propertiesMap = PropertiesMap.withValues(
					this.first.createValue(firstProperty),
					this.second.createValue(secondProperty),
					this.third.createValue(thirdProperty)
			);
			this.register(propertiesMap, variant);
			return this;
		}

		public BlockStateVariantMap<V> generate(Function3<T1, T2, T3, V> variantFactory) {
			this.first
					.getValues()
					.forEach(
							firstValue -> this.second
									.getValues()
									.forEach(
											secondValue -> this.third
													.getValues()
													.forEach(
															thirdValue -> this.register(
																	(T1) firstValue,
																	(T2) secondValue,
																	(T3) thirdValue,
																	(V) variantFactory.apply(
																			firstValue,
																			secondValue,
																			thirdValue
																	)
															)
													)
									)
					);
			return this;
		}
	}
}
