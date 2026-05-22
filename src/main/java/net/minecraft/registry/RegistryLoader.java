package net.minecraft.registry;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.block.spawner.TrialSpawnerConfig;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.provider.EnchantmentProvider;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.mob.ZombieNautilusVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Instrument;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.message.MessageType;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.test.TestEnvironmentDefinition;
import net.minecraft.test.TestInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.attribute.timeline.Timeline;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Загружает динамические реестры из ресурсов датапаков или из сетевых пакетов.
 *
 * <p>Содержит два статических списка реестров:
 * <ul>
 *   <li>{@link #DYNAMIC_REGISTRIES} — все реестры, загружаемые с диска (биомы, структуры и т.д.);</li>
 *   <li>{@link #SYNCED_REGISTRIES} — подмножество реестров, синхронизируемых с клиентом по сети.</li>
 * </ul>
 *
 * <p>Точки входа: {@link #loadFromResource} и {@link #loadFromNetwork}.</p>
 */
public class RegistryLoader {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Comparator<RegistryKey<?>> KEY_COMPARATOR = Comparator
			.<RegistryKey<?>, Identifier>comparing(RegistryKey::getRegistry)
			.thenComparing(RegistryKey::getValue);
	private static final RegistryEntryInfo EXPERIMENTAL_ENTRY_INFO =
			new RegistryEntryInfo(Optional.empty(), Lifecycle.experimental());
	private static final Function<Optional<VersionedIdentifier>, RegistryEntryInfo> RESOURCE_ENTRY_INFO_GETTER =
			Util.memoize(knownPacks -> {
				Lifecycle lifecycle = knownPacks
						.map(VersionedIdentifier::isVanilla)
						.map(vanilla -> Lifecycle.stable())
						.orElse(Lifecycle.experimental());
				return new RegistryEntryInfo(knownPacks, lifecycle);
			});

	public static final List<Entry<?>> DYNAMIC_REGISTRIES = List.of(
			new Entry<>(RegistryKeys.DIMENSION_TYPE, DimensionType.CODEC),
			new Entry<>(RegistryKeys.BIOME, Biome.CODEC),
			new Entry<>(RegistryKeys.MESSAGE_TYPE, MessageType.CODEC),
			new Entry<>(RegistryKeys.CONFIGURED_CARVER, ConfiguredCarver.CODEC),
			new Entry<>(RegistryKeys.CONFIGURED_FEATURE, ConfiguredFeature.CODEC),
			new Entry<>(RegistryKeys.PLACED_FEATURE, PlacedFeature.CODEC),
			new Entry<>(RegistryKeys.STRUCTURE, Structure.STRUCTURE_CODEC),
			new Entry<>(RegistryKeys.STRUCTURE_SET, StructureSet.CODEC),
			new Entry<>(RegistryKeys.PROCESSOR_LIST, StructureProcessorType.PROCESSORS_CODEC),
			new Entry<>(RegistryKeys.TEMPLATE_POOL, StructurePool.CODEC),
			new Entry<>(RegistryKeys.CHUNK_GENERATOR_SETTINGS, ChunkGeneratorSettings.CODEC),
			new Entry<>(RegistryKeys.NOISE_PARAMETERS, DoublePerlinNoiseSampler.NoiseParameters.CODEC),
			new Entry<>(RegistryKeys.DENSITY_FUNCTION, DensityFunction.CODEC),
			new Entry<>(RegistryKeys.WORLD_PRESET, WorldPreset.CODEC),
			new Entry<>(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, FlatLevelGeneratorPreset.CODEC),
			new Entry<>(RegistryKeys.TRIM_PATTERN, ArmorTrimPattern.CODEC),
			new Entry<>(RegistryKeys.TRIM_MATERIAL, ArmorTrimMaterial.CODEC),
			new Entry<>(RegistryKeys.TRIAL_SPAWNER, TrialSpawnerConfig.CODEC),
			new Entry<>(RegistryKeys.WOLF_VARIANT, WolfVariant.CODEC, true),
			new Entry<>(RegistryKeys.WOLF_SOUND_VARIANT, WolfSoundVariant.CODEC, true),
			new Entry<>(RegistryKeys.PIG_VARIANT, PigVariant.CODEC, true),
			new Entry<>(RegistryKeys.FROG_VARIANT, FrogVariant.CODEC, true),
			new Entry<>(RegistryKeys.CAT_VARIANT, CatVariant.CODEC, true),
			new Entry<>(RegistryKeys.COW_VARIANT, CowVariant.CODEC, true),
			new Entry<>(RegistryKeys.CHICKEN_VARIANT, ChickenVariant.CODEC, true),
			new Entry<>(RegistryKeys.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.CODEC, true),
			new Entry<>(RegistryKeys.PAINTING_VARIANT, PaintingVariant.CODEC, true),
			new Entry<>(RegistryKeys.DAMAGE_TYPE, DamageType.CODEC),
			new Entry<>(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, MultiNoiseBiomeSourceParameterList.CODEC),
			new Entry<>(RegistryKeys.BANNER_PATTERN, BannerPattern.CODEC),
			new Entry<>(RegistryKeys.ENCHANTMENT, Enchantment.CODEC),
			new Entry<>(RegistryKeys.ENCHANTMENT_PROVIDER, EnchantmentProvider.CODEC),
			new Entry<>(RegistryKeys.JUKEBOX_SONG, JukeboxSong.CODEC),
			new Entry<>(RegistryKeys.INSTRUMENT, Instrument.CODEC),
			new Entry<>(RegistryKeys.TEST_ENVIRONMENT, TestEnvironmentDefinition.CODEC),
			new Entry<>(RegistryKeys.TEST_INSTANCE, TestInstance.CODEC),
			new Entry<>(RegistryKeys.DIALOG, Dialog.CODEC),
			new Entry<>(RegistryKeys.TIMELINE, Timeline.CODEC)
	);

	public static final List<Entry<?>> DIMENSION_REGISTRIES =
			List.of(new Entry<>(RegistryKeys.DIMENSION, DimensionOptions.CODEC));

	public static final List<Entry<?>> SYNCED_REGISTRIES = List.of(
			new Entry<>(RegistryKeys.BIOME, Biome.NETWORK_CODEC),
			new Entry<>(RegistryKeys.MESSAGE_TYPE, MessageType.CODEC),
			new Entry<>(RegistryKeys.TRIM_PATTERN, ArmorTrimPattern.CODEC),
			new Entry<>(RegistryKeys.TRIM_MATERIAL, ArmorTrimMaterial.CODEC),
			new Entry<>(RegistryKeys.WOLF_VARIANT, WolfVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.WOLF_SOUND_VARIANT, WolfSoundVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.PIG_VARIANT, PigVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.FROG_VARIANT, FrogVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.CAT_VARIANT, CatVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.COW_VARIANT, CowVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.CHICKEN_VARIANT, ChickenVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.NETWORK_CODEC, true),
			new Entry<>(RegistryKeys.PAINTING_VARIANT, PaintingVariant.CODEC, true),
			new Entry<>(RegistryKeys.DIMENSION_TYPE, DimensionType.NETWORK_CODEC),
			new Entry<>(RegistryKeys.DAMAGE_TYPE, DamageType.CODEC),
			new Entry<>(RegistryKeys.BANNER_PATTERN, BannerPattern.CODEC),
			new Entry<>(RegistryKeys.ENCHANTMENT, Enchantment.CODEC),
			new Entry<>(RegistryKeys.JUKEBOX_SONG, JukeboxSong.CODEC),
			new Entry<>(RegistryKeys.INSTRUMENT, Instrument.CODEC),
			new Entry<>(RegistryKeys.TEST_ENVIRONMENT, TestEnvironmentDefinition.CODEC),
			new Entry<>(RegistryKeys.TEST_INSTANCE, TestInstance.CODEC),
			new Entry<>(RegistryKeys.DIALOG, Dialog.CODEC),
			new Entry<>(RegistryKeys.TIMELINE, Timeline.NETWORK_CODEC)
	);

	public static DynamicRegistryManager.Immutable loadFromResource(
			ResourceManager resourceManager,
			List<RegistryWrapper.Impl<?>> registries,
			List<Entry<?>> entries
	) {
		return load(
				(loader, infoGetter) -> loader.loadFromResource(resourceManager, infoGetter),
				registries,
				entries
		);
	}

	public static DynamicRegistryManager.Immutable loadFromNetwork(
			Map<RegistryKey<? extends Registry<?>>, ElementsAndTags> data,
			ResourceFactory factory,
			List<RegistryWrapper.Impl<?>> registries,
			List<Entry<?>> entries
	) {
		return load(
				(loader, infoGetter) -> loader.loadFromNetwork(data, factory, infoGetter),
				registries,
				entries
		);
	}

	/**
	 * Общая точка загрузки реестров: создаёт загрузчики, применяет {@code loadable},
	 * замораживает реестры и проверяет обязательные непустые реестры.
	 */
	private static DynamicRegistryManager.Immutable load(
			RegistryLoadable loadable,
			List<RegistryWrapper.Impl<?>> registries,
			List<Entry<?>> entries
	) {
		Map<RegistryKey<?>, Exception> errors = new HashMap<>();
		List<Loader<?>> loaders = entries.stream()
				.map(entry -> entry.getLoader(Lifecycle.stable(), errors))
				.collect(Collectors.toUnmodifiableList());
		RegistryOps.RegistryInfoGetter infoGetter = createInfoGetter(registries, loaders);
		loaders.forEach(loader -> loadable.apply((Loader<?>) loader, infoGetter));

		loaders.forEach(loader -> {
			Registry<?> registry = loader.registry();

			try {
				registry.freeze();
			} catch (Exception exception) {
				errors.put(registry.getKey(), exception);
			}

			if (loader.data.requiredNonEmpty && registry.size() == 0) {
				errors.put(
						registry.getKey(),
						new IllegalStateException("Registry must be non-empty: " + registry.getKey().getValue())
				);
			}
		});

		if (errors.isEmpty()) {
			return new DynamicRegistryManager.ImmutableImpl(
					loaders.stream().map(Loader::registry).toList()
			).toImmutable();
		}

		throw writeAndCreateLoadingException(errors);
	}

	private static RegistryOps.RegistryInfoGetter createInfoGetter(
			List<RegistryWrapper.Impl<?>> registries,
			List<Loader<?>> additionalRegistries
	) {
		final Map<RegistryKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> infoMap = new HashMap<>();
		registries.forEach(registry -> infoMap.put(registry.getKey(), createInfo((RegistryWrapper.Impl<?>) registry)));
		additionalRegistries.forEach(loader -> infoMap.put(loader.registry.getKey(), createInfo(loader.registry)));
		return new RegistryOps.RegistryInfoGetter() {
			@Override
			public <T> Optional<RegistryOps.RegistryInfo<T>> getRegistryInfo(
					RegistryKey<? extends Registry<? extends T>> registryRef
			) {
				return Optional.ofNullable((RegistryOps.RegistryInfo<T>) infoMap.get(registryRef));
			}
		};
	}

	private static <T> RegistryOps.RegistryInfo<T> createInfo(MutableRegistry<T> registry) {
		return new RegistryOps.RegistryInfo<>(
				registry,
				registry.createMutableRegistryLookup(),
				registry.getLifecycle()
		);
	}

	private static <T> RegistryOps.RegistryInfo<T> createInfo(RegistryWrapper.Impl<T> registry) {
		return new RegistryOps.RegistryInfo<>(registry, registry, registry.getLifecycle());
	}

	private static CrashException writeAndCreateLoadingException(Map<RegistryKey<?>, Exception> exceptions) {
		writeLoadingError(exceptions);
		return createLoadingException(exceptions);
	}

	private static void writeLoadingError(Map<RegistryKey<?>, Exception> exceptions) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		Map<Identifier, Map<Identifier, Exception>> grouped = exceptions.entrySet()
				.stream()
				.collect(Collectors.groupingBy(
						entry -> entry.getKey().getRegistry(),
						Collectors.toMap(
								entry -> entry.getKey().getValue(),
								Map.Entry::getValue
						)
				));

		grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			printWriter.printf(Locale.ROOT, "> Errors in registry %s:%n", entry.getKey());
			entry.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(element -> {
				printWriter.printf(Locale.ROOT, ">> Errors in element %s:%n", element.getKey());
				element.getValue().printStackTrace(printWriter);
			});
		});

		printWriter.flush();
		LOGGER.error("Registry loading errors:\n{}", stringWriter);
	}

	private static CrashException createLoadingException(Map<RegistryKey<?>, Exception> exceptions) {
		CrashReport crashReport = CrashReport.create(
				new IllegalStateException("Failed to load registries due to errors"),
				"Registry Loading"
		);
		CrashReportSection section = crashReport.addElement("Loading info");
		section.add(
				"Errors",
				() -> {
					StringBuilder builder = new StringBuilder();
					exceptions.entrySet()
							.stream()
							.sorted(Map.Entry.comparingByKey(KEY_COMPARATOR))
							.forEach(entry -> builder
									.append("\n\t\t")
									.append(entry.getKey().getRegistry())
									.append("/")
									.append(entry.getKey().getValue())
									.append(": ")
									.append(entry.getValue().getMessage())
							);
					return builder.toString();
				}
		);
		return new CrashException(crashReport);
	}

	private static <E> void parseAndAdd(
			MutableRegistry<E> registry,
			Decoder<E> decoder,
			RegistryOps<JsonElement> ops,
			RegistryKey<E> key,
			Resource resource,
			RegistryEntryInfo entryInfo
	) throws IOException {
		try (Reader reader = resource.getReader()) {
			JsonElement jsonElement = StrictJsonParser.parse(reader);
			DataResult<E> dataResult = decoder.parse(ops, jsonElement);
			E value = (E) dataResult.getOrThrow();
			registry.add(key, value, entryInfo);
		}
	}

	static <E> void loadFromResource(
			ResourceManager resourceManager,
			RegistryOps.RegistryInfoGetter infoGetter,
			MutableRegistry<E> registry,
			Decoder<E> elementDecoder,
			Map<RegistryKey<?>, Exception> errors
	) {
		ResourceFinder resourceFinder = ResourceFinder.json(registry.getKey());
		RegistryOps<JsonElement> registryOps = RegistryOps.of(JsonOps.INSTANCE, infoGetter);

		for (Map.Entry<Identifier, Resource> entry : resourceFinder.findResources(resourceManager).entrySet()) {
			Identifier resourcePath = entry.getKey();
			RegistryKey<E> registryKey = RegistryKey.of(
					registry.getKey(),
					resourceFinder.toResourceId(resourcePath)
			);
			Resource resource = entry.getValue();
			RegistryEntryInfo entryInfo = RESOURCE_ENTRY_INFO_GETTER.apply(resource.getKnownPackInfo());

			try {
				parseAndAdd(registry, elementDecoder, registryOps, registryKey, resource, entryInfo);
			} catch (Exception exception) {
				errors.put(
						registryKey,
						new IllegalStateException(
								String.format(
										Locale.ROOT,
										"Failed to parse %s from pack %s",
										resourcePath,
										resource.getPackId()
								),
								exception
						)
				);
			}
		}

		TagGroupLoader.loadInitial(resourceManager, registry);
	}

	static <E> void loadFromNetwork(
			Map<RegistryKey<? extends Registry<?>>, ElementsAndTags> data,
			ResourceFactory factory,
			RegistryOps.RegistryInfoGetter infoGetter,
			MutableRegistry<E> registry,
			Decoder<E> decoder,
			Map<RegistryKey<?>, Exception> loadingErrors
	) {
		ElementsAndTags elementsAndTags = data.get(registry.getKey());
		if (elementsAndTags == null) {
			return;
		}

		RegistryOps<NbtElement> nbtOps = RegistryOps.of(NbtOps.INSTANCE, infoGetter);
		RegistryOps<JsonElement> jsonOps = RegistryOps.of(JsonOps.INSTANCE, infoGetter);
		ResourceFinder resourceFinder = ResourceFinder.json(registry.getKey());

		for (SerializableRegistries.SerializedRegistryEntry serializedEntry : elementsAndTags.elements) {
			RegistryKey<E> registryKey = RegistryKey.of(registry.getKey(), serializedEntry.id());
			Optional<NbtElement> nbtData = serializedEntry.data();

			if (nbtData.isPresent()) {
				try {
					DataResult<E> dataResult = decoder.parse(nbtOps, nbtData.get());
					E value = (E) dataResult.getOrThrow();
					registry.add(registryKey, value, EXPERIMENTAL_ENTRY_INFO);
				} catch (Exception exception) {
					loadingErrors.put(
							registryKey,
							new IllegalStateException(
									String.format(Locale.ROOT, "Failed to parse value %s from server", nbtData.get()),
									exception
							)
					);
				}
			} else {
				Identifier resourcePath = resourceFinder.toResourcePath(serializedEntry.id());

				try {
					Resource resource = factory.getResourceOrThrow(resourcePath);
					parseAndAdd(registry, decoder, jsonOps, registryKey, resource, EXPERIMENTAL_ENTRY_INFO);
				} catch (Exception exception) {
					loadingErrors.put(
							registryKey,
							new IllegalStateException("Failed to parse local data", exception)
					);
				}
			}
		}

		TagGroupLoader.loadFromNetwork(elementsAndTags.tags, registry);
	}

	/**
	 * Пара из списка элементов реестра и сериализованных тегов, полученных по сети.
	 *
	 * @param elements список сериализованных записей реестра
	 * @param tags     сериализованные теги для этого реестра
	 */
	public record ElementsAndTags(
			List<SerializableRegistries.SerializedRegistryEntry> elements,
			TagPacketSerializer.Serialized tags
	) {
	}

	/**
	 * Описание одного динамического реестра: ключ, кодек и флаг обязательной непустоты.
	 *
	 * @param key              ключ реестра
	 * @param elementCodec     кодек для сериализации/десериализации элементов
	 * @param requiredNonEmpty если {@code true}, реестр должен содержать хотя бы один элемент
	 */
	public record Entry<T>(
			RegistryKey<? extends Registry<T>> key,
			Codec<T> elementCodec,
			boolean requiredNonEmpty
	) {

		Entry(RegistryKey<? extends Registry<T>> key, Codec<T> codec) {
			this(key, codec, false);
		}

		Loader<T> getLoader(Lifecycle lifecycle, Map<RegistryKey<?>, Exception> errors) {
			MutableRegistry<T> mutableRegistry = new SimpleRegistry<>(key, lifecycle);
			return new Loader<>(this, mutableRegistry, errors);
		}

		public void addToCloner(BiConsumer<RegistryKey<? extends Registry<T>>, Codec<T>> callback) {
			callback.accept(key, elementCodec);
		}
	}

	/**
	 * Загрузчик одного реестра: связывает {@link Entry} с конкретным {@link MutableRegistry}.
	 */
	record Loader<T>(
			Entry<T> data,
			MutableRegistry<T> registry,
			Map<RegistryKey<?>, Exception> loadingErrors
	) {

		public void loadFromResource(ResourceManager resourceManager, RegistryOps.RegistryInfoGetter infoGetter) {
			RegistryLoader.loadFromResource(
					resourceManager,
					infoGetter,
					registry,
					data.elementCodec,
					loadingErrors
			);
		}

		public void loadFromNetwork(
				Map<RegistryKey<? extends Registry<?>>, ElementsAndTags> networkData,
				ResourceFactory factory,
				RegistryOps.RegistryInfoGetter infoGetter
		) {
			RegistryLoader.loadFromNetwork(
					networkData,
					factory,
					infoGetter,
					registry,
					data.elementCodec,
					loadingErrors
			);
		}
	}

	@FunctionalInterface
	interface RegistryLoadable {

		void apply(Loader<?> loader, RegistryOps.RegistryInfoGetter infoGetter);
	}
}
