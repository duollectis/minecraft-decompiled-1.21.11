package net.minecraft.entity.passive;

import net.minecraft.entity.spawn.BiomeSpawnCondition;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.ModelAndTexture;
import net.minecraft.world.biome.Biome;

/**
 * {@code ChickenVariants}.
 */
public class ChickenVariants {

	public static final RegistryKey<ChickenVariant> TEMPERATE = of(AnimalTemperature.TEMPERATE);
	public static final RegistryKey<ChickenVariant> WARM = of(AnimalTemperature.WARM);
	public static final RegistryKey<ChickenVariant> COLD = of(AnimalTemperature.COLD);
	public static final RegistryKey<ChickenVariant> DEFAULT = TEMPERATE;

	private static RegistryKey<ChickenVariant> of(Identifier id) {
		return RegistryKey.of(RegistryKeys.CHICKEN_VARIANT, id);
	}

	/**
	 * Bootstrap.
	 *
	 * @param registry registry
	 */
	public static void bootstrap(Registerable<ChickenVariant> registry) {
		register(
				registry,
				TEMPERATE,
				ChickenVariant.Model.NORMAL,
				"temperate_chicken",
				SpawnConditionSelectors.createFallback(0)
		);
		register(
				registry,
				WARM,
				ChickenVariant.Model.NORMAL,
				"warm_chicken",
				BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS
		);
		register(registry, COLD, ChickenVariant.Model.COLD, "cold_chicken", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
	}

	private static void register(
			Registerable<ChickenVariant> registry,
			RegistryKey<ChickenVariant> key,
			ChickenVariant.Model model,
			String textureName,
			TagKey<Biome> biomes
	) {
		RegistryEntryList<Biome> registryEntryList = registry.getRegistryLookup(RegistryKeys.BIOME).getOrThrow(biomes);
		register(
				registry,
				key,
				model,
				textureName,
				SpawnConditionSelectors.createSingle(new BiomeSpawnCondition(registryEntryList), 1)
		);
	}

	private static void register(
			Registerable<ChickenVariant> registry,
			RegistryKey<ChickenVariant> key,
			ChickenVariant.Model model,
			String textureName,
			SpawnConditionSelectors spawnConditions
	) {
		Identifier identifier = Identifier.ofVanilla("entity/chicken/" + textureName);
		registry.register(key, new ChickenVariant(new ModelAndTexture<>(model, identifier), spawnConditions));
	}
}
