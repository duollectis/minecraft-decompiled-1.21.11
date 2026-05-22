package net.minecraft.world.gen.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSetKeys;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.*;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Конфигурация плоского генератора чанков. Описывает стек слоёв, биом,
 * наличие озёр и фич, а также опциональные переопределения структур.
 * <p>
 * Слои хранятся в порядке снизу вверх; {@link #updateLayerBlocks()} разворачивает
 * их в плоский список {@code BlockState} для быстрого доступа по Y-индексу.
 */
public class FlatChunkGeneratorConfig {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Codec<FlatChunkGeneratorConfig> CODEC = RecordCodecBuilder.<FlatChunkGeneratorConfig>create(
			instance -> instance.group(
					RegistryCodecs.entryList(RegistryKeys.STRUCTURE_SET)
					              .lenientOptionalFieldOf("structure_overrides")
					              .forGetter(config -> config.structureOverrides),
					FlatChunkGeneratorLayer.CODEC
					                      .listOf()
					                      .fieldOf("layers")
					                      .forGetter(FlatChunkGeneratorConfig::getLayers),
					Codec.BOOL.fieldOf("lakes").orElse(false).forGetter(config -> config.hasLakes),
					Codec.BOOL.fieldOf("features").orElse(false).forGetter(config -> config.hasFeatures),
					Biome.REGISTRY_CODEC
					     .lenientOptionalFieldOf("biome")
					     .orElseGet(Optional::empty)
					     .forGetter(config -> Optional.of(config.biome)),
					RegistryOps.<Biome, FlatChunkGeneratorConfig>getEntryCodec(BiomeKeys.PLAINS),
					RegistryOps.<PlacedFeature, FlatChunkGeneratorConfig>getEntryCodec(MiscPlacedFeatures.LAKE_LAVA_UNDERGROUND),
					RegistryOps.<PlacedFeature, FlatChunkGeneratorConfig>getEntryCodec(MiscPlacedFeatures.LAKE_LAVA_SURFACE)
			).apply(instance, FlatChunkGeneratorConfig::new)
	).comapFlatMap(FlatChunkGeneratorConfig::checkHeight, Function.identity()).stable();

	private final Optional<RegistryEntryList<StructureSet>> structureOverrides;
	private final List<FlatChunkGeneratorLayer> layers = Lists.newArrayList();
	private final RegistryEntry<Biome> biome;
	private final List<BlockState> layerBlocks;
	private boolean hasNoTerrain;
	private boolean hasFeatures;
	private boolean hasLakes;
	private final List<RegistryEntry<PlacedFeature>> features;

	private static DataResult<FlatChunkGeneratorConfig> checkHeight(FlatChunkGeneratorConfig config) {
		int totalHeight = config.layers.stream().mapToInt(FlatChunkGeneratorLayer::getThickness).sum();

		return totalHeight > DimensionType.MAX_HEIGHT
				? DataResult.error(() -> "Sum of layer heights is > " + DimensionType.MAX_HEIGHT, config)
				: DataResult.success(config);
	}

	private FlatChunkGeneratorConfig(
			Optional<RegistryEntryList<StructureSet>> structureOverrides,
			List<FlatChunkGeneratorLayer> layers,
			boolean lakes,
			boolean features,
			Optional<RegistryEntry<Biome>> biome,
			RegistryEntry.Reference<Biome> fallback,
			RegistryEntry<PlacedFeature> undergroundLavaLakeFeature,
			RegistryEntry<PlacedFeature> surfaceLavaLakeFeature
	) {
		this(
				structureOverrides,
				getBiome(biome, fallback),
				List.of(undergroundLavaLakeFeature, surfaceLavaLakeFeature)
		);

		if (lakes) {
			enableLakes();
		}

		if (features) {
			enableFeatures();
		}

		this.layers.addAll(layers);
		updateLayerBlocks();
	}

	/**
	 * Возвращает биом из Optional, либо fallback (Plains) с предупреждением в лог.
	 * Unchecked cast безопасен: тип гарантирован структурой Codec.
	 */
	@SuppressWarnings("unchecked")
	private static RegistryEntry<Biome> getBiome(
			Optional<? extends RegistryEntry<Biome>> biome,
			RegistryEntry<Biome> fallback
	) {
		if (biome.isEmpty()) {
			LOGGER.error("Unknown biome, defaulting to plains");
			return fallback;
		}

		return (RegistryEntry<Biome>) biome.get();
	}

	public FlatChunkGeneratorConfig(
			Optional<RegistryEntryList<StructureSet>> structureOverrides,
			RegistryEntry<Biome> biome,
			List<RegistryEntry<PlacedFeature>> features
	) {
		this.structureOverrides = structureOverrides;
		this.biome = biome;
		this.layerBlocks = Lists.newArrayList();
		this.features = features;
	}

	public FlatChunkGeneratorConfig with(
			List<FlatChunkGeneratorLayer> layers,
			Optional<RegistryEntryList<StructureSet>> structureOverrides,
			RegistryEntry<Biome> biome
	) {
		FlatChunkGeneratorConfig copy = new FlatChunkGeneratorConfig(structureOverrides, biome, this.features);

		for (FlatChunkGeneratorLayer layer : layers) {
			copy.layers.add(new FlatChunkGeneratorLayer(layer.getThickness(), layer.getBlockState().getBlock()));
			copy.updateLayerBlocks();
		}

		if (hasFeatures) {
			copy.enableFeatures();
		}

		if (hasLakes) {
			copy.enableLakes();
		}

		return copy;
	}

	public void enableFeatures() {
		hasFeatures = true;
	}

	public void enableLakes() {
		hasLakes = true;
	}

	/**
	 * Создаёт {@link GenerationSettings} для данного биома с учётом конфигурации плоского мира.
	 * <p>
	 * Если переданный биом не совпадает с биомом конфига — возвращает его родные настройки генерации.
	 * Иначе строит кастомные настройки: добавляет лавовые озёра (если включены), фичи биома
	 * (кроме структур и, опционально, озёр), а также FILL_LAYER-фичи для непрозрачных блоков слоёв.
	 */
	public GenerationSettings createGenerationSettings(RegistryEntry<Biome> biomeEntry) {
		if (!biomeEntry.equals(biome)) {
			return biomeEntry.value().getGenerationSettings();
		}

		GenerationSettings biomeSettings = getBiome().value().getGenerationSettings();
		GenerationSettings.Builder builder = new GenerationSettings.Builder();

		if (hasLakes) {
			for (RegistryEntry<PlacedFeature> lavaLake : features) {
				builder.feature(GenerationStep.Feature.LAKES, lavaLake);
			}
		}

		boolean shouldAddFeatures = (!hasNoTerrain || biomeEntry.matchesKey(BiomeKeys.THE_VOID)) && hasFeatures;

		if (shouldAddFeatures) {
			List<RegistryEntryList<PlacedFeature>> biomeFeatures = biomeSettings.getFeatures();

			for (int step = 0; step < biomeFeatures.size(); step++) {
				if (step == GenerationStep.Feature.UNDERGROUND_STRUCTURES.ordinal()
						|| step == GenerationStep.Feature.SURFACE_STRUCTURES.ordinal()
						|| (hasLakes && step == GenerationStep.Feature.LAKES.ordinal())) {
					continue;
				}

				for (RegistryEntry<PlacedFeature> feature : biomeFeatures.get(step)) {
					builder.addFeature(step, feature);
				}
			}
		}

		List<BlockState> blocks = getLayerBlocks();

		for (int layerIndex = 0; layerIndex < blocks.size(); layerIndex++) {
			BlockState blockState = blocks.get(layerIndex);

			if (Heightmap.Type.MOTION_BLOCKING.getBlockPredicate().test(blockState)) {
				continue;
			}

			blocks.set(layerIndex, null);
			builder.feature(
					GenerationStep.Feature.TOP_LAYER_MODIFICATION,
					PlacedFeatures.createEntry(Feature.FILL_LAYER, new FillLayerFeatureConfig(layerIndex, blockState))
			);
		}

		return builder.build();
	}

	public Optional<RegistryEntryList<StructureSet>> getStructureOverrides() {
		return structureOverrides;
	}

	public RegistryEntry<Biome> getBiome() {
		return biome;
	}

	public List<FlatChunkGeneratorLayer> getLayers() {
		return layers;
	}

	public List<BlockState> getLayerBlocks() {
		return layerBlocks;
	}

	/**
	 * Перестраивает плоский список {@link BlockState} из стека слоёв.
	 * Вызывается после каждого изменения {@link #layers}.
	 */
	public void updateLayerBlocks() {
		layerBlocks.clear();

		for (FlatChunkGeneratorLayer layer : layers) {
			for (int repeat = 0; repeat < layer.getThickness(); repeat++) {
				layerBlocks.add(layer.getBlockState());
			}
		}

		hasNoTerrain = layerBlocks.stream().allMatch(state -> state.isOf(Blocks.AIR));
	}

	public static FlatChunkGeneratorConfig getDefaultConfig(
			RegistryEntryLookup<Biome> biomeLookup,
			RegistryEntryLookup<StructureSet> structureSetLookup,
			RegistryEntryLookup<PlacedFeature> featureLookup
	) {
		RegistryEntryList<StructureSet> defaultStructures = RegistryEntryList.of(
				structureSetLookup.getOrThrow(StructureSetKeys.STRONGHOLDS),
				structureSetLookup.getOrThrow(StructureSetKeys.VILLAGES)
		);
		FlatChunkGeneratorConfig config = new FlatChunkGeneratorConfig(
				Optional.of(defaultStructures), getPlains(biomeLookup), getLavaLakes(featureLookup)
		);

		config.getLayers().add(new FlatChunkGeneratorLayer(1, Blocks.BEDROCK));
		config.getLayers().add(new FlatChunkGeneratorLayer(2, Blocks.DIRT));
		config.getLayers().add(new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK));
		config.updateLayerBlocks();

		return config;
	}

	public static RegistryEntry<Biome> getPlains(RegistryEntryLookup<Biome> biomeLookup) {
		return biomeLookup.getOrThrow(BiomeKeys.PLAINS);
	}

	public static List<RegistryEntry<PlacedFeature>> getLavaLakes(RegistryEntryLookup<PlacedFeature> featureLookup) {
		return List.of(
				featureLookup.getOrThrow(MiscPlacedFeatures.LAKE_LAVA_UNDERGROUND),
				featureLookup.getOrThrow(MiscPlacedFeatures.LAKE_LAVA_SURFACE)
		);
	}
}
