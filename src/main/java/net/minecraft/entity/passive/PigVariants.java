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
 * Реестр климатических вариантов свиньи.
 * Умеренный вариант — запасной (fallback), тёплый и холодный — биомно-зависимые.
 */
public class PigVariants {

	public static final RegistryKey<PigVariant> TEMPERATE = of(AnimalTemperature.TEMPERATE);
	public static final RegistryKey<PigVariant> WARM = of(AnimalTemperature.WARM);
	public static final RegistryKey<PigVariant> COLD = of(AnimalTemperature.COLD);
	public static final RegistryKey<PigVariant> DEFAULT = TEMPERATE;

	/**
	 * Регистрирует все три климатических варианта свиньи в реестре.
	 *
	 * @param registry реестр вариантов свиньи
	 */
	public static void bootstrap(Registerable<PigVariant> registry) {
		register(
				registry,
				TEMPERATE,
				PigVariant.Model.NORMAL,
				"temperate_pig",
				SpawnConditionSelectors.createFallback(0)
		);
		register(registry, WARM, PigVariant.Model.NORMAL, "warm_pig", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
		register(registry, COLD, PigVariant.Model.COLD, "cold_pig", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
	}

	private static void register(
			Registerable<PigVariant> registry,
			RegistryKey<PigVariant> key,
			PigVariant.Model model,
			String textureName,
			TagKey<Biome> biomes
	) {
		RegistryEntryList<Biome> biomeList = registry.getRegistryLookup(RegistryKeys.BIOME).getOrThrow(biomes);
		register(
				registry,
				key,
				model,
				textureName,
				SpawnConditionSelectors.createSingle(new BiomeSpawnCondition(biomeList), 1)
		);
	}

	private static void register(
			Registerable<PigVariant> registry,
			RegistryKey<PigVariant> key,
			PigVariant.Model model,
			String textureName,
			SpawnConditionSelectors spawnConditions
	) {
		Identifier texture = Identifier.ofVanilla("entity/pig/" + textureName);
		registry.register(key, new PigVariant(new ModelAndTexture<>(model, texture), spawnConditions));
	}

	private static RegistryKey<PigVariant> of(Identifier id) {
		return RegistryKey.of(RegistryKeys.PIG_VARIANT, id);
	}
}
