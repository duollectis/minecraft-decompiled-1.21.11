package net.minecraft.world.attribute;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.attribute.timeline.Timeline;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * Реализация {@link EnvironmentAttributeAccess} для реального мира.
 * Вычисляет значения атрибутов, применяя цепочку функций модификации:
 * константных (из измерения), временны́х (из таймлайнов и погоды)
 * и позиционных (из биомов).
 * <p>
 * Константные функции в начале цепочки вычисляются один раз и кешируются.
 * Временны́е функции пересчитываются каждый тик.
 * Позиционные функции пересчитываются при каждом запросе с позицией.
 */
public class WorldEnvironmentAttributeAccess implements EnvironmentAttributeAccess {

	private final Map<EnvironmentAttribute<?>, Entry<?>> entries = new Reference2ObjectOpenHashMap<>();

	@SuppressWarnings("unchecked")
	WorldEnvironmentAttributeAccess(Map<EnvironmentAttribute<?>, List<EnvironmentAttributeFunction<?>>> modificationsByAttribute) {
		modificationsByAttribute.forEach((attribute, mods) ->
			entries.put(
				attribute,
				computeEntry(
					(EnvironmentAttribute<Object>) attribute,
					(List<EnvironmentAttributeFunction<Object>>) (List<?>) mods
				)
			)
		);
	}

	private <Value> Entry<Value> computeEntry(
		EnvironmentAttribute<Value> attribute,
		List<? extends EnvironmentAttributeFunction<?>> mods
	) {
		@SuppressWarnings("unchecked")
		List<EnvironmentAttributeFunction<Value>> functions = new ArrayList<>(
			(Collection<? extends EnvironmentAttributeFunction<Value>>) mods
		);
		Value defaultValue = attribute.getDefaultValue();

		// Применяем константные функции в начале цепочки сразу — они не меняются
		while (!functions.isEmpty()) {
			if (!(functions.getFirst() instanceof EnvironmentAttributeFunction.Constant<Value> constant)) {
				break;
			}

			defaultValue = constant.applyConstant(defaultValue);
			functions.removeFirst();
		}

		boolean hasPositional = functions.stream()
			.anyMatch(fn -> fn instanceof EnvironmentAttributeFunction.Positional);

		return new Entry<>(attribute, defaultValue, List.copyOf(functions), hasPositional);
	}

	public static Builder builder() {
		return new Builder();
	}

	static void addModifiersFromWorld(Builder builder, World world) {
		DynamicRegistryManager registryManager = world.getRegistryManager();
		BiomeAccess biomeAccess = world.getBiomeAccess();
		LongSupplier timeSupplier = world::getTimeOfDay;

		addModifiersFromDimension(builder, world.getDimension());
		addModifiersFromBiomes(builder, registryManager.getOrThrow(RegistryKeys.BIOME), biomeAccess);

		world.getDimension()
			.timelines()
			.forEach(timeline -> builder.addFromTimeline((RegistryEntry<Timeline>) timeline, timeSupplier));

		if (world.canHaveWeather()) {
			WeatherAttributes.addWeatherAttributes(builder, WeatherAttributes.WeatherAccess.ofWorld(world));
		}
	}

	private static void addModifiersFromDimension(Builder builder, DimensionType dimensionType) {
		builder.addFromMap(dimensionType.attributes());
	}

	private static void addModifiersFromBiomes(
		Builder builder,
		RegistryWrapper<Biome> biomeRegistry,
		BiomeAccess biomeAccess
	) {
		Stream<EnvironmentAttribute<?>> biomeAttributes = biomeRegistry.streamEntries()
			.flatMap(biome -> biome.value().getEnvironmentAttributes().keySet().stream())
			.distinct();

		biomeAttributes.forEach(attribute -> addModifiersFromBiomes(builder, attribute, biomeAccess));
	}

	private static <Value> void addModifiersFromBiomes(
		Builder builder,
		EnvironmentAttribute<Value> attribute,
		BiomeAccess biomeAccess
	) {
		builder.positional(attribute, (value, pos, weightedAttributeList) -> {
			if (weightedAttributeList != null && attribute.isInterpolated()) {
				return weightedAttributeList.interpolate(attribute, value);
			}

			RegistryEntry<Biome> biome = biomeAccess.getBiomeForNoiseGen(pos.x, pos.y, pos.z);
			return biome.value().getEnvironmentAttributes().apply(attribute, value);
		});
	}

	/** Инвалидирует кеш всех записей для следующего тика. */
	public void tick() {
		entries.values().forEach(Entry::tick);
	}

	@SuppressWarnings("unchecked")
	private <Value> @Nullable Entry<Value> getEntry(EnvironmentAttribute<Value> attribute) {
		return (Entry<Value>) entries.get(attribute);
	}

	@Override
	public <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute) {
		if (SharedConstants.isDevelopment && attribute.isPositional()) {
			throw new IllegalStateException(
				"Position must always be provided for positional attribute " + attribute
			);
		}

		Entry<Value> entry = getEntry(attribute);
		return entry == null ? attribute.getDefaultValue() : entry.get();
	}

	@Override
	public <Value> Value getAttributeValue(
		EnvironmentAttribute<Value> attribute,
		Vec3d pos,
		@Nullable WeightedAttributeList pool
	) {
		Entry<Value> entry = getEntry(attribute);
		return entry == null ? attribute.getDefaultValue() : entry.getAt(pos, pool);
	}

	@VisibleForTesting
	<Value> Value getDefaultValue(EnvironmentAttribute<Value> attribute) {
		Entry<Value> entry = getEntry(attribute);
		return entry != null ? entry.defaultValue : attribute.getDefaultValue();
	}

	@VisibleForTesting
	boolean isPositional(EnvironmentAttribute<?> attribute) {
		Entry<?> entry = getEntry(attribute);
		return entry != null && entry.positional;
	}

	/**
	 * Билдер для создания {@link WorldEnvironmentAttributeAccess}.
	 * Позволяет добавлять функции модификации из различных источников:
	 * карт атрибутов, таймлайнов, погоды и биомов.
	 */
	public static class Builder {

		private final Map<EnvironmentAttribute<?>, List<EnvironmentAttributeFunction<?>>> modifications = new HashMap<>();

		Builder() {
		}

		/** Добавляет все модификаторы из реального мира (измерение, биомы, таймлайны, погода). */
		public Builder world(World world) {
			WorldEnvironmentAttributeAccess.addModifiersFromWorld(this, world);
			return this;
		}

		/** Добавляет все атрибуты из карты как константные модификаторы. */
		public Builder addFromMap(EnvironmentAttributeMap attributes) {
			for (EnvironmentAttribute<?> attribute : attributes.keySet()) {
				addFromMap(attribute, attributes);
			}

			return this;
		}

		private <Value> Builder addFromMap(EnvironmentAttribute<Value> attribute, EnvironmentAttributeMap attributeMap) {
			EnvironmentAttributeMap.Entry<Value, ?> entry = attributeMap.getEntry(attribute);
			if (entry == null) {
				throw new IllegalArgumentException("Missing attribute " + attribute);
			}

			return constant(attribute, entry::apply);
		}

		public <Value> Builder constant(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeFunction.Constant<Value> mod
		) {
			return addModification(attribute, mod);
		}

		public <Value> Builder timeBased(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeFunction.TimeBased<Value> mod
		) {
			return addModification(attribute, mod);
		}

		public <Value> Builder positional(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeFunction.Positional<Value> mod
		) {
			return addModification(attribute, mod);
		}

		private <Value> Builder addModification(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeFunction<Value> mod
		) {
			modifications.computeIfAbsent(attribute, key -> new ArrayList<>()).add(mod);
			return this;
		}

		/** Добавляет временны́е модификаторы из таймлайна для всех его атрибутов. */
		public Builder addFromTimeline(RegistryEntry<Timeline> timeline, LongSupplier timeSupplier) {
			for (EnvironmentAttribute<?> attribute : timeline.value().getAttributes()) {
				addModificationFromTimeline(timeline, attribute, timeSupplier);
			}

			return this;
		}

		private <Value> void addModificationFromTimeline(
			RegistryEntry<Timeline> timeline,
			EnvironmentAttribute<Value> attribute,
			LongSupplier timeSupplier
		) {
			timeBased(attribute, timeline.value().getModification(attribute, timeSupplier));
		}

		/** Собирает {@link WorldEnvironmentAttributeAccess} из накопленных модификаторов. */
		public WorldEnvironmentAttributeAccess build() {
			return new WorldEnvironmentAttributeAccess(modifications);
		}
	}

	/**
	 * Запись для одного атрибута: хранит предвычисленное базовое значение,
	 * список оставшихся функций и кешированное текущее значение.
	 */
	static class Entry<Value> {

		private final EnvironmentAttribute<Value> attribute;
		final Value defaultValue;
		private final List<EnvironmentAttributeFunction<Value>> modifications;
		final boolean positional;
		private @Nullable Value cachedValue;
		private int age;

		Entry(
			EnvironmentAttribute<Value> attribute,
			Value defaultValue,
			List<EnvironmentAttributeFunction<Value>> modifications,
			boolean positional
		) {
			this.attribute = attribute;
			this.defaultValue = defaultValue;
			this.modifications = modifications;
			this.positional = positional;
		}

		/** Инвалидирует кеш и увеличивает счётчик тиков. */
		public void tick() {
			cachedValue = null;
			age++;
		}

		/** Возвращает глобальное значение (без позиции), используя кеш. */
		public Value get() {
			if (cachedValue != null) {
				return cachedValue;
			}

			cachedValue = compute();
			return cachedValue;
		}

		/** Возвращает значение для заданной позиции. Если не позиционный — использует кеш. */
		public Value getAt(Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList) {
			return positional ? computeAt(pos, weightedAttributeList) : get();
		}

		private Value computeAt(Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList) {
			Value value = defaultValue;

			for (EnvironmentAttributeFunction<Value> function : modifications) {
				value = switch (function) {
					case EnvironmentAttributeFunction.Constant<Value> constant ->
						constant.applyConstant(value);
					case EnvironmentAttributeFunction.TimeBased<Value> timeBased ->
						timeBased.applyTimeBased(value, age);
					case EnvironmentAttributeFunction.Positional<Value> positionalFn ->
						positionalFn.applyPositional(value, Objects.requireNonNull(pos), weightedAttributeList);
				};
			}

			return attribute.clamp(value);
		}

		private Value compute() {
			Value value = defaultValue;

			for (EnvironmentAttributeFunction<Value> function : modifications) {
				value = switch (function) {
					case EnvironmentAttributeFunction.Constant<Value> constant ->
						constant.applyConstant(value);
					case EnvironmentAttributeFunction.TimeBased<Value> timeBased ->
						timeBased.applyTimeBased(value, age);
					// Позиционные функции пропускаются при глобальном вычислении
					case EnvironmentAttributeFunction.Positional<Value> ignored -> value;
				};
			}

			return attribute.clamp(value);
		}
	}
}
