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
 * {@code CowVariants}.
 */
public class CowVariants {

	public static final RegistryKey<CowVariant> TEMPERATE = of(AnimalTemperature.TEMPERATE);
	public static final RegistryKey<CowVariant> WARM = of(AnimalTemperature.WARM);
	public static final RegistryKey<CowVariant> COLD = of(AnimalTemperature.COLD);
	public static final RegistryKey<CowVariant> DEFAULT = TEMPERATE;

	private static RegistryKey<CowVariant> of(Identifier id) {
		return RegistryKey.of(RegistryKeys.COW_VARIANT, id);
	}

	public static void bootstrap(Registerable<CowVariant> registry) {
		register(
				registry,
				TEMPERATE,
				CowVariant.Model.NORMAL,
				"temperate_cow",
				SpawnConditionSelectors.createFallback(0)
		);
		register(registry, WARM, CowVariant.Model.WARM, "warm_cow", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
		register(registry, COLD, CowVariant.Model.COLD, "cold_cow", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
	}

	private static void register(
			Registerable<CowVariant> registry,
			RegistryKey<CowVariant> key,
			CowVariant.Model model,
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
			Registerable<CowVariant> registry,
			RegistryKey<CowVariant> key,
			CowVariant.Model model,
			String textureName,
			SpawnConditionSelectors spawnConditions
	) {
		Identifier identifier = Identifier.ofVanilla("entity/cow/" + textureName);
		registry.register(key, new CowVariant(new ModelAndTexture<>(model, identifier), spawnConditions));
	}
}
