package net.minecraft.world.biome;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.sound.BiomeAdditionsSound;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.carver.ConfiguredCarvers;
import net.minecraft.world.gen.feature.*;

import java.util.List;
import java.util.Optional;

/**
 * Фабрика биомов Нижнего мира (The Nether).
 * Все биомы Нижнего мира не имеют осадков, имеют температуру 2.0 и нулевое количество осадков.
 */
public class TheNetherBiomeCreator {

	private static final int NETHER_WATER_COLOR = 4159204;
	private static final float NETHER_TEMPERATURE = 2.0F;
	private static final float NETHER_DOWNFALL = 0.0F;

	// Цвета тумана биомов Нижнего мира
	private static final int NETHER_WASTES_FOG_COLOR = -13432824;
	private static final int SOUL_SAND_VALLEY_FOG_COLOR = -14989499;
	private static final int BASALT_DELTAS_FOG_COLOR = -9937040;
	private static final int CRIMSON_FOREST_FOG_COLOR = -13434109;
	private static final int WARPED_FOREST_FOG_COLOR = -15071974;

	// Параметры стоимости спавна для Soul Sand Valley
	private static final double SOUL_SAND_VALLEY_SPAWN_ENERGY_BUDGET = 0.7;
	private static final double SOUL_SAND_VALLEY_SPAWN_CHARGE = 0.15;

	// Параметры стоимости спавна для Warped Forest
	private static final double WARPED_FOREST_ENDERMAN_ENERGY_BUDGET = 1.0;
	private static final double WARPED_FOREST_ENDERMAN_CHARGE = 0.12;

	// Параметры звуков настроения (общие для всех биомов Нижнего мира)
	private static final int MOOD_SOUND_DELAY = 6000;
	private static final int MOOD_SOUND_BLOCK_SEARCH_EXTENT = 8;
	private static final double MOOD_SOUND_OFFSET = 2.0;
	private static final double ADDITIONS_SOUND_CHANCE = 0.0111;

	public static Biome.Builder biome() {
		return new Biome.Builder()
			.precipitation(false)
			.temperature(NETHER_TEMPERATURE)
			.downfall(NETHER_DOWNFALL)
			.effects(new BiomeEffects.Builder().waterColor(NETHER_WATER_COLOR).build());
	}

	public static Biome createNetherWastes(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		SpawnSettings spawnSettings = new SpawnSettings.Builder()
			.spawn(SpawnGroup.MONSTER, 50, new SpawnSettings.SpawnEntry(EntityType.GHAST, 4, 4))
			.spawn(SpawnGroup.MONSTER, 100, new SpawnSettings.SpawnEntry(EntityType.ZOMBIFIED_PIGLIN, 4, 4))
			.spawn(SpawnGroup.MONSTER, 2, new SpawnSettings.SpawnEntry(EntityType.MAGMA_CUBE, 4, 4))
			.spawn(SpawnGroup.MONSTER, 1, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 4, 4))
			.spawn(SpawnGroup.MONSTER, 15, new SpawnSettings.SpawnEntry(EntityType.PIGLIN, 4, 4))
			.spawn(SpawnGroup.CREATURE, 60, new SpawnSettings.SpawnEntry(EntityType.STRIDER, 1, 2))
			.build();

		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.carver(ConfiguredCarvers.NETHER_CAVE)
				.feature(GenerationStep.Feature.VEGETAL_DECORATION, MiscPlacedFeatures.SPRING_LAVA);

		DefaultBiomeFeatures.addDefaultMushrooms(generationBuilder);

		generationBuilder
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_OPEN)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_FIRE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_SOUL_FIRE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE_EXTRA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_NETHER)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_NETHER)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_MAGMA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_CLOSED);

		DefaultBiomeFeatures.addNetherMineables(generationBuilder);

		return biome()
			.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, NETHER_WASTES_FOG_COLOR)
			.setEnvironmentAttribute(
				EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
				new BackgroundMusic(SoundEvents.MUSIC_NETHER_NETHER_WASTES)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
				new AmbientSounds(
					Optional.of(SoundEvents.AMBIENT_NETHER_WASTES_LOOP),
					Optional.of(new BiomeMoodSound(
						SoundEvents.AMBIENT_NETHER_WASTES_MOOD,
						MOOD_SOUND_DELAY,
						MOOD_SOUND_BLOCK_SEARCH_EXTENT,
						MOOD_SOUND_OFFSET
					)),
					List.of(new BiomeAdditionsSound(SoundEvents.AMBIENT_NETHER_WASTES_ADDITIONS, ADDITIONS_SOUND_CHANCE))
				)
			)
			.spawnSettings(spawnSettings)
			.generationSettings(generationBuilder.build())
			.build();
	}

	public static Biome createSoulSandValley(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		SpawnSettings spawnSettings = new SpawnSettings.Builder()
			.spawn(SpawnGroup.MONSTER, 20, new SpawnSettings.SpawnEntry(EntityType.SKELETON, 5, 5))
			.spawn(SpawnGroup.MONSTER, 50, new SpawnSettings.SpawnEntry(EntityType.GHAST, 4, 4))
			.spawn(SpawnGroup.MONSTER, 1, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 4, 4))
			.spawn(SpawnGroup.CREATURE, 60, new SpawnSettings.SpawnEntry(EntityType.STRIDER, 1, 2))
			.spawnCost(EntityType.SKELETON, SOUL_SAND_VALLEY_SPAWN_ENERGY_BUDGET, SOUL_SAND_VALLEY_SPAWN_CHARGE)
			.spawnCost(EntityType.GHAST, SOUL_SAND_VALLEY_SPAWN_ENERGY_BUDGET, SOUL_SAND_VALLEY_SPAWN_CHARGE)
			.spawnCost(EntityType.ENDERMAN, SOUL_SAND_VALLEY_SPAWN_ENERGY_BUDGET, SOUL_SAND_VALLEY_SPAWN_CHARGE)
			.spawnCost(EntityType.STRIDER, SOUL_SAND_VALLEY_SPAWN_ENERGY_BUDGET, SOUL_SAND_VALLEY_SPAWN_CHARGE)
			.build();

		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.carver(ConfiguredCarvers.NETHER_CAVE)
				.feature(GenerationStep.Feature.VEGETAL_DECORATION, MiscPlacedFeatures.SPRING_LAVA)
				.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, NetherPlacedFeatures.BASALT_PILLAR)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_OPEN)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_FIRE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_SOUL_FIRE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE_EXTRA)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_CRIMSON_ROOTS)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_MAGMA)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_CLOSED)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_SOUL_SAND);

		DefaultBiomeFeatures.addNetherMineables(generationBuilder);

		return biome()
			.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, SOUL_SAND_VALLEY_FOG_COLOR)
			.setEnvironmentAttribute(
				EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
				new BackgroundMusic(SoundEvents.MUSIC_NETHER_SOUL_SAND_VALLEY)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
				AmbientParticle.of(ParticleTypes.ASH, 0.00625F)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
				new AmbientSounds(
					Optional.of(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP),
					Optional.of(new BiomeMoodSound(
						SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD,
						MOOD_SOUND_DELAY,
						MOOD_SOUND_BLOCK_SEARCH_EXTENT,
						MOOD_SOUND_OFFSET
					)),
					List.of(new BiomeAdditionsSound(
						SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS,
						ADDITIONS_SOUND_CHANCE
					))
				)
			)
			.spawnSettings(spawnSettings)
			.generationSettings(generationBuilder.build())
			.build();
	}

	public static Biome createBasaltDeltas(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		SpawnSettings spawnSettings = new SpawnSettings.Builder()
			.spawn(SpawnGroup.MONSTER, 40, new SpawnSettings.SpawnEntry(EntityType.GHAST, 1, 1))
			.spawn(SpawnGroup.MONSTER, 100, new SpawnSettings.SpawnEntry(EntityType.MAGMA_CUBE, 2, 5))
			.spawn(SpawnGroup.CREATURE, 60, new SpawnSettings.SpawnEntry(EntityType.STRIDER, 1, 2))
			.build();

		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.carver(ConfiguredCarvers.NETHER_CAVE)
				.feature(GenerationStep.Feature.SURFACE_STRUCTURES, NetherPlacedFeatures.DELTA)
				.feature(GenerationStep.Feature.SURFACE_STRUCTURES, NetherPlacedFeatures.SMALL_BASALT_COLUMNS)
				.feature(GenerationStep.Feature.SURFACE_STRUCTURES, NetherPlacedFeatures.LARGE_BASALT_COLUMNS)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.BASALT_BLOBS)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.BLACKSTONE_BLOBS)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_DELTA)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_FIRE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_SOUL_FIRE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE_EXTRA)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_NETHER)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_NETHER)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_MAGMA)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_CLOSED_DOUBLE)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_GOLD_DELTAS)
				.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_QUARTZ_DELTAS);

		DefaultBiomeFeatures.addAncientDebris(generationBuilder);

		return biome()
			.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, BASALT_DELTAS_FOG_COLOR)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
				AmbientParticle.of(ParticleTypes.WHITE_ASH, 0.118093334F)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
				new BackgroundMusic(SoundEvents.MUSIC_NETHER_BASALT_DELTAS)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
				new AmbientSounds(
					Optional.of(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP),
					Optional.of(new BiomeMoodSound(
						SoundEvents.AMBIENT_BASALT_DELTAS_MOOD,
						MOOD_SOUND_DELAY,
						MOOD_SOUND_BLOCK_SEARCH_EXTENT,
						MOOD_SOUND_OFFSET
					)),
					List.of(new BiomeAdditionsSound(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, ADDITIONS_SOUND_CHANCE))
				)
			)
			.spawnSettings(spawnSettings)
			.generationSettings(generationBuilder.build())
			.build();
	}

	public static Biome createCrimsonForest(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		SpawnSettings spawnSettings = new SpawnSettings.Builder()
			.spawn(SpawnGroup.MONSTER, 1, new SpawnSettings.SpawnEntry(EntityType.ZOMBIFIED_PIGLIN, 2, 4))
			.spawn(SpawnGroup.MONSTER, 9, new SpawnSettings.SpawnEntry(EntityType.HOGLIN, 3, 4))
			.spawn(SpawnGroup.MONSTER, 5, new SpawnSettings.SpawnEntry(EntityType.PIGLIN, 3, 4))
			.spawn(SpawnGroup.CREATURE, 60, new SpawnSettings.SpawnEntry(EntityType.STRIDER, 1, 2))
			.build();

		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.carver(ConfiguredCarvers.NETHER_CAVE)
				.feature(GenerationStep.Feature.VEGETAL_DECORATION, MiscPlacedFeatures.SPRING_LAVA);

		DefaultBiomeFeatures.addDefaultMushrooms(generationBuilder);

		generationBuilder
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_OPEN)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_FIRE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE_EXTRA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_MAGMA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_CLOSED)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, NetherPlacedFeatures.WEEPING_VINES)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, TreePlacedFeatures.CRIMSON_FUNGI)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, NetherPlacedFeatures.CRIMSON_FOREST_VEGETATION);

		DefaultBiomeFeatures.addNetherMineables(generationBuilder);

		return biome()
			.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, CRIMSON_FOREST_FOG_COLOR)
			.setEnvironmentAttribute(
				EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
				new BackgroundMusic(SoundEvents.MUSIC_NETHER_CRIMSON_FOREST)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
				AmbientParticle.of(ParticleTypes.CRIMSON_SPORE, 0.025F)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
				new AmbientSounds(
					Optional.of(SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP),
					Optional.of(new BiomeMoodSound(
						SoundEvents.AMBIENT_CRIMSON_FOREST_MOOD,
						MOOD_SOUND_DELAY,
						MOOD_SOUND_BLOCK_SEARCH_EXTENT,
						MOOD_SOUND_OFFSET
					)),
					List.of(new BiomeAdditionsSound(
						SoundEvents.AMBIENT_CRIMSON_FOREST_ADDITIONS,
						ADDITIONS_SOUND_CHANCE
					))
				)
			)
			.spawnSettings(spawnSettings)
			.generationSettings(generationBuilder.build())
			.build();
	}

	public static Biome createWarpedForest(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		SpawnSettings spawnSettings = new SpawnSettings.Builder()
			.spawn(SpawnGroup.MONSTER, 1, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 4, 4))
			.spawn(SpawnGroup.CREATURE, 60, new SpawnSettings.SpawnEntry(EntityType.STRIDER, 1, 2))
			.spawnCost(EntityType.ENDERMAN, WARPED_FOREST_ENDERMAN_ENERGY_BUDGET, WARPED_FOREST_ENDERMAN_CHARGE)
			.build();

		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.carver(ConfiguredCarvers.NETHER_CAVE)
				.feature(GenerationStep.Feature.VEGETAL_DECORATION, MiscPlacedFeatures.SPRING_LAVA);

		DefaultBiomeFeatures.addDefaultMushrooms(generationBuilder);

		generationBuilder
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_OPEN)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_FIRE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.PATCH_SOUL_FIRE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE_EXTRA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.GLOWSTONE)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_MAGMA)
			.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, NetherPlacedFeatures.SPRING_CLOSED)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, TreePlacedFeatures.WARPED_FUNGI)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, NetherPlacedFeatures.WARPED_FOREST_VEGETATION)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, NetherPlacedFeatures.NETHER_SPROUTS)
			.feature(GenerationStep.Feature.VEGETAL_DECORATION, NetherPlacedFeatures.TWISTING_VINES);

		DefaultBiomeFeatures.addNetherMineables(generationBuilder);

		return biome()
			.setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, WARPED_FOREST_FOG_COLOR)
			.setEnvironmentAttribute(
				EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO,
				new BackgroundMusic(SoundEvents.MUSIC_NETHER_WARPED_FOREST)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL,
				AmbientParticle.of(ParticleTypes.WARPED_SPORE, 0.01428F)
			)
			.setEnvironmentAttribute(
				EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO,
				new AmbientSounds(
					Optional.of(SoundEvents.AMBIENT_WARPED_FOREST_LOOP),
					Optional.of(new BiomeMoodSound(
						SoundEvents.AMBIENT_WARPED_FOREST_MOOD,
						MOOD_SOUND_DELAY,
						MOOD_SOUND_BLOCK_SEARCH_EXTENT,
						MOOD_SOUND_OFFSET
					)),
					List.of(new BiomeAdditionsSound(
						SoundEvents.AMBIENT_WARPED_FOREST_ADDITIONS,
						ADDITIONS_SOUND_CHANCE
					))
				)
			)
			.spawnSettings(spawnSettings)
			.generationSettings(generationBuilder.build())
			.build();
	}
}
