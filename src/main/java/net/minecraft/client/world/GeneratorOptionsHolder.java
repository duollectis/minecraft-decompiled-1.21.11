package net.minecraft.client.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.world.InitialWorldOptions;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.registry.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.DataPackContents;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.WorldGenSettings;
import net.minecraft.world.rule.ServerGameRules;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Иммутабельный контейнер всех параметров генерации мира на клиенте.
 *
 * <p>Хранит {@link GeneratorOptions}, реестр измерений, объединённые динамические реестры,
 * содержимое датапаков и начальные параметры создания мира. Все методы {@code apply} и {@code with}
 * возвращают новый экземпляр, не изменяя текущий.
 */
@Environment(EnvType.CLIENT)
public record GeneratorOptionsHolder(
		GeneratorOptions generatorOptions,
		Registry<DimensionOptions> dimensionOptionsRegistry,
		DimensionOptionsRegistryHolder selectedDimensions,
		CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
		DataPackContents dataPackContents,
		DataConfiguration dataConfiguration,
		InitialWorldOptions initialWorldCreationOptions
) {

	public GeneratorOptionsHolder(
			WorldGenSettings worldGenSettings,
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
			DataPackContents dataPackContents,
			DataConfiguration dataConfiguration
	) {
		this(
				worldGenSettings.generatorOptions(),
				worldGenSettings.dimensionOptionsRegistryHolder(),
				combinedDynamicRegistries,
				dataPackContents,
				dataConfiguration,
				new InitialWorldOptions(WorldCreator.Mode.SURVIVAL, ServerGameRules.of(), null)
		);
	}

	public GeneratorOptionsHolder(
			GeneratorOptions generatorOptions,
			DimensionOptionsRegistryHolder selectedDimensions,
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
			DataPackContents dataPackContents,
			DataConfiguration dataConfiguration,
			InitialWorldOptions initialWorldOptions
	) {
		this(
				generatorOptions,
				combinedDynamicRegistries.get(ServerDynamicRegistryType.DIMENSIONS).getOrThrow(RegistryKeys.DIMENSION),
				selectedDimensions,
				combinedDynamicRegistries.with(ServerDynamicRegistryType.DIMENSIONS),
				dataPackContents,
				dataConfiguration,
				initialWorldOptions
		);
	}

	public GeneratorOptionsHolder with(
			GeneratorOptions generatorOptions,
			DimensionOptionsRegistryHolder selectedDimensions
	) {
		return new GeneratorOptionsHolder(
				generatorOptions,
				dimensionOptionsRegistry,
				selectedDimensions,
				combinedDynamicRegistries,
				dataPackContents,
				dataConfiguration,
				initialWorldCreationOptions
		);
	}

	/**
	 * Применяет модификатор к {@link GeneratorOptions}, возвращая новый экземпляр держателя.
	 *
	 * @param modifier функция-преобразователь параметров генератора
	 * @return новый {@code GeneratorOptionsHolder} с изменёнными параметрами генератора
	 */
	public GeneratorOptionsHolder apply(GeneratorOptionsHolder.Modifier modifier) {
		return new GeneratorOptionsHolder(
				modifier.apply(generatorOptions),
				dimensionOptionsRegistry,
				selectedDimensions,
				combinedDynamicRegistries,
				dataPackContents,
				dataConfiguration,
				initialWorldCreationOptions
		);
	}

	/**
	 * Применяет реестро-зависимый модификатор к {@link DimensionOptionsRegistryHolder},
	 * возвращая новый экземпляр держателя с обновлёнными измерениями.
	 *
	 * @param modifier функция, принимающая объединённый реестр и текущие измерения
	 * @return новый {@code GeneratorOptionsHolder} с изменёнными измерениями
	 */
	public GeneratorOptionsHolder apply(GeneratorOptionsHolder.RegistryAwareModifier modifier) {
		return new GeneratorOptionsHolder(
				generatorOptions,
				dimensionOptionsRegistry,
				modifier.apply(getCombinedRegistryManager(), selectedDimensions),
				combinedDynamicRegistries,
				dataPackContents,
				dataConfiguration,
				initialWorldCreationOptions
		);
	}

	public DynamicRegistryManager.Immutable getCombinedRegistryManager() {
		return combinedDynamicRegistries.getCombinedRegistryManager();
	}

	/**
	 * Инициализирует индексированные списки фич для всех генераторов чанков в реестре измерений.
	 * Должен вызываться после финальной сборки реестра перед стартом генерации.
	 */
	public void initializeIndexedFeaturesLists() {
		for (DimensionOptions dimensionOptions : dimensionOptionsRegistry()) {
			dimensionOptions.chunkGenerator().initializeIndexedFeaturesList();
		}

	}

	/**
	 * Функциональный интерфейс для модификации {@link GeneratorOptions}.
	 */
	@Environment(EnvType.CLIENT)
	public interface Modifier extends UnaryOperator<GeneratorOptions> {
	}

	/**
	 * Функциональный интерфейс для модификации {@link DimensionOptionsRegistryHolder}
	 * с доступом к объединённому реестру.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface RegistryAwareModifier
			extends BiFunction<DynamicRegistryManager.Immutable, DimensionOptionsRegistryHolder, DimensionOptionsRegistryHolder> {
	}

}
