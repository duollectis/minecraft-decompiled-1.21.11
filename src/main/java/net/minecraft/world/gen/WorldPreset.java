package net.minecraft.world.gen;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;

import java.util.Map;
import java.util.Optional;

/**
 * Пресет мира, содержащий набор измерений с их настройками генерации.
 * Обязательно должен содержать измерение Верхнего мира (Overworld).
 */
public class WorldPreset {

	public static final Codec<WorldPreset> CODEC = RecordCodecBuilder.<WorldPreset>create(
		instance -> instance.group(
			Codec.unboundedMap(RegistryKey.createCodec(RegistryKeys.DIMENSION), DimensionOptions.CODEC)
				.fieldOf("dimensions")
				.forGetter(preset -> preset.dimensions)
		).apply(instance, WorldPreset::new)
	).validate(WorldPreset::validate);

	public static final Codec<RegistryEntry<WorldPreset>> ENTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.WORLD_PRESET, CODEC);

	private final Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions;

	public WorldPreset(Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions) {
		this.dimensions = dimensions;
	}

	/**
	 * Собирает измерения в порядке, определённом {@link DimensionOptionsRegistryHolder#streamAll},
	 * пропуская отсутствующие в пресете измерения.
	 */
	private ImmutableMap<RegistryKey<DimensionOptions>, DimensionOptions> collectDimensions() {
		ImmutableMap.Builder<RegistryKey<DimensionOptions>, DimensionOptions> builder = ImmutableMap.builder();

		DimensionOptionsRegistryHolder.streamAll(dimensions.keySet().stream()).forEach(dimensionKey -> {
			DimensionOptions options = dimensions.get(dimensionKey);

			if (options != null) {
				builder.put(dimensionKey, options);
			}
		});

		return builder.build();
	}

	public DimensionOptionsRegistryHolder createDimensionsRegistryHolder() {
		return new DimensionOptionsRegistryHolder(collectDimensions());
	}

	public Optional<DimensionOptions> getOverworld() {
		return Optional.ofNullable(dimensions.get(DimensionOptions.OVERWORLD));
	}

	private static DataResult<WorldPreset> validate(WorldPreset preset) {
		return preset.getOverworld().isEmpty()
			? DataResult.error(() -> "Missing overworld dimension")
			: DataResult.success(preset, Lifecycle.stable());
	}
}
