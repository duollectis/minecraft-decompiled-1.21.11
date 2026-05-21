package net.minecraft.entity.mob;

import net.minecraft.entity.passive.AnimalTemperature;
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
 * {@code ZombieNautilusVariants}.
 */
public class ZombieNautilusVariants {

	public static final RegistryKey<ZombieNautilusVariant> TEMPERATE = of(AnimalTemperature.TEMPERATE);
	public static final RegistryKey<ZombieNautilusVariant> WARM = of(AnimalTemperature.WARM);
	public static final RegistryKey<ZombieNautilusVariant> DEFAULT = TEMPERATE;

	private static RegistryKey<ZombieNautilusVariant> of(Identifier id) {
		return RegistryKey.of(RegistryKeys.ZOMBIE_NAUTILUS_VARIANT, id);
	}

	/**
	 * Bootstrap.
	 *
	 * @param registry registry
	 */
	public static void bootstrap(Registerable<ZombieNautilusVariant> registry) {
		register(
				registry,
				TEMPERATE,
				ZombieNautilusVariant.Model.NORMAL,
				"zombie_nautilus",
				SpawnConditionSelectors.createFallback(0)
		);
		register(
				registry,
				WARM,
				ZombieNautilusVariant.Model.WARM,
				"zombie_nautilus_coral",
				BiomeTags.SPAWNS_CORAL_VARIANT_ZOMBIE_NAUTILUS
		);
	}

	private static void register(
			Registerable<ZombieNautilusVariant> registry,
			RegistryKey<ZombieNautilusVariant> key,
			ZombieNautilusVariant.Model model,
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
			Registerable<ZombieNautilusVariant> registry,
			RegistryKey<ZombieNautilusVariant> key,
			ZombieNautilusVariant.Model model,
			String textureName,
			SpawnConditionSelectors spawnConditions
	) {
		Identifier identifier = Identifier.ofVanilla("entity/nautilus/" + textureName);
		registry.register(key, new ZombieNautilusVariant(new ModelAndTexture<>(model, identifier), spawnConditions));
	}
}
