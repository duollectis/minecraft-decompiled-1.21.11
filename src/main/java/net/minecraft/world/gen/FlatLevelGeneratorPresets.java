package net.minecraft.world.gen;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSetKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реестр предустановленных конфигураций плоского мира.
 * Каждый пресет задаёт слои блоков, биом, набор структур и флаги генерации.
 */
public class FlatLevelGeneratorPresets {

	public static final RegistryKey<FlatLevelGeneratorPreset> CLASSIC_FLAT = of("classic_flat");
	public static final RegistryKey<FlatLevelGeneratorPreset> TUNNELERS_DREAM = of("tunnelers_dream");
	public static final RegistryKey<FlatLevelGeneratorPreset> WATER_WORLD = of("water_world");
	public static final RegistryKey<FlatLevelGeneratorPreset> OVERWORLD = of("overworld");
	public static final RegistryKey<FlatLevelGeneratorPreset> SNOWY_KINGDOM = of("snowy_kingdom");
	public static final RegistryKey<FlatLevelGeneratorPreset> BOTTOMLESS_PIT = of("bottomless_pit");
	public static final RegistryKey<FlatLevelGeneratorPreset> DESERT = of("desert");
	public static final RegistryKey<FlatLevelGeneratorPreset> REDSTONE_READY = of("redstone_ready");
	public static final RegistryKey<FlatLevelGeneratorPreset> THE_VOID = of("the_void");

	public static void bootstrap(Registerable<FlatLevelGeneratorPreset> presetRegisterable) {
		new Registrar(presetRegisterable).bootstrap();
	}

	private static RegistryKey<FlatLevelGeneratorPreset> of(String id) {
		return RegistryKey.of(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, Identifier.ofVanilla(id));
	}

	/**
	 * Внутренний регистратор пресетов плоского мира.
	 */
	static class Registrar {

		private final Registerable<FlatLevelGeneratorPreset> presetRegisterable;

		Registrar(Registerable<FlatLevelGeneratorPreset> presetRegisterable) {
			this.presetRegisterable = presetRegisterable;
		}

		/**
		 * Создаёт и регистрирует пресет плоского мира.
		 * Слои передаются в порядке сверху вниз и добавляются в обратном порядке в конфиг.
		 *
		 * @param registryKey       ключ реестра пресета
		 * @param icon              иконка для отображения в UI
		 * @param biome             биом плоского мира
		 * @param structureSetKeys  набор структур для генерации
		 * @param hasFeatures       включить генерацию фич биома
		 * @param hasLakes          включить генерацию озёр
		 * @param layers            слои блоков сверху вниз
		 */
		private void createAndRegister(
			RegistryKey<FlatLevelGeneratorPreset> registryKey,
			ItemConvertible icon,
			RegistryKey<Biome> biome,
			Set<RegistryKey<StructureSet>> structureSetKeys,
			boolean hasFeatures,
			boolean hasLakes,
			FlatChunkGeneratorLayer... layers
		) {
			RegistryEntryLookup<StructureSet> structureLookup =
				presetRegisterable.getRegistryLookup(RegistryKeys.STRUCTURE_SET);
			RegistryEntryLookup<PlacedFeature> featureLookup =
				presetRegisterable.getRegistryLookup(RegistryKeys.PLACED_FEATURE);
			RegistryEntryLookup<Biome> biomeLookup =
				presetRegisterable.getRegistryLookup(RegistryKeys.BIOME);

			RegistryEntryList.Direct<StructureSet> structureList = RegistryEntryList.of(
				structureSetKeys.stream().map(structureLookup::getOrThrow).collect(Collectors.toList())
			);

			FlatChunkGeneratorConfig config = new FlatChunkGeneratorConfig(
				Optional.of(structureList),
				biomeLookup.getOrThrow(biome),
				FlatChunkGeneratorConfig.getLavaLakes(featureLookup)
			);

			if (hasFeatures) {
				config.enableFeatures();
			}

			if (hasLakes) {
				config.enableLakes();
			}

			for (int index = layers.length - 1; index >= 0; index--) {
				config.getLayers().add(layers[index]);
			}

			presetRegisterable.register(
				registryKey,
				new FlatLevelGeneratorPreset(icon.asItem().getRegistryEntry(), config)
			);
		}

		public void bootstrap() {
			createAndRegister(
				CLASSIC_FLAT,
				Blocks.GRASS_BLOCK,
				BiomeKeys.PLAINS,
				ImmutableSet.of(StructureSetKeys.VILLAGES),
				false,
				false,
				new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
				new FlatChunkGeneratorLayer(2, Blocks.DIRT),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				TUNNELERS_DREAM,
				Blocks.STONE,
				BiomeKeys.WINDSWEPT_HILLS,
				ImmutableSet.of(StructureSetKeys.MINESHAFTS, StructureSetKeys.STRONGHOLDS),
				true,
				false,
				new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
				new FlatChunkGeneratorLayer(5, Blocks.DIRT),
				new FlatChunkGeneratorLayer(230, Blocks.STONE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				WATER_WORLD,
				Items.WATER_BUCKET,
				BiomeKeys.DEEP_OCEAN,
				ImmutableSet.of(
					StructureSetKeys.OCEAN_RUINS,
					StructureSetKeys.SHIPWRECKS,
					StructureSetKeys.OCEAN_MONUMENTS
				),
				false,
				false,
				new FlatChunkGeneratorLayer(90, Blocks.WATER),
				new FlatChunkGeneratorLayer(5, Blocks.GRAVEL),
				new FlatChunkGeneratorLayer(5, Blocks.DIRT),
				new FlatChunkGeneratorLayer(5, Blocks.STONE),
				new FlatChunkGeneratorLayer(64, Blocks.DEEPSLATE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				OVERWORLD,
				Blocks.SHORT_GRASS,
				BiomeKeys.PLAINS,
				ImmutableSet.of(
					StructureSetKeys.VILLAGES,
					StructureSetKeys.MINESHAFTS,
					StructureSetKeys.PILLAGER_OUTPOSTS,
					StructureSetKeys.RUINED_PORTALS,
					StructureSetKeys.STRONGHOLDS
				),
				true,
				true,
				new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
				new FlatChunkGeneratorLayer(3, Blocks.DIRT),
				new FlatChunkGeneratorLayer(59, Blocks.STONE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				SNOWY_KINGDOM,
				Blocks.SNOW,
				BiomeKeys.SNOWY_PLAINS,
				ImmutableSet.of(StructureSetKeys.VILLAGES, StructureSetKeys.IGLOOS),
				false,
				false,
				new FlatChunkGeneratorLayer(1, Blocks.SNOW),
				new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
				new FlatChunkGeneratorLayer(3, Blocks.DIRT),
				new FlatChunkGeneratorLayer(59, Blocks.STONE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				BOTTOMLESS_PIT,
				Items.FEATHER,
				BiomeKeys.PLAINS,
				ImmutableSet.of(StructureSetKeys.VILLAGES),
				false,
				false,
				new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
				new FlatChunkGeneratorLayer(3, Blocks.DIRT),
				new FlatChunkGeneratorLayer(2, Blocks.COBBLESTONE)
			);
			createAndRegister(
				DESERT,
				Blocks.SAND,
				BiomeKeys.DESERT,
				ImmutableSet.of(
					StructureSetKeys.VILLAGES,
					StructureSetKeys.DESERT_PYRAMIDS,
					StructureSetKeys.MINESHAFTS,
					StructureSetKeys.STRONGHOLDS
				),
				true,
				false,
				new FlatChunkGeneratorLayer(8, Blocks.SAND),
				new FlatChunkGeneratorLayer(52, Blocks.SANDSTONE),
				new FlatChunkGeneratorLayer(3, Blocks.STONE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				REDSTONE_READY,
				Items.REDSTONE,
				BiomeKeys.DESERT,
				ImmutableSet.of(),
				false,
				false,
				new FlatChunkGeneratorLayer(116, Blocks.SANDSTONE),
				new FlatChunkGeneratorLayer(3, Blocks.STONE),
				new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)
			);
			createAndRegister(
				THE_VOID,
				Blocks.BARRIER,
				BiomeKeys.THE_VOID,
				ImmutableSet.of(),
				true,
				false,
				new FlatChunkGeneratorLayer(1, Blocks.AIR)
			);
		}
	}
}
