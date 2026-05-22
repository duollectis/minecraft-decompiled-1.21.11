package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.TypeReferences;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;

/**
 * Мигрирует настройки генерации мира (WorldGenSettings) из старого формата в новый.
 * <p>
 * Конвертирует устаревшие поля {@code generatorName}, {@code generatorOptions} и {@code RandomSeed}
 * в современную структуру с измерениями ({@code dimensions}), источниками биомов ({@code biome_source})
 * и параметрами структур ({@code structures}). Применяется при обновлении мира до версии 1.16+.
 */
public class StructureSeparationDataFix extends DataFix {

	private static final String VILLAGE_STRUCTURE_ID = "minecraft:village";
	private static final String DESERT_PYRAMID_STRUCTURE_ID = "minecraft:desert_pyramid";
	private static final String IGLOO_STRUCTURE_ID = "minecraft:igloo";
	private static final String JUNGLE_PYRAMID_STRUCTURE_ID = "minecraft:jungle_pyramid";
	private static final String SWAMP_HUT_STRUCTURE_ID = "minecraft:swamp_hut";
	private static final String PILLAGER_OUTPOST_STRUCTURE_ID = "minecraft:pillager_outpost";
	private static final String END_CITY_STRUCTURE_ID = "minecraft:endcity";
	private static final String MANSION_STRUCTURE_ID = "minecraft:mansion";
	private static final String MONUMENT_STRUCTURE_ID = "minecraft:monument";
	private static final ImmutableMap<String, StructureSeparationDataFix.Information>
			STRUCTURE_SPACING =
			ImmutableMap.<String, StructureSeparationDataFix.Information>builder()
			            .put("minecraft:village", new StructureSeparationDataFix.Information(32, 8, 10387312))
			            .put("minecraft:desert_pyramid", new StructureSeparationDataFix.Information(32, 8, 14357617))
			            .put("minecraft:igloo", new StructureSeparationDataFix.Information(32, 8, 14357618))
			            .put("minecraft:jungle_pyramid", new StructureSeparationDataFix.Information(32, 8, 14357619))
			            .put("minecraft:swamp_hut", new StructureSeparationDataFix.Information(32, 8, 14357620))
			            .put("minecraft:pillager_outpost", new StructureSeparationDataFix.Information(32, 8, 165745296))
			            .put("minecraft:monument", new StructureSeparationDataFix.Information(32, 5, 10387313))
			            .put("minecraft:endcity", new StructureSeparationDataFix.Information(20, 11, 10387313))
			            .put("minecraft:mansion", new StructureSeparationDataFix.Information(80, 20, 10387319))
			            .build();

	public StructureSeparationDataFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"WorldGenSettings building",
				getInputSchema().getType(TypeReferences.WORLD_GEN_SETTINGS),
				worldGenSettingsTyped -> worldGenSettingsTyped.update(
						DSL.remainderFinder(),
						StructureSeparationDataFix::updateWorldGenSettings
				)
		);
	}

	private static <T> Dynamic<T> createGeneratorSettings(
			long seed, DynamicLike<T> worldGenSettingsDynamic, Dynamic<T> settingsDynamic, Dynamic<T> biomeSourceDynamic
	) {
		return worldGenSettingsDynamic.createMap(
				ImmutableMap.of(
						worldGenSettingsDynamic.createString("type"),
						worldGenSettingsDynamic.createString("minecraft:noise"),
						worldGenSettingsDynamic.createString("biome_source"),
						biomeSourceDynamic,
						worldGenSettingsDynamic.createString("seed"),
						worldGenSettingsDynamic.createLong(seed),
						worldGenSettingsDynamic.createString("settings"),
						settingsDynamic
				)
		);
	}

	private static <T> Dynamic<T> createBiomeSource(
			Dynamic<T> worldGenSettingsDynamic,
			long seed,
			boolean legacyBiomeInitLayer,
			boolean largeBiomes
	) {
		Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.<Dynamic<T>, Dynamic<T>>builder()
		                                                      .put(
				                                                      worldGenSettingsDynamic.createString("type"),
				                                                      worldGenSettingsDynamic.createString(
						                                                      "minecraft:vanilla_layered")
		                                                      )
		                                                      .put(
				                                                      worldGenSettingsDynamic.createString("seed"),
				                                                      worldGenSettingsDynamic.createLong(seed)
		                                                      )
		                                                      .put(
				                                                      worldGenSettingsDynamic.createString(
						                                                      "large_biomes"),
				                                                      worldGenSettingsDynamic.createBoolean(largeBiomes)
		                                                      );
		if (legacyBiomeInitLayer) {
			builder.put(
					worldGenSettingsDynamic.createString("legacy_biome_init_layer"),
					worldGenSettingsDynamic.createBoolean(legacyBiomeInitLayer)
			);
		}

		return worldGenSettingsDynamic.createMap(builder.build());
	}

	private static <T> Dynamic<T> updateWorldGenSettings(Dynamic<T> worldGenSettingsDynamic) {
		DynamicOps<T> dynamicOps = worldGenSettingsDynamic.getOps();
		long seed = worldGenSettingsDynamic.get("RandomSeed").asLong(0L);
		Optional<String> generatorName = worldGenSettingsDynamic
				.get("generatorName")
				.asString()
				.map(name -> name.toLowerCase(Locale.ROOT))
				.result();
		Optional<String> legacyCustomOptions = worldGenSettingsDynamic.get("legacy_custom_options")
				.asString()
				.result()
				.map(Optional::of)
				.orElseGet(
						() -> generatorName.equals(Optional.of("customized"))
								? worldGenSettingsDynamic.get("generatorOptions").asString().result()
								: Optional.empty()
				);
		boolean isCaves = false;
		Dynamic<T> overworldGenerator;
		if (generatorName.equals(Optional.of("customized")) || generatorName.isEmpty()) {
			overworldGenerator = createDefaultOverworldGeneratorSettings(worldGenSettingsDynamic, seed);
		}
		else {
			String generatorType = generatorName.get();
			switch (generatorType) {
				case "flat":
					OptionalDynamic<T> flatOptions = worldGenSettingsDynamic.get("generatorOptions");
					Map<Dynamic<T>, Dynamic<T>> flatStructures = createFlatWorldStructureSettings(dynamicOps, flatOptions);
					overworldGenerator = worldGenSettingsDynamic.createMap(
							ImmutableMap.of(
									worldGenSettingsDynamic.createString("type"),
									worldGenSettingsDynamic.createString("minecraft:flat"),
									worldGenSettingsDynamic.createString("settings"),
									worldGenSettingsDynamic.createMap(
											ImmutableMap.of(
													worldGenSettingsDynamic.createString("structures"),
													worldGenSettingsDynamic.createMap(flatStructures),
													worldGenSettingsDynamic.createString("layers"),
													flatOptions.get("layers")
													           .result()
													           .orElseGet(
															           () -> worldGenSettingsDynamic.createList(
																	           Stream.of(
																			           worldGenSettingsDynamic.createMap(
																					           ImmutableMap.of(
																							           worldGenSettingsDynamic.createString("height"),
																							           worldGenSettingsDynamic.createInt(1),
																							           worldGenSettingsDynamic.createString("block"),
																							           worldGenSettingsDynamic.createString("minecraft:bedrock")
																					           )
																			           ),
																			           worldGenSettingsDynamic.createMap(
																					           ImmutableMap.of(
																							           worldGenSettingsDynamic.createString("height"),
																							           worldGenSettingsDynamic.createInt(2),
																							           worldGenSettingsDynamic.createString("block"),
																							           worldGenSettingsDynamic.createString("minecraft:dirt")
																					           )
																			           ),
																			           worldGenSettingsDynamic.createMap(
																					           ImmutableMap.of(
																							           worldGenSettingsDynamic.createString("height"),
																							           worldGenSettingsDynamic.createInt(1),
																							           worldGenSettingsDynamic.createString("block"),
																							           worldGenSettingsDynamic.createString("minecraft:grass_block")
																					           )
																			           )
																	           )
															           )
													           ),
													worldGenSettingsDynamic.createString("biome"),
													worldGenSettingsDynamic.createString(
															flatOptions.get("biome").asString("minecraft:plains")
													)
											)
									)
							)
					);
					break;
				case "debug_all_block_states":
					overworldGenerator = worldGenSettingsDynamic.createMap(
							ImmutableMap.of(
									worldGenSettingsDynamic.createString("type"),
									worldGenSettingsDynamic.createString("minecraft:debug")
							)
					);
					break;
				case "buffet":
					OptionalDynamic<T> buffetOptions = worldGenSettingsDynamic.get("generatorOptions");
					OptionalDynamic<?> chunkGenerator = buffetOptions.get("chunk_generator");
					Optional<String> chunkGeneratorType = chunkGenerator.get("type").asString().result();
					Dynamic<T> dimensionType;
					if (Objects.equals(chunkGeneratorType, Optional.of("minecraft:caves"))) {
						dimensionType = worldGenSettingsDynamic.createString("minecraft:caves");
						isCaves = true;
					}
					else if (Objects.equals(chunkGeneratorType, Optional.of("minecraft:floating_islands"))) {
						dimensionType = worldGenSettingsDynamic.createString("minecraft:floating_islands");
					}
					else {
						dimensionType = worldGenSettingsDynamic.createString("minecraft:overworld");
					}

					Dynamic<T> biomeSource = buffetOptions.get("biome_source")
							.result()
							.orElseGet(
									() -> worldGenSettingsDynamic.createMap(
											ImmutableMap.of(
													worldGenSettingsDynamic.createString("type"),
													worldGenSettingsDynamic.createString("minecraft:fixed")
											)
									)
							);
					Dynamic<T> fixedBiomeSource;
					if (biomeSource.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
						String firstBiome = biomeSource.get("options")
								.get("biomes")
								.asStream()
								.findFirst()
								.flatMap(biomeDynamic -> biomeDynamic.asString().result())
								.orElse("minecraft:ocean");
						fixedBiomeSource = biomeSource
								.remove("options")
								.set("biome", worldGenSettingsDynamic.createString(firstBiome));
					}
					else {
						fixedBiomeSource = biomeSource;
					}

					overworldGenerator = createGeneratorSettings(seed, worldGenSettingsDynamic, dimensionType, fixedBiomeSource);
					break;
				default:
					boolean isDefault = generatorType.equals("default");
					boolean isLegacyDefault = generatorType.equals("default_1_1")
							|| isDefault && worldGenSettingsDynamic.get("generatorVersion").asInt(0) == 0;
					boolean isAmplified = generatorType.equals("amplified");
					boolean isLargeBiomes = generatorType.equals("largebiomes");
					overworldGenerator = createGeneratorSettings(
							seed,
							worldGenSettingsDynamic,
							worldGenSettingsDynamic.createString(isAmplified ? "minecraft:amplified" : "minecraft:overworld"),
							createBiomeSource(worldGenSettingsDynamic, seed, isLegacyDefault, isLargeBiomes)
					);
			}
		}

		boolean generateFeatures = worldGenSettingsDynamic.get("MapFeatures").asBoolean(true);
		boolean bonusChest = worldGenSettingsDynamic.get("BonusChest").asBoolean(false);
		Builder<T, T> builder = ImmutableMap.builder();
		builder.put(dynamicOps.createString("seed"), dynamicOps.createLong(seed));
		builder.put(dynamicOps.createString("generate_features"), dynamicOps.createBoolean(generateFeatures));
		builder.put(dynamicOps.createString("bonus_chest"), dynamicOps.createBoolean(bonusChest));
		builder.put(
				dynamicOps.createString("dimensions"),
				createDimensionSettings(worldGenSettingsDynamic, seed, overworldGenerator, isCaves)
		);
		legacyCustomOptions.ifPresent(options -> builder.put(
				dynamicOps.createString("legacy_custom_options"),
				dynamicOps.createString(options)
		));
		return new Dynamic<>(dynamicOps, dynamicOps.createMap(builder.build()));
	}

	protected static <T> Dynamic<T> createDefaultOverworldGeneratorSettings(
			Dynamic<T> worldGenSettingsDynamic,
			long seed
	) {
		return createGeneratorSettings(
				seed,
				worldGenSettingsDynamic,
				worldGenSettingsDynamic.createString("minecraft:overworld"),
				createBiomeSource(worldGenSettingsDynamic, seed, false, false)
		);
	}

	protected static <T> T createDimensionSettings(
			Dynamic<T> worldGenSettingsDynamic,
			long seed,
			Dynamic<T> generatorSettingsDynamic,
			boolean caves
	) {
		DynamicOps<T> dynamicOps = worldGenSettingsDynamic.getOps();
		return (T) dynamicOps.createMap(
				ImmutableMap.of(
						dynamicOps.createString("minecraft:overworld"),
						dynamicOps.createMap(
								ImmutableMap.of(
										dynamicOps.createString("type"),
										dynamicOps.createString("minecraft:overworld" + (caves ? "_caves" : "")),
										dynamicOps.createString("generator"),
										generatorSettingsDynamic.getValue()
								)
						),
						dynamicOps.createString("minecraft:the_nether"),
						dynamicOps.createMap(
								ImmutableMap.of(
										dynamicOps.createString("type"),
										dynamicOps.createString("minecraft:the_nether"),
										dynamicOps.createString("generator"),
										createGeneratorSettings(
												seed,
												worldGenSettingsDynamic,
												worldGenSettingsDynamic.createString("minecraft:nether"),
												worldGenSettingsDynamic.createMap(
														ImmutableMap.of(
																worldGenSettingsDynamic.createString("type"),
																worldGenSettingsDynamic.createString(
																		"minecraft:multi_noise"),
																worldGenSettingsDynamic.createString("seed"),
																worldGenSettingsDynamic.createLong(seed),
																worldGenSettingsDynamic.createString("preset"),
																worldGenSettingsDynamic.createString("minecraft:nether")
														)
												)
										)
												.getValue()
								)
						),
						dynamicOps.createString("minecraft:the_end"),
						dynamicOps.createMap(
								ImmutableMap.of(
										dynamicOps.createString("type"),
										dynamicOps.createString("minecraft:the_end"),
										dynamicOps.createString("generator"),
										createGeneratorSettings(
												seed,
												worldGenSettingsDynamic,
												worldGenSettingsDynamic.createString("minecraft:end"),
												worldGenSettingsDynamic.createMap(
														ImmutableMap.of(
																worldGenSettingsDynamic.createString("type"),
																worldGenSettingsDynamic.createString("minecraft:the_end"),
																worldGenSettingsDynamic.createString("seed"),
																worldGenSettingsDynamic.createLong(seed)
														)
												)
										)
												.getValue()
								)
						)
				)
		);
	}

	private static <T> Map<Dynamic<T>, Dynamic<T>> createFlatWorldStructureSettings(
			DynamicOps<T> worldGenSettingsDynamicOps, OptionalDynamic<T> generatorOptionsDynamic
	) {
		MutableInt strongholdDistance = new MutableInt(32);
		MutableInt strongholdSpread = new MutableInt(3);
		MutableInt strongholdCount = new MutableInt(128);
		MutableBoolean hasStronghold = new MutableBoolean(false);
		Map<String, StructureSeparationDataFix.Information> map = new HashMap<>();
		if (generatorOptionsDynamic.result().isEmpty()) {
			hasStronghold.setTrue();
			map.put(
					"minecraft:village",
					(StructureSeparationDataFix.Information) STRUCTURE_SPACING.get("minecraft:village")
			);
		}

		generatorOptionsDynamic.get("structures")
		                       .flatMap(Dynamic::getMapValues)
		                       .ifSuccess(
				                       map2 -> map2.forEach(
						                       (oldStructureName, dynamic) -> dynamic.getMapValues()
						                                                             .result()
						                                                             .ifPresent(
								                                                             map2x -> map2x.forEach(
										                                                             (propertyName, spacing) -> {
											                                                             String
													                                                             string =
													                                                             oldStructureName.asString(
															                                                             "");
											                                                             String
													                                                             string2 =
													                                                             propertyName.asString(
															                                                             "");
											                                                             String
													                                                             string3 =
													                                                             spacing.asString(
															                                                             "");
											                                                             if ("stronghold".equals(
											                                                               string)) {
											                                                              hasStronghold.setTrue();
											                                                              switch (string2) {
											                                                               case "distance":
											                                                                strongholdDistance.setValue(
											                                                                  parseInt(
											                                                                    string3,
											                                                                    strongholdDistance.intValue(),
											                                                                    1
											                                                                  ));
											                                                                return;
											                                                               case "spread":
											                                                                strongholdSpread.setValue(
											                                                                  parseInt(
											                                                                    string3,
											                                                                    strongholdSpread.intValue(),
											                                                                    1
											                                                                  ));
											                                                                return;
											                                                               case "count":
											                                                                strongholdCount.setValue(
											                                                                  parseInt(
											                                                                    string3,
											                                                                    strongholdCount.intValue(),
											                                                                    1
											                                                                  ));
											                                                                return;
											                                                              }
											                                                             }
											                                                             else {
												                                                             switch (string2) {
													                                                             case "distance":
														                                                             switch (string) {
															                                                             case "village":
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:village",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             return;
															                                                             case "biome_1":
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:desert_pyramid",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:igloo",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:jungle_pyramid",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:swamp_hut",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:pillager_outpost",
																		                                                             string3,
																		                                                             9
																                                                             );
																                                                             return;
															                                                             case "endcity":
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:endcity",
																		                                                             string3,
																		                                                             1
																                                                             );
																                                                             return;
															                                                             case "mansion":
																                                                             insertStructureSettings(
																		                                                             map,
																		                                                             "minecraft:mansion",
																		                                                             string3,
																		                                                             1
																                                                             );
																                                                             return;
															                                                             default:
																                                                             return;
														                                                             }
													                                                             case "separation":
													                                                              if ("oceanmonument".equals(
													                                                                string)) {
													                                                               StructureSeparationDataFix.Information
													                                                                 monumentInfo =
													                                                                 map.getOrDefault(
													                                                                   "minecraft:monument",
													                                                                   (StructureSeparationDataFix.Information) STRUCTURE_SPACING.get(
													                                                                     "minecraft:monument")
													                                                                 );
													                                                               int
													                                                                 newSpacing =
													                                                                 parseInt(
													                                                                   string3,
													                                                                   monumentInfo.separation,
													                                                                   1
													                                                                 );
													                                                               map.put(
													                                                                 "minecraft:monument",
													                                                                 new StructureSeparationDataFix.Information(
													                                                                   newSpacing,
													                                                                   monumentInfo.separation,
													                                                                   monumentInfo.salt
													                                                                 )
													                                                               );
													                                                              }

														                                                             return;
													                                                             case "spacing":
														                                                             if ("oceanmonument".equals(
																                                                             string)) {
															                                                             insertStructureSettings(
																	                                                             map,
																	                                                             "minecraft:monument",
																	                                                             string3,
																	                                                             1
															                                                             );
														                                                             }

														                                                             return;
												                                                             }
											                                                             }
										                                                             }
								                                                             )
						                                                             )
				                       )
		                       );
		Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.builder();
		builder.put(
				generatorOptionsDynamic.createString("structures"),
				generatorOptionsDynamic.createMap(
						map.entrySet()
						   .stream()
						   .collect(
								   Collectors.toMap(
										   entry -> generatorOptionsDynamic.createString(entry.getKey()),
										   entry -> entry.getValue().toDynamic(worldGenSettingsDynamicOps)
								   )
						   )
				)
		);
		if (hasStronghold.isTrue()) {
			builder.put(
					generatorOptionsDynamic.createString("stronghold"),
					generatorOptionsDynamic.createMap(
							ImmutableMap.of(
									generatorOptionsDynamic.createString("distance"),
									generatorOptionsDynamic.createInt(strongholdDistance.intValue()),
									generatorOptionsDynamic.createString("spread"),
									generatorOptionsDynamic.createInt(strongholdSpread.intValue()),
									generatorOptionsDynamic.createString("count"),
									generatorOptionsDynamic.createInt(strongholdCount.intValue())
							)
					)
			);
		}

		return builder.build();
	}

	private static int parseInt(String string, int defaultValue) {
		return NumberUtils.toInt(string, defaultValue);
	}

	private static int parseInt(String string, int defaultValue, int minValue) {
		return Math.max(minValue, parseInt(string, defaultValue));
	}

	private static void insertStructureSettings(
			Map<String, StructureSeparationDataFix.Information> map,
			String structureId,
			String spacingStr,
			int minSpacing
	) {
		StructureSeparationDataFix.Information existing = map.getOrDefault(
				structureId, (StructureSeparationDataFix.Information) STRUCTURE_SPACING.get(structureId)
		);
		int newSpacing = parseInt(spacingStr, existing.spacing, minSpacing);
		map.put(structureId, new StructureSeparationDataFix.Information(newSpacing, existing.separation, existing.salt));
	}

	/**
	 * Хранит параметры размещения структуры в мире: шаг сетки ({@code spacing}),
	 * минимальное расстояние между структурами ({@code separation}) и соль генератора ({@code salt}).
	 */
	static final class Information {

		public static final Codec<StructureSeparationDataFix.Information> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.INT.fieldOf("spacing").forGetter(information -> information.spacing),
						                    Codec.INT.fieldOf("separation").forGetter(information -> information.separation),
						                    Codec.INT.fieldOf("salt").forGetter(information -> information.salt)
				                    )
				                    .apply(instance, StructureSeparationDataFix.Information::new)
		);
		final int spacing;
		final int separation;
		final int salt;

		public Information(int spacing, int separation, int salt) {
			this.spacing = spacing;
			this.separation = separation;
			this.salt = salt;
		}

		public <T> Dynamic<T> toDynamic(DynamicOps<T> dynamicOps) {
			return new Dynamic<>(dynamicOps, CODEC.encodeStart(dynamicOps, this).result().orElse(dynamicOps.emptyMap()));
		}
	}
}
