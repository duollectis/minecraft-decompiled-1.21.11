package net.minecraft.world.biome.source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Список параметров источника биомов с шумом, хранящийся в реестре.
 * Связывает {@link Preset} с конкретными записями биомов через {@link RegistryEntryLookup}.
 */
public class MultiNoiseBiomeSourceParameterList {

	public static final Codec<MultiNoiseBiomeSourceParameterList> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			MultiNoiseBiomeSourceParameterList.Preset.CODEC
				.fieldOf("preset")
				.forGetter(list -> list.preset),
			RegistryOps.getEntryLookupCodec(RegistryKeys.BIOME)
		)
		.apply(instance, MultiNoiseBiomeSourceParameterList::new)
	);

	public static final Codec<RegistryEntry<MultiNoiseBiomeSourceParameterList>> REGISTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, CODEC);

	private final MultiNoiseBiomeSourceParameterList.Preset preset;
	private final MultiNoiseUtil.Entries<RegistryEntry<Biome>> entries;

	public MultiNoiseBiomeSourceParameterList(
		MultiNoiseBiomeSourceParameterList.Preset preset,
		RegistryEntryLookup<Biome> biomeLookup
	) {
		this.preset = preset;
		this.entries = preset.biomeSourceFunction.apply(biomeLookup::getOrThrow);
	}

	public MultiNoiseUtil.Entries<RegistryEntry<Biome>> getEntries() {
		return entries;
	}

	/**
	 * Возвращает карту всех пресетов с их записями биомов в виде ключей реестра.
	 * Используется для генерации данных и отладки.
	 */
	public static Map<MultiNoiseBiomeSourceParameterList.Preset, MultiNoiseUtil.Entries<RegistryKey<Biome>>> getPresetToEntriesMap() {
		return MultiNoiseBiomeSourceParameterList.Preset.BY_IDENTIFIER
			.values()
			.stream()
			.collect(Collectors.toMap(
				preset -> preset,
				preset -> preset.biomeSourceFunction().apply(biomeKey -> biomeKey)
			));
	}

	/**
	 * Пресет источника биомов — именованная конфигурация, определяющая набор биомов
	 * и их шумовые параметры для конкретного измерения (Нижний мир, Верхний мир).
	 */
	public record Preset(
		Identifier id,
		MultiNoiseBiomeSourceParameterList.Preset.BiomeSourceFunction biomeSourceFunction
	) {

		public static final MultiNoiseBiomeSourceParameterList.Preset NETHER =
			new MultiNoiseBiomeSourceParameterList.Preset(
				Identifier.ofVanilla("nether"),
				new MultiNoiseBiomeSourceParameterList.Preset.BiomeSourceFunction() {
					@Override
					public <T> MultiNoiseUtil.Entries<T> apply(Function<RegistryKey<Biome>, T> function) {
						return new MultiNoiseUtil.Entries<>(
							List.of(
								Pair.of(
									MultiNoiseUtil.createNoiseHypercube(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
									function.apply(BiomeKeys.NETHER_WASTES)
								),
								Pair.of(
									MultiNoiseUtil.createNoiseHypercube(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
									function.apply(BiomeKeys.SOUL_SAND_VALLEY)
								),
								Pair.of(
									MultiNoiseUtil.createNoiseHypercube(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
									function.apply(BiomeKeys.CRIMSON_FOREST)
								),
								Pair.of(
									MultiNoiseUtil.createNoiseHypercube(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.375F),
									function.apply(BiomeKeys.WARPED_FOREST)
								),
								Pair.of(
									MultiNoiseUtil.createNoiseHypercube(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.175F),
									function.apply(BiomeKeys.BASALT_DELTAS)
								)
							)
						);
					}
				}
			);

		public static final MultiNoiseBiomeSourceParameterList.Preset OVERWORLD =
			new MultiNoiseBiomeSourceParameterList.Preset(
				Identifier.ofVanilla("overworld"),
				new MultiNoiseBiomeSourceParameterList.Preset.BiomeSourceFunction() {
					@Override
					public <T> MultiNoiseUtil.Entries<T> apply(Function<RegistryKey<Biome>, T> function) {
						return MultiNoiseBiomeSourceParameterList.Preset.getOverworldEntries(function);
					}
				}
			);

		static final Map<Identifier, MultiNoiseBiomeSourceParameterList.Preset> BY_IDENTIFIER =
			Stream.of(NETHER, OVERWORLD)
				.collect(Collectors.toMap(
					MultiNoiseBiomeSourceParameterList.Preset::id,
					preset -> preset
				));

		public static final Codec<MultiNoiseBiomeSourceParameterList.Preset> CODEC = Identifier.CODEC
			.flatXmap(
				id -> Optional.ofNullable(BY_IDENTIFIER.get(id))
					.<DataResult<MultiNoiseBiomeSourceParameterList.Preset>>map(DataResult::success)
					.orElseGet(() -> DataResult.error(() -> "Unknown preset: " + id)),
				preset -> DataResult.success(preset.id)
			);

		static <T> MultiNoiseUtil.Entries<T> getOverworldEntries(Function<RegistryKey<Biome>, T> biomeEntryGetter) {
			Builder<Pair<MultiNoiseUtil.NoiseHypercube, T>> builder = ImmutableList.builder();
			new VanillaBiomeParameters().writeOverworldBiomeParameters(
				pair -> builder.add(pair.mapSecond(biomeEntryGetter))
			);
			return new MultiNoiseUtil.Entries<>(builder.build());
		}

		public Stream<RegistryKey<Biome>> biomeStream() {
			return biomeSourceFunction
				.apply(biomeKey -> biomeKey)
				.getEntries()
				.stream()
				.<RegistryKey<Biome>>map(Pair::getSecond)
				.distinct();
		}

		/**
		 * Функциональный интерфейс для создания записей биомов с произвольным типом значения.
		 * Позволяет использовать один и тот же пресет как для {@link RegistryEntry}, так и для {@link RegistryKey}.
		 */
		@FunctionalInterface
		interface BiomeSourceFunction {

			<T> MultiNoiseUtil.Entries<T> apply(Function<RegistryKey<Biome>, T> biomeEntryGetter);
		}
	}
}
