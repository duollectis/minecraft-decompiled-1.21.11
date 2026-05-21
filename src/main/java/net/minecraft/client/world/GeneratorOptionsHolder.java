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

@Environment(EnvType.CLIENT)
/**
 * {@code GeneratorOptionsHolder}.
 */
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
				this.dimensionOptionsRegistry,
				selectedDimensions,
				this.combinedDynamicRegistries,
				this.dataPackContents,
				this.dataConfiguration,
				this.initialWorldCreationOptions
		);
	}

	/**
	 * Apply.
	 *
	 * @param modifier modifier
	 *
	 * @return GeneratorOptionsHolder — результат операции
	 */
	public GeneratorOptionsHolder apply(GeneratorOptionsHolder.Modifier modifier) {
		return new GeneratorOptionsHolder(
				modifier.apply(this.generatorOptions),
				this.dimensionOptionsRegistry,
				this.selectedDimensions,
				this.combinedDynamicRegistries,
				this.dataPackContents,
				this.dataConfiguration,
				this.initialWorldCreationOptions
		);
	}

	/**
	 * Apply.
	 *
	 * @param modifier modifier
	 *
	 * @return GeneratorOptionsHolder — результат операции
	 */
	public GeneratorOptionsHolder apply(GeneratorOptionsHolder.RegistryAwareModifier modifier) {
		return new GeneratorOptionsHolder(
				this.generatorOptions,
				this.dimensionOptionsRegistry,
				modifier.apply(this.getCombinedRegistryManager(), this.selectedDimensions),
				this.combinedDynamicRegistries,
				this.dataPackContents,
				this.dataConfiguration,
				this.initialWorldCreationOptions
		);
	}

	public DynamicRegistryManager.Immutable getCombinedRegistryManager() {
		return this.combinedDynamicRegistries.getCombinedRegistryManager();
	}

	/**
	 * Инициализирует ialize indexed features lists.
	 */
	public void initializeIndexedFeaturesLists() {
		for (DimensionOptions dimensionOptions : this.dimensionOptionsRegistry()) {
			dimensionOptions.chunkGenerator().initializeIndexedFeaturesList();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Modifier}.
	 */
	public interface Modifier extends UnaryOperator<GeneratorOptions> {
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code RegistryAwareModifier}.
	 */
	public interface RegistryAwareModifier extends BiFunction<DynamicRegistryManager.Immutable, DimensionOptionsRegistryHolder, DimensionOptionsRegistryHolder> {
	}
}
