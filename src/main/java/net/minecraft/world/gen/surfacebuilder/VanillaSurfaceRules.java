package net.minecraft.world.gen.surfacebuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.noise.NoiseParametersKeys;

/**
 * Фабрика правил поверхности для ванильных измерений Minecraft.
 * Содержит статические методы для создания полных наборов правил поверхности
 * для Верхнего мира, Нижнего мира и Края, определяющих какие блоки
 * размещаются на поверхности и под ней в зависимости от биома, высоты и шума.
 */
public class VanillaSurfaceRules {

	private static final MaterialRules.MaterialRule AIR = block(Blocks.AIR);
	private static final MaterialRules.MaterialRule BEDROCK = block(Blocks.BEDROCK);
	private static final MaterialRules.MaterialRule WHITE_TERRACOTTA = block(Blocks.WHITE_TERRACOTTA);
	private static final MaterialRules.MaterialRule ORANGE_TERRACOTTA = block(Blocks.ORANGE_TERRACOTTA);
	private static final MaterialRules.MaterialRule TERRACOTTA = block(Blocks.TERRACOTTA);
	private static final MaterialRules.MaterialRule RED_SAND = block(Blocks.RED_SAND);
	private static final MaterialRules.MaterialRule RED_SANDSTONE = block(Blocks.RED_SANDSTONE);
	private static final MaterialRules.MaterialRule STONE = block(Blocks.STONE);
	private static final MaterialRules.MaterialRule DEEPSLATE = block(Blocks.DEEPSLATE);
	private static final MaterialRules.MaterialRule DIRT = block(Blocks.DIRT);
	private static final MaterialRules.MaterialRule PODZOL = block(Blocks.PODZOL);
	private static final MaterialRules.MaterialRule COARSE_DIRT = block(Blocks.COARSE_DIRT);
	private static final MaterialRules.MaterialRule MYCELIUM = block(Blocks.MYCELIUM);
	private static final MaterialRules.MaterialRule GRASS_BLOCK = block(Blocks.GRASS_BLOCK);
	private static final MaterialRules.MaterialRule CALCITE = block(Blocks.CALCITE);
	private static final MaterialRules.MaterialRule GRAVEL = block(Blocks.GRAVEL);
	private static final MaterialRules.MaterialRule SAND = block(Blocks.SAND);
	private static final MaterialRules.MaterialRule SANDSTONE = block(Blocks.SANDSTONE);
	private static final MaterialRules.MaterialRule PACKED_ICE = block(Blocks.PACKED_ICE);
	private static final MaterialRules.MaterialRule SNOW_BLOCK = block(Blocks.SNOW_BLOCK);
	private static final MaterialRules.MaterialRule MUD = block(Blocks.MUD);
	private static final MaterialRules.MaterialRule POWDER_SNOW = block(Blocks.POWDER_SNOW);
	private static final MaterialRules.MaterialRule ICE = block(Blocks.ICE);
	private static final MaterialRules.MaterialRule WATER = block(Blocks.WATER);
	private static final MaterialRules.MaterialRule LAVA = block(Blocks.LAVA);
	private static final MaterialRules.MaterialRule NETHERRACK = block(Blocks.NETHERRACK);
	private static final MaterialRules.MaterialRule SOUL_SAND = block(Blocks.SOUL_SAND);
	private static final MaterialRules.MaterialRule SOUL_SOIL = block(Blocks.SOUL_SOIL);
	private static final MaterialRules.MaterialRule BASALT = block(Blocks.BASALT);
	private static final MaterialRules.MaterialRule BLACKSTONE = block(Blocks.BLACKSTONE);
	private static final MaterialRules.MaterialRule WARPED_WART_BLOCK = block(Blocks.WARPED_WART_BLOCK);
	private static final MaterialRules.MaterialRule WARPED_NYLIUM = block(Blocks.WARPED_NYLIUM);
	private static final MaterialRules.MaterialRule NETHER_WART_BLOCK = block(Blocks.NETHER_WART_BLOCK);
	private static final MaterialRules.MaterialRule CRIMSON_NYLIUM = block(Blocks.CRIMSON_NYLIUM);
	private static final MaterialRules.MaterialRule END_STONE = block(Blocks.END_STONE);

	private static MaterialRules.MaterialRule block(Block block) {
		return MaterialRules.block(block.getDefaultState());
	}

	public static MaterialRules.MaterialRule createOverworldSurfaceRule() {
		return createDefaultRule(true, false, true);
	}

	public static MaterialRules.MaterialRule createDefaultRule(
			boolean surface,
			boolean bedrockRoof,
			boolean bedrockFloor
	) {
		MaterialRules.MaterialCondition aboveY97 = MaterialRules.aboveY(YOffset.fixed(97), 2);
		MaterialRules.MaterialCondition aboveY256 = MaterialRules.aboveY(YOffset.fixed(256), 0);
		MaterialRules.MaterialCondition aboveY63WithDepth = MaterialRules.aboveYWithStoneDepth(YOffset.fixed(63), -1);
		MaterialRules.MaterialCondition aboveY74WithDepth = MaterialRules.aboveYWithStoneDepth(YOffset.fixed(74), 1);
		MaterialRules.MaterialCondition aboveY60 = MaterialRules.aboveY(YOffset.fixed(60), 0);
		MaterialRules.MaterialCondition aboveY62 = MaterialRules.aboveY(YOffset.fixed(62), 0);
		MaterialRules.MaterialCondition aboveY63 = MaterialRules.aboveY(YOffset.fixed(63), 0);
		MaterialRules.MaterialCondition waterAtSurface = MaterialRules.water(-1, 0);
		MaterialRules.MaterialCondition waterAtOrAbove = MaterialRules.water(0, 0);
		MaterialRules.MaterialCondition waterWithDepth = MaterialRules.waterWithStoneDepth(-6, -1);
		MaterialRules.MaterialCondition isHole = MaterialRules.hole();
		MaterialRules.MaterialCondition isFrozenOcean = MaterialRules.biome(BiomeKeys.FROZEN_OCEAN, BiomeKeys.DEEP_FROZEN_OCEAN);
		MaterialRules.MaterialCondition isSteepSlope = MaterialRules.steepSlope();
		MaterialRules.MaterialRule grassOrDirt = MaterialRules.sequence(MaterialRules.condition(waterAtOrAbove, GRASS_BLOCK), DIRT);
		MaterialRules.MaterialRule sandOrSandstone = MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, SANDSTONE), SAND);
		MaterialRules.MaterialRule gravelOrStone = MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, STONE), GRAVEL);
		MaterialRules.MaterialCondition isSandyBiome = MaterialRules.biome(BiomeKeys.WARM_OCEAN, BiomeKeys.BEACH, BiomeKeys.SNOWY_BEACH);
		MaterialRules.MaterialCondition isDesert = MaterialRules.biome(BiomeKeys.DESERT);
		MaterialRules.MaterialRule rockyTopRule = MaterialRules.sequence(
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.STONY_PEAKS),
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.CALCITE, -0.0125, 0.0125),
										CALCITE
								),
								STONE
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.STONY_SHORE),
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.GRAVEL, -0.05, 0.05),
										gravelOrStone
								),
								STONE
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.WINDSWEPT_HILLS),
						MaterialRules.condition(surfaceNoiseThreshold(1.0), STONE)
				),
				MaterialRules.condition(isSandyBiome, sandOrSandstone),
				MaterialRules.condition(isDesert, sandOrSandstone),
				MaterialRules.condition(MaterialRules.biome(BiomeKeys.DRIPSTONE_CAVES), STONE)
		);
		MaterialRules.MaterialRule powderSnowNarrow = MaterialRules.condition(
				MaterialRules.noiseThreshold(NoiseParametersKeys.POWDER_SNOW, 0.45, 0.58),
				MaterialRules.condition(waterAtOrAbove, POWDER_SNOW)
		);
		MaterialRules.MaterialRule powderSnowWide = MaterialRules.condition(
				MaterialRules.noiseThreshold(NoiseParametersKeys.POWDER_SNOW, 0.35, 0.6),
				MaterialRules.condition(waterAtOrAbove, POWDER_SNOW)
		);
		MaterialRules.MaterialRule underSurfaceRule = MaterialRules.sequence(
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.FROZEN_PEAKS),
						MaterialRules.sequence(
								MaterialRules.condition(isSteepSlope, PACKED_ICE),
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.PACKED_ICE, -0.5, 0.2),
										PACKED_ICE
								),
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.ICE, -0.0625, 0.025),
										ICE
								),
								MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.SNOWY_SLOPES),
						MaterialRules.sequence(
								MaterialRules.condition(isSteepSlope, STONE),
								powderSnowNarrow,
								MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
						)
				),
				MaterialRules.condition(MaterialRules.biome(BiomeKeys.JAGGED_PEAKS), STONE),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.GROVE),
						MaterialRules.sequence(powderSnowNarrow, DIRT)
				),
				rockyTopRule,
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.WINDSWEPT_SAVANNA),
						MaterialRules.condition(surfaceNoiseThreshold(1.75), STONE)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS),
						MaterialRules.sequence(
								MaterialRules.condition(surfaceNoiseThreshold(2.0), gravelOrStone),
								MaterialRules.condition(surfaceNoiseThreshold(1.0), STONE),
								MaterialRules.condition(surfaceNoiseThreshold(-1.0), DIRT),
								gravelOrStone
						)
				),
				MaterialRules.condition(MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP), MUD),
				DIRT
		);
		MaterialRules.MaterialRule topSurfaceRule = MaterialRules.sequence(
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.FROZEN_PEAKS),
						MaterialRules.sequence(
								MaterialRules.condition(isSteepSlope, PACKED_ICE),
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.PACKED_ICE, 0.0, 0.2),
										PACKED_ICE
								),
								MaterialRules.condition(
										MaterialRules.noiseThreshold(NoiseParametersKeys.ICE, 0.0, 0.025),
										ICE
								),
								MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.SNOWY_SLOPES),
						MaterialRules.sequence(
								MaterialRules.condition(isSteepSlope, STONE),
								powderSnowWide,
								MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.JAGGED_PEAKS),
						MaterialRules.sequence(
								MaterialRules.condition(isSteepSlope, STONE),
								MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.GROVE),
						MaterialRules.sequence(powderSnowWide, MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK))
				),
				rockyTopRule,
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.WINDSWEPT_SAVANNA),
						MaterialRules.sequence(
								MaterialRules.condition(surfaceNoiseThreshold(1.75), STONE),
								MaterialRules.condition(surfaceNoiseThreshold(-0.5), COARSE_DIRT)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS),
						MaterialRules.sequence(
								MaterialRules.condition(surfaceNoiseThreshold(2.0), gravelOrStone),
								MaterialRules.condition(surfaceNoiseThreshold(1.0), STONE),
								MaterialRules.condition(surfaceNoiseThreshold(-1.0), grassOrDirt),
								gravelOrStone
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA),
						MaterialRules.sequence(
								MaterialRules.condition(surfaceNoiseThreshold(1.75), COARSE_DIRT),
								MaterialRules.condition(surfaceNoiseThreshold(-0.95), PODZOL)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.ICE_SPIKES),
						MaterialRules.condition(waterAtOrAbove, SNOW_BLOCK)
				),
				MaterialRules.condition(MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP), MUD),
				MaterialRules.condition(MaterialRules.biome(BiomeKeys.MUSHROOM_FIELDS), MYCELIUM),
				grassOrDirt
		);
		MaterialRules.MaterialCondition surfaceNoiseLow = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, -0.909, -0.5454);
		MaterialRules.MaterialCondition surfaceNoiseMid = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, -0.1818, 0.1818);
		MaterialRules.MaterialCondition surfaceNoiseHigh = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, 0.5454, 0.909);
		MaterialRules.MaterialRule overworldRule = MaterialRules.sequence(
				MaterialRules.condition(
						MaterialRules.STONE_DEPTH_FLOOR,
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.WOODED_BADLANDS),
										MaterialRules.condition(
												aboveY97,
												MaterialRules.sequence(
														MaterialRules.condition(surfaceNoiseLow, COARSE_DIRT),
														MaterialRules.condition(surfaceNoiseMid, COARSE_DIRT),
														MaterialRules.condition(surfaceNoiseHigh, COARSE_DIRT),
														grassOrDirt
												)
										)
								),
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.SWAMP),
										MaterialRules.condition(
												aboveY62,
												MaterialRules.condition(
														MaterialRules.not(aboveY63),
														MaterialRules.condition(
																MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE_SWAMP, 0.0),
																WATER
														)
												)
										)
								),
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP),
										MaterialRules.condition(
												aboveY60,
												MaterialRules.condition(
														MaterialRules.not(aboveY63),
														MaterialRules.condition(
																MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE_SWAMP, 0.0),
																WATER
														)
												)
										)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS),
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR,
										MaterialRules.sequence(
												MaterialRules.condition(aboveY256, ORANGE_TERRACOTTA),
												MaterialRules.condition(
														aboveY74WithDepth,
														MaterialRules.sequence(
																MaterialRules.condition(surfaceNoiseLow, TERRACOTTA),
																MaterialRules.condition(surfaceNoiseMid, TERRACOTTA),
																MaterialRules.condition(surfaceNoiseHigh, TERRACOTTA),
																MaterialRules.terracottaBands()
														)
												),
												MaterialRules.condition(
														waterAtSurface,
														MaterialRules.sequence(
																MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, RED_SANDSTONE),
																RED_SAND
														)
												),
												MaterialRules.condition(MaterialRules.not(isHole), ORANGE_TERRACOTTA),
												MaterialRules.condition(waterWithDepth, WHITE_TERRACOTTA),
												gravelOrStone
										)
								),
								MaterialRules.condition(
										aboveY63WithDepth,
										MaterialRules.sequence(
												MaterialRules.condition(
														aboveY63,
														MaterialRules.condition(MaterialRules.not(aboveY74WithDepth), ORANGE_TERRACOTTA)
												),
												MaterialRules.terracottaBands()
										)
								),
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH,
										MaterialRules.condition(waterWithDepth, WHITE_TERRACOTTA)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.STONE_DEPTH_FLOOR,
						MaterialRules.condition(
								waterAtSurface,
								MaterialRules.sequence(
										MaterialRules.condition(
												isFrozenOcean,
												MaterialRules.condition(
														isHole,
														MaterialRules.sequence(
																MaterialRules.condition(waterAtOrAbove, AIR),
																MaterialRules.condition(MaterialRules.temperature(), ICE),
																WATER
														)
												)
										),
										topSurfaceRule
								)
						)
				),
				MaterialRules.condition(
						waterWithDepth,
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR,
										MaterialRules.condition(isFrozenOcean, MaterialRules.condition(isHole, WATER))
								),
								MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH, underSurfaceRule),
								MaterialRules.condition(
										isSandyBiome,
										MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_6, SANDSTONE)
								),
								MaterialRules.condition(
										isDesert,
										MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_30, SANDSTONE)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.STONE_DEPTH_FLOOR,
						MaterialRules.sequence(
								MaterialRules.condition(MaterialRules.biome(BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS), STONE),
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.DEEP_LUKEWARM_OCEAN),
										sandOrSandstone
								),
								gravelOrStone
						)
				)
		);
		Builder<MaterialRules.MaterialRule> builder = ImmutableList.builder();

		if (bedrockRoof) {
			builder.add(MaterialRules.condition(
					MaterialRules.not(MaterialRules.verticalGradient("bedrock_roof", YOffset.belowTop(5), YOffset.getTop())),
					BEDROCK
			));
		}

		if (bedrockFloor) {
			builder.add(MaterialRules.condition(
					MaterialRules.verticalGradient("bedrock_floor", YOffset.getBottom(), YOffset.aboveBottom(5)),
					BEDROCK
			));
		}

		MaterialRules.MaterialRule surfaceOnlyRule = MaterialRules.condition(MaterialRules.surface(), overworldRule);
		builder.add(surface ? surfaceOnlyRule : overworldRule);
		builder.add(MaterialRules.condition(
				MaterialRules.verticalGradient(
						"deepslate",
						YOffset.fixed(0),
						YOffset.fixed(8)
				), DEEPSLATE
		));
		return MaterialRules.sequence(
				builder.build()
						.toArray(MaterialRules.MaterialRule[]::new)
		);
	}

	public static MaterialRules.MaterialRule createNetherSurfaceRule() {
		MaterialRules.MaterialCondition aboveY31 = MaterialRules.aboveY(YOffset.fixed(31), 0);
		MaterialRules.MaterialCondition aboveY32 = MaterialRules.aboveY(YOffset.fixed(32), 0);
		MaterialRules.MaterialCondition aboveY30WithDepth = MaterialRules.aboveYWithStoneDepth(YOffset.fixed(30), 0);
		MaterialRules.MaterialCondition notAboveY35WithDepth = MaterialRules.not(MaterialRules.aboveYWithStoneDepth(YOffset.fixed(35), 0));
		MaterialRules.MaterialCondition nearNetherRoof = MaterialRules.aboveY(YOffset.belowTop(5), 0);
		MaterialRules.MaterialCondition isHole = MaterialRules.hole();
		MaterialRules.MaterialCondition soulSandNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.SOUL_SAND_LAYER, -0.012);
		MaterialRules.MaterialCondition gravelNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.GRAVEL_LAYER, -0.012);
		MaterialRules.MaterialCondition patchNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.PATCH, -0.012);
		MaterialRules.MaterialCondition netherrackNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.NETHERRACK, 0.54);
		MaterialRules.MaterialCondition netherWartNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.NETHER_WART, 1.17);
		MaterialRules.MaterialCondition netherStateSelectorNoise = MaterialRules.noiseThreshold(NoiseParametersKeys.NETHER_STATE_SELECTOR, 0.0);
		MaterialRules.MaterialRule gravelPatchRule = MaterialRules.condition(
				patchNoise,
				MaterialRules.condition(aboveY30WithDepth, MaterialRules.condition(notAboveY35WithDepth, GRAVEL))
		);

		return MaterialRules.sequence(
				MaterialRules.condition(
						MaterialRules.verticalGradient("bedrock_floor", YOffset.getBottom(), YOffset.aboveBottom(5)),
						BEDROCK
				),
				MaterialRules.condition(
						MaterialRules.not(MaterialRules.verticalGradient("bedrock_roof", YOffset.belowTop(5), YOffset.getTop())),
						BEDROCK
				),
				MaterialRules.condition(nearNetherRoof, NETHERRACK),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.BASALT_DELTAS),
						MaterialRules.sequence(
								MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING_WITH_SURFACE_DEPTH, BASALT),
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH,
										MaterialRules.sequence(
												gravelPatchRule,
												MaterialRules.condition(netherStateSelectorNoise, BASALT),
												BLACKSTONE
										)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.SOUL_SAND_VALLEY),
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_CEILING_WITH_SURFACE_DEPTH,
										MaterialRules.sequence(
												MaterialRules.condition(netherStateSelectorNoise, SOUL_SAND),
												SOUL_SOIL
										)
								),
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH,
										MaterialRules.sequence(
												gravelPatchRule,
												MaterialRules.condition(netherStateSelectorNoise, SOUL_SAND),
												SOUL_SOIL
										)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.STONE_DEPTH_FLOOR,
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.not(aboveY32),
										MaterialRules.condition(isHole, LAVA)
								),
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.WARPED_FOREST),
										MaterialRules.condition(
												MaterialRules.not(netherrackNoise),
												MaterialRules.condition(
														aboveY31,
														MaterialRules.sequence(
																MaterialRules.condition(netherWartNoise, WARPED_WART_BLOCK),
																WARPED_NYLIUM
														)
												)
										)
								),
								MaterialRules.condition(
										MaterialRules.biome(BiomeKeys.CRIMSON_FOREST),
										MaterialRules.condition(
												MaterialRules.not(netherrackNoise),
												MaterialRules.condition(
														aboveY31,
														MaterialRules.sequence(
																MaterialRules.condition(netherWartNoise, NETHER_WART_BLOCK),
																CRIMSON_NYLIUM
														)
												)
										)
								)
						)
				),
				MaterialRules.condition(
						MaterialRules.biome(BiomeKeys.NETHER_WASTES),
						MaterialRules.sequence(
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH,
										MaterialRules.condition(
												soulSandNoise,
												MaterialRules.sequence(
														MaterialRules.condition(
																isHole,
																MaterialRules.condition(
																		aboveY30WithDepth,
																		MaterialRules.condition(notAboveY35WithDepth, SOUL_SAND)
																)
														),
														NETHERRACK
												)
										)
								),
								MaterialRules.condition(
										MaterialRules.STONE_DEPTH_FLOOR,
										MaterialRules.condition(
												aboveY31,
												MaterialRules.condition(
														notAboveY35WithDepth,
														MaterialRules.condition(
																gravelNoise,
																MaterialRules.sequence(
																		MaterialRules.condition(aboveY32, GRAVEL),
																		MaterialRules.condition(isHole, GRAVEL)
																)
														)
												)
										)
								)
						)
				),
				NETHERRACK
		);
	}

	public static MaterialRules.MaterialRule getEndStoneRule() {
		return END_STONE;
	}

	public static MaterialRules.MaterialRule getAirRule() {
		return AIR;
	}

	private static MaterialRules.MaterialCondition surfaceNoiseThreshold(double min) {
		return MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, min / 8.25, Double.MAX_VALUE);
	}
}
