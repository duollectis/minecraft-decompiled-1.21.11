package net.minecraft.world.dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.level.LevelProperties;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Хранилище реестра параметров измерений.
 * Управляет набором всех зарегистрированных измерений мира, включая ванильные
 * (Overworld, Nether, End) и пользовательские. Обеспечивает валидацию наличия
 * обязательного измерения Overworld и определение жизненного цикла реестра.
 */
public record DimensionOptionsRegistryHolder(Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions) {

	public static final MapCodec<DimensionOptionsRegistryHolder> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.unboundedMap(RegistryKey.createCodec(RegistryKeys.DIMENSION), DimensionOptions.CODEC)
				.fieldOf("dimensions")
				.forGetter(DimensionOptionsRegistryHolder::dimensions)
		).apply(instance, instance.stable(DimensionOptionsRegistryHolder::new))
	);

	private static final Set<RegistryKey<DimensionOptions>> VANILLA_KEYS = ImmutableSet.of(
		DimensionOptions.OVERWORLD, DimensionOptions.NETHER, DimensionOptions.END
	);
	private static final int VANILLA_KEY_COUNT = VANILLA_KEYS.size();

	public DimensionOptionsRegistryHolder(Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions) {
		if (dimensions.get(DimensionOptions.OVERWORLD) == null) {
			throw new IllegalStateException("Overworld settings missing");
		}

		this.dimensions = dimensions;
	}

	public DimensionOptionsRegistryHolder(Registry<DimensionOptions> dimensionOptionsRegistry) {
		this(dimensionOptionsRegistry
			.streamEntries()
			.collect(Collectors.toMap(RegistryEntry.Reference::registryKey, RegistryEntry.Reference::value)));
	}

	/**
	 * Создаёт поток всех ключей измерений, гарантируя, что ванильные ключи идут первыми,
	 * а дополнительные — без дублирования ванильных.
	 *
	 * @param otherKeys поток дополнительных ключей измерений
	 * @return объединённый поток: сначала ванильные ключи, затем уникальные дополнительные
	 */
	public static Stream<RegistryKey<DimensionOptions>> streamAll(Stream<RegistryKey<DimensionOptions>> otherKeys) {
		return Stream.concat(VANILLA_KEYS.stream(), otherKeys.filter(key -> !VANILLA_KEYS.contains(key)));
	}

	/**
	 * Создаёт новый экземпляр с заменённым генератором чанков для Overworld,
	 * сохраняя тип измерения из текущих настроек (или дефолтный, если Overworld отсутствует).
	 *
	 * @param registries    реестры для получения типа измерения Overworld
	 * @param chunkGenerator новый генератор чанков для Overworld
	 * @return новый холдер с обновлённым Overworld
	 */
	public DimensionOptionsRegistryHolder with(
		RegistryWrapper.WrapperLookup registries,
		ChunkGenerator chunkGenerator
	) {
		RegistryWrapper<DimensionType> dimensionTypeRegistry = registries.getOrThrow(RegistryKeys.DIMENSION_TYPE);
		Map<RegistryKey<DimensionOptions>, DimensionOptions> updated =
			createRegistry(dimensionTypeRegistry, dimensions, chunkGenerator);

		return new DimensionOptionsRegistryHolder(updated);
	}

	/**
	 * Создаёт карту измерений с заменённым генератором чанков для Overworld.
	 * Тип измерения берётся из существующих настроек Overworld или из реестра по умолчанию.
	 *
	 * @param dimensionTypeRegistry реестр типов измерений
	 * @param dimensionOptions      текущая карта параметров измерений
	 * @param chunkGenerator        новый генератор чанков для Overworld
	 * @return обновлённая карта параметров измерений
	 */
	public static Map<RegistryKey<DimensionOptions>, DimensionOptions> createRegistry(
		RegistryWrapper<DimensionType> dimensionTypeRegistry,
		Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensionOptions,
		ChunkGenerator chunkGenerator
	) {
		DimensionOptions overworldOptions = dimensionOptions.get(DimensionOptions.OVERWORLD);
		RegistryEntry<DimensionType> overworldType = overworldOptions == null
			? dimensionTypeRegistry.getOrThrow(DimensionTypes.OVERWORLD)
			: overworldOptions.dimensionTypeEntry();

		return createRegistry(dimensionOptions, overworldType, chunkGenerator);
	}

	/**
	 * Создаёт карту измерений, заменяя Overworld на новые параметры с указанным типом и генератором.
	 *
	 * @param dimensionOptions текущая карта параметров измерений
	 * @param overworld        запись типа измерения для Overworld
	 * @param chunkGenerator   новый генератор чанков для Overworld
	 * @return обновлённая карта с новым Overworld (последнее значение побеждает при дублировании)
	 */
	public static Map<RegistryKey<DimensionOptions>, DimensionOptions> createRegistry(
		Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensionOptions,
		RegistryEntry<DimensionType> overworld,
		ChunkGenerator chunkGenerator
	) {
		return ImmutableMap.<RegistryKey<DimensionOptions>, DimensionOptions>builder()
			.putAll(dimensionOptions)
			.put(DimensionOptions.OVERWORLD, new DimensionOptions(overworld, chunkGenerator))
			.buildKeepingLast();
	}

	/**
	 * Возвращает генератор чанков для измерения Overworld.
	 *
	 * @return генератор чанков Overworld
	 * @throws IllegalStateException если настройки Overworld отсутствуют
	 */
	public ChunkGenerator getChunkGenerator() {
		DimensionOptions overworldOptions = dimensions.get(DimensionOptions.OVERWORLD);
		if (overworldOptions == null) {
			throw new IllegalStateException("Overworld settings missing");
		}

		return overworldOptions.chunkGenerator();
	}

	public Optional<DimensionOptions> getOrEmpty(RegistryKey<DimensionOptions> key) {
		return Optional.ofNullable(dimensions.get(key));
	}

	public ImmutableSet<RegistryKey<World>> getWorldKeys() {
		return dimensions().keySet().stream()
			.map(RegistryKeys::toWorldKey)
			.collect(ImmutableSet.toImmutableSet());
	}

	public boolean isDebug() {
		return getChunkGenerator() instanceof DebugChunkGenerator;
	}

	private static LevelProperties.SpecialProperty getSpecialProperty(Registry<DimensionOptions> dimensionOptionsRegistry) {
		return dimensionOptionsRegistry.getOptionalValue(DimensionOptions.OVERWORLD).map(overworldEntry -> {
			ChunkGenerator chunkGenerator = overworldEntry.chunkGenerator();
			if (chunkGenerator instanceof DebugChunkGenerator) {
				return LevelProperties.SpecialProperty.DEBUG;
			}

			return chunkGenerator instanceof FlatChunkGenerator
				? LevelProperties.SpecialProperty.FLAT
				: LevelProperties.SpecialProperty.NONE;
		}).orElse(LevelProperties.SpecialProperty.NONE);
	}

	static Lifecycle getLifecycle(RegistryKey<DimensionOptions> key, DimensionOptions dimensionOptions) {
		return isVanilla(key, dimensionOptions) ? Lifecycle.stable() : Lifecycle.experimental();
	}

	private static boolean isVanilla(RegistryKey<DimensionOptions> key, DimensionOptions dimensionOptions) {
		if (key == DimensionOptions.OVERWORLD) {
			return isOverworldVanilla(dimensionOptions);
		}

		if (key == DimensionOptions.NETHER) {
			return isNetherVanilla(dimensionOptions);
		}

		return key == DimensionOptions.END && isTheEndVanilla(dimensionOptions);
	}

	private static boolean isOverworldVanilla(DimensionOptions dimensionOptions) {
		RegistryEntry<DimensionType> typeEntry = dimensionOptions.dimensionTypeEntry();
		if (!typeEntry.matchesKey(DimensionTypes.OVERWORLD) && !typeEntry.matchesKey(DimensionTypes.OVERWORLD_CAVES)) {
			return false;
		}

		if (dimensionOptions.chunkGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource multiNoiseBiomeSource) {
			return multiNoiseBiomeSource.matchesInstance(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
		}

		return true;
	}

	private static boolean isNetherVanilla(DimensionOptions dimensionOptions) {
		return dimensionOptions.dimensionTypeEntry().matchesKey(DimensionTypes.THE_NETHER)
			&& dimensionOptions.chunkGenerator() instanceof NoiseChunkGenerator noiseChunkGenerator
			&& noiseChunkGenerator.matchesSettings(ChunkGeneratorSettings.NETHER)
			&& noiseChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource multiNoiseBiomeSource
			&& multiNoiseBiomeSource.matchesInstance(MultiNoiseBiomeSourceParameterLists.NETHER);
	}

	private static boolean isTheEndVanilla(DimensionOptions dimensionOptions) {
		return dimensionOptions.dimensionTypeEntry().matchesKey(DimensionTypes.THE_END)
			&& dimensionOptions.chunkGenerator() instanceof NoiseChunkGenerator noiseChunkGenerator
			&& noiseChunkGenerator.matchesSettings(ChunkGeneratorSettings.END)
			&& noiseChunkGenerator.getBiomeSource() instanceof TheEndBiomeSource;
	}

	/**
	 * Преобразует текущий холдер в финальную конфигурацию измерений, объединяя
	 * существующий реестр с текущими настройками. Ванильные ключи идут первыми,
	 * жизненный цикл реестра определяется наличием нестандартных измерений.
	 *
	 * @param existingRegistry существующий реестр параметров измерений
	 * @return финальная конфигурация с замороженным реестром и типом мира
	 */
	public DimensionsConfig toConfig(Registry<DimensionOptions> existingRegistry) {
		Stream<RegistryKey<DimensionOptions>> allKeys =
			Stream.concat(existingRegistry.getKeys().stream(), dimensions.keySet().stream()).distinct();

		record Entry(RegistryKey<DimensionOptions> key, DimensionOptions value) {

			RegistryEntryInfo toEntryInfo() {
				return new RegistryEntryInfo(
					Optional.empty(),
					DimensionOptionsRegistryHolder.getLifecycle(key, value)
				);
			}
		}

		List<Entry> entries = new ArrayList<>();
		streamAll(allKeys).forEach(key ->
			existingRegistry.getOptionalValue(key)
				.or(() -> Optional.ofNullable(dimensions.get(key)))
				.ifPresent(opts -> entries.add(new Entry(key, opts)))
		);

		Lifecycle lifecycle = entries.size() == VANILLA_KEY_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
		MutableRegistry<DimensionOptions> mutableRegistry = new SimpleRegistry<>(RegistryKeys.DIMENSION, lifecycle);
		entries.forEach(entry -> mutableRegistry.add(entry.key(), entry.value(), entry.toEntryInfo()));

		Registry<DimensionOptions> frozenRegistry = mutableRegistry.freeze();
		LevelProperties.SpecialProperty specialProperty = getSpecialProperty(frozenRegistry);

		return new DimensionsConfig(frozenRegistry.freeze(), specialProperty);
	}

	/**
	 * Финальная конфигурация измерений с замороженным реестром и типом мира.
	 */
	public record DimensionsConfig(
		Registry<DimensionOptions> dimensions,
		LevelProperties.SpecialProperty specialWorldProperty
	) {

		public Lifecycle getLifecycle() {
			return dimensions.getLifecycle();
		}

		public DynamicRegistryManager.Immutable toDynamicRegistryManager() {
			return new DynamicRegistryManager.ImmutableImpl(List.of(dimensions)).toImmutable();
		}
	}
}
