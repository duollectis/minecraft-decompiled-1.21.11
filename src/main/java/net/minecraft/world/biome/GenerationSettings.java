package net.minecraft.world.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Util;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Настройки генерации биома: карверы и фичи, сгруппированные по шагам генерации.
 * Иммутабельны после построения через {@link Builder}.
 */
public class GenerationSettings {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final GenerationSettings INSTANCE = new GenerationSettings(RegistryEntryList.of(), List.of());

	public static final MapCodec<GenerationSettings> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			ConfiguredCarver.LIST_CODEC
				.promotePartial(Util.addPrefix("Carver: ", LOGGER::error))
				.fieldOf("carvers")
				.forGetter(settings -> settings.carvers),
			PlacedFeature.LISTS_CODEC
				.promotePartial(Util.addPrefix("Features: ", LOGGER::error))
				.fieldOf("features")
				.forGetter(settings -> settings.features)
		)
		.apply(instance, GenerationSettings::new)
	);

	private final RegistryEntryList<ConfiguredCarver<?>> carvers;
	private final List<RegistryEntryList<PlacedFeature>> features;
	private final Supplier<List<ConfiguredFeature<?, ?>>> flowerFeatures;
	private final Supplier<Set<PlacedFeature>> allowedFeatures;

	GenerationSettings(
		RegistryEntryList<ConfiguredCarver<?>> carvers,
		List<RegistryEntryList<PlacedFeature>> features
	) {
		this.carvers = carvers;
		this.features = features;
		this.flowerFeatures = Suppliers.memoize(
			() -> features.stream()
				.flatMap(RegistryEntryList::stream)
				.map(RegistryEntry::value)
				.flatMap(PlacedFeature::getDecoratedFeatures)
				.filter(feature -> feature.feature() == Feature.FLOWER)
				.collect(ImmutableList.toImmutableList())
		);
		this.allowedFeatures = Suppliers.memoize(
			() -> features.stream()
				.flatMap(RegistryEntryList::stream)
				.map(RegistryEntry::value)
				.collect(Collectors.toSet())
		);
	}

	public Iterable<RegistryEntry<ConfiguredCarver<?>>> getCarversForStep() {
		return carvers;
	}

	public List<ConfiguredFeature<?, ?>> getFlowerFeatures() {
		return flowerFeatures.get();
	}

	/**
	 * Возвращает иммутабельную копию списка фич, сгруппированных по шагам генерации.
	 * Каждый элемент списка соответствует одному {@link GenerationStep.Feature}.
	 */
	public List<RegistryEntryList<PlacedFeature>> getFeatures() {
		return List.copyOf(features);
	}

	public boolean isFeatureAllowed(PlacedFeature feature) {
		return allowedFeatures.get().contains(feature);
	}

	public static class Builder {

		private final List<RegistryEntry<ConfiguredCarver<?>>> carverStepsToCarvers = new ArrayList<>();
		private final List<List<RegistryEntry<PlacedFeature>>> indexedFeaturesList = new ArrayList<>();

		public GenerationSettings.Builder feature(
			GenerationStep.Feature featureStep,
			RegistryEntry<PlacedFeature> featureEntry
		) {
			return addFeature(featureStep.ordinal(), featureEntry);
		}

		public GenerationSettings.Builder addFeature(int ordinal, RegistryEntry<PlacedFeature> featureEntry) {
			fillFeaturesList(ordinal);
			indexedFeaturesList.get(ordinal).add(featureEntry);
			return this;
		}

		public GenerationSettings.Builder carver(RegistryEntry<ConfiguredCarver<?>> carverEntry) {
			carverStepsToCarvers.add(carverEntry);
			return this;
		}

		private void fillFeaturesList(int size) {
			while (indexedFeaturesList.size() <= size) {
				indexedFeaturesList.add(Lists.newArrayList());
			}
		}

		public GenerationSettings build() {
			return new GenerationSettings(
				RegistryEntryList.of(carverStepsToCarvers),
				indexedFeaturesList.stream()
					.map(RegistryEntryList::of)
					.collect(ImmutableList.toImmutableList())
			);
		}
	}

	/**
	 * Расширение {@link Builder}, которое принимает ключи реестра вместо прямых ссылок на записи.
	 * Используется при начальной загрузке биомов, когда реестры ещё не полностью инициализированы.
	 */
	public static class LookupBackedBuilder extends GenerationSettings.Builder {

		private final RegistryEntryLookup<PlacedFeature> placedFeatureLookup;
		private final RegistryEntryLookup<ConfiguredCarver<?>> configuredCarverLookup;

		public LookupBackedBuilder(
			RegistryEntryLookup<PlacedFeature> placedFeatureLookup,
			RegistryEntryLookup<ConfiguredCarver<?>> configuredCarverLookup
		) {
			this.placedFeatureLookup = placedFeatureLookup;
			this.configuredCarverLookup = configuredCarverLookup;
		}

		public GenerationSettings.LookupBackedBuilder feature(
			GenerationStep.Feature featureStep,
			RegistryKey<PlacedFeature> featureKey
		) {
			addFeature(featureStep.ordinal(), placedFeatureLookup.getOrThrow(featureKey));
			return this;
		}

		public GenerationSettings.LookupBackedBuilder carver(RegistryKey<ConfiguredCarver<?>> carverKey) {
			carver(configuredCarverLookup.getOrThrow(carverKey));
			return this;
		}
	}
}
