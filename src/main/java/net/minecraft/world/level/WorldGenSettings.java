package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;

/**
 * Неизменяемый контейнер настроек генерации мира: параметры генератора
 * и реестр конфигураций измерений. Используется при сериализации/десериализации
 * секции {@code WorldGenSettings} в level.dat.
 */
public record WorldGenSettings(
	GeneratorOptions generatorOptions,
	DimensionOptionsRegistryHolder dimensionOptionsRegistryHolder
) {

	public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			GeneratorOptions.CODEC.forGetter(WorldGenSettings::generatorOptions),
			DimensionOptionsRegistryHolder.CODEC.forGetter(WorldGenSettings::dimensionOptionsRegistryHolder)
		).apply(instance, instance.stable(WorldGenSettings::new))
	);

	/**
	 * Кодирует настройки генерации в формат {@code T} с явно переданным
	 * {@link DimensionOptionsRegistryHolder}.
	 *
	 * @param registryOps                    операции кодирования с доступом к реестрам
	 * @param generatorOptions               параметры генератора мира
	 * @param dimensionOptionsRegistryHolder реестр конфигураций измерений
	 * @return результат кодирования или ошибка
	 */
	public static <T> DataResult<T> encode(
		DynamicOps<T> registryOps,
		GeneratorOptions generatorOptions,
		DimensionOptionsRegistryHolder dimensionOptionsRegistryHolder
	) {
		return CODEC.encodeStart(registryOps, new WorldGenSettings(generatorOptions, dimensionOptionsRegistryHolder));
	}

	/**
	 * Кодирует настройки генерации, автоматически извлекая реестр измерений
	 * из {@link DynamicRegistryManager}. Удобный вариант для серверного контекста.
	 *
	 * @param registryOps          операции кодирования с доступом к реестрам
	 * @param generatorOptions     параметры генератора мира
	 * @param dynamicRegistryManager менеджер динамических реестров
	 * @return результат кодирования или ошибка
	 */
	public static <T> DataResult<T> encode(
		DynamicOps<T> registryOps,
		GeneratorOptions generatorOptions,
		DynamicRegistryManager dynamicRegistryManager
	) {
		return encode(
			registryOps,
			generatorOptions,
			new DimensionOptionsRegistryHolder(dynamicRegistryManager.getOrThrow(RegistryKeys.DIMENSION))
		);
	}
}
