package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Мигрирует чанки из старого формата высот (0..255) в новый расширенный формат (-64..319),
 * конвертирует биомы из плоского массива в паллетизированный 3D формат по секциям,
 * а также обновляет хайтмапы, маски карвинга и статус генерации чанка.
 */
public class ChunkHeightAndBiomeFix extends DataFix {

	public static final String CONTEXT = "__context";
	private static final String NAME = "ChunkHeightAndBiomeFix";
	private static final int CHUNK_SECTIONS_IN_OLD_CHUNK = 16;
	private static final int CHUNK_SECTIONS_IN_NEW_CHUNK = 24;
	private static final int MIN_CHUNK_SECTION_Y = -4;
	public static final int BIOME_ARRAY_SIZE = 4096;
	private static final int BIOME_SECTION_COUNT = 64;
	private static final int BIOME_BITS_PER_ENTRY = 9;
	private static final long BIOME_ENTRY_MASK = 511L;
	private static final String[] HEIGHTMAP_KEYS = new String[]{
			"WORLD_SURFACE_WG",
			"WORLD_SURFACE",
			"WORLD_SURFACE_IGNORE_SNOW",
			"OCEAN_FLOOR_WG",
			"OCEAN_FLOOR",
			"MOTION_BLOCKING",
			"MOTION_BLOCKING_NO_LEAVES"
	};
	private static final Set<String>
			STATUSES_TO_SKIP_UPDATE =
			Set.of("surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full");
	private static final Set<String>
			STATUSES_REQUIRING_OLD_NOISE =
			Set.of("noise", "surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full");
	private static final Set<String> SURFACE_BLOCKS = Set.of(
			"minecraft:air",
			"minecraft:basalt",
			"minecraft:bedrock",
			"minecraft:blackstone",
			"minecraft:calcite",
			"minecraft:cave_air",
			"minecraft:coarse_dirt",
			"minecraft:crimson_nylium",
			"minecraft:dirt",
			"minecraft:end_stone",
			"minecraft:grass_block",
			"minecraft:gravel",
			"minecraft:ice",
			"minecraft:lava",
			"minecraft:mycelium",
			"minecraft:nether_wart_block",
			"minecraft:netherrack",
			"minecraft:orange_terracotta",
			"minecraft:packed_ice",
			"minecraft:podzol",
			"minecraft:powder_snow",
			"minecraft:red_sand",
			"minecraft:red_sandstone",
			"minecraft:sand",
			"minecraft:sandstone",
			"minecraft:snow_block",
			"minecraft:soul_sand",
			"minecraft:soul_soil",
			"minecraft:stone",
			"minecraft:terracotta",
			"minecraft:warped_nylium",
			"minecraft:warped_wart_block",
			"minecraft:water",
			"minecraft:white_terracotta"
	);
	private static final int BLOCKS_PER_SECTION = 16;
	private static final int BIOME_ENTRIES_PER_CHUNK_SECTION = 64;
	private static final int UPPER_BIOME_SECTION_OFFSET = 1008;
	private static final int DEEP_OCEAN_BIOME_ID = 24;
	public static final String PLAINS_ID = "minecraft:plains";
	private static final Int2ObjectMap<String> RAW_BIOME_IDS = new Int2ObjectOpenHashMap<>();

	public ChunkHeightAndBiomeFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> inputChunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = inputChunkType.findField("Level");
		OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
		Type<?> outputChunkType = getOutputSchema().getType(TypeReferences.CHUNK);
		Type<?> outputLevelType = outputChunkType.findField("Level").type();
		Type<?> outputSectionsType = outputLevelType.findField("Sections").type();

		return fixTypeEverywhereTyped(
				"ChunkHeightAndBiomeFix",
				inputChunkType,
				outputChunkType,
				chunk -> chunk.updateTyped(
						levelFinder,
						outputLevelType,
						level -> {
							Dynamic<?> levelDynamic = (Dynamic<?>) level.get(DSL.remainderFinder());
							OptionalDynamic<?> context = ((Dynamic) chunk.get(DSL.remainderFinder())).get(CONTEXT);
							String dimension = context.get("dimension").asString().result().orElse("");
							String generator = context.get("generator").asString().result().orElse("");
							boolean isOverworld = "minecraft:overworld".equals(dimension);
							MutableBoolean heightAlreadyUpdated = new MutableBoolean();
							int bottomSectionY = isOverworld ? MIN_CHUNK_SECTION_Y : 0;
							Dynamic<?>[] biomeSections = fixBiomes(levelDynamic, isOverworld, bottomSectionY, heightAlreadyUpdated);
							Dynamic<?> airPalette = fixPalette(
									levelDynamic.createList(Stream.of(levelDynamic.createMap(ImmutableMap.of(
											levelDynamic.createString("Name"),
											levelDynamic.createString("minecraft:air")
									))))
							);
							Set<String> blockNames = Sets.newHashSet();
							MutableObject<Supplier<ProtoChunkTickListFix.PalettedSection>> groundSectionSupplier =
									new MutableObject(
											(Supplier<ProtoChunkTickListFix.PalettedSection>) () -> null
									);

							level = level.updateTyped(
									sectionsFinder,
									outputSectionsType,
									sections -> {
										IntSet processedSectionYs = new IntOpenHashSet();
										Dynamic<?> sectionsData = (Dynamic<?>) sections
												.write()
												.result()
												.orElseThrow(() -> new IllegalStateException(
														"Malformed Chunk.Level.Sections"
												));

										List<Dynamic<?>> sectionList = sectionsData.asStream().map(sectionTag -> {
											int sectionY = sectionTag.get("Y").asInt(0);
											Dynamic<?> blockStates = (Dynamic<?>) DataFixUtils.orElse(
													sectionTag.get("Palette").result().flatMap(palette -> {
														palette
																.asStream()
																.map(entry -> entry.get("Name").asString("minecraft:air"))
																.forEach(blockNames::add);

														return sectionTag
																.get("BlockStates")
																.result()
																.map(rawData -> fixPalette(palette, rawData));
													}),
													airPalette
											);
											Dynamic<?> updatedSection = sectionTag;
											int biomeIndex = sectionY - bottomSectionY;

											if (biomeIndex >= 0 && biomeIndex < biomeSections.length) {
												updatedSection = sectionTag.set("biomes", biomeSections[biomeIndex]);
											}

											processedSectionYs.add(sectionY);

											if (sectionTag.get("Y").asInt(Integer.MAX_VALUE) == 0) {
												groundSectionSupplier.setValue(
														(Supplier<ProtoChunkTickListFix.PalettedSection>) () -> {
															List<? extends Dynamic<?>> palette =
																	blockStates.get("palette").asList(Function.identity());
															long[] data = blockStates.get("data").asLongStream().toArray();
															return new ProtoChunkTickListFix.PalettedSection(palette, data);
														}
												);
											}

											return updatedSection
													.set("block_states", blockStates)
													.remove("Palette")
													.remove("BlockStates");
										}).collect(Collectors.toCollection(ArrayList::new));

										for (int sectionIdx = 0; sectionIdx < biomeSections.length; sectionIdx++) {
											int absoluteSectionY = sectionIdx + bottomSectionY;

											if (processedSectionYs.add(absoluteSectionY)) {
												Dynamic<?> newSection = levelDynamic.createMap(Map.of(
														levelDynamic.createString("Y"),
														levelDynamic.createInt(absoluteSectionY)
												));
												newSection = newSection.set("block_states", airPalette);
												newSection = newSection.set("biomes", biomeSections[sectionIdx]);
												sectionList.add(newSection);
											}
										}

										return Util.readTyped(outputSectionsType, levelDynamic.createList(sectionList.stream()));
									}
							);

							return level.update(
									DSL.remainderFinder(),
									levelData -> {
										if (isOverworld) {
											levelData = fixStatus(levelData, blockNames);
										}

										return fixLevel(
												levelData,
												isOverworld,
												heightAlreadyUpdated.booleanValue(),
												"minecraft:noise".equals(generator),
												(Supplier<ProtoChunkTickListFix.PalettedSection>) groundSectionSupplier.get()
										);
									}
							);
						}
				)
		);
	}

	private Dynamic<?> fixStatus(Dynamic<?> level, Set<String> blocks) {
		return level.update(
				"Status", status -> {
					String statusName = status.asString("empty");

					if (STATUSES_TO_SKIP_UPDATE.contains(statusName)) {
						return status;
					}

					blocks.remove("minecraft:air");
					boolean hasNonAirBlocks = !blocks.isEmpty();
					blocks.removeAll(SURFACE_BLOCKS);
					boolean hasNonSurfaceBlocks = !blocks.isEmpty();

					if (hasNonSurfaceBlocks) {
						return status.createString("liquid_carvers");
					}

					if ("noise".equals(statusName) || hasNonAirBlocks) {
						return status.createString("noise");
					}

					return "biomes".equals(statusName)
							? status.createString("structure_references")
							: status;
				}
		);
	}

	private static Dynamic<?>[] fixBiomes(
			Dynamic<?> level,
			boolean overworld,
			int bottomSectionY,
			MutableBoolean heightAlreadyUpdated
	) {
		Dynamic<?>[] result = new Dynamic[overworld ? CHUNK_SECTIONS_IN_NEW_CHUNK : CHUNK_SECTIONS_IN_OLD_CHUNK];
		int[] rawBiomes = level.get("Biomes").asIntStreamOpt().result().map(IntStream::toArray).orElse(null);

		if (rawBiomes != null && rawBiomes.length == 1536) {
			heightAlreadyUpdated.setValue(true);

			for (int sectionIdx = 0; sectionIdx < CHUNK_SECTIONS_IN_NEW_CHUNK; sectionIdx++) {
				int capturedIdx = sectionIdx;
				result[sectionIdx] = fixBiomes(
						level,
						sectionY -> getClamped(rawBiomes, capturedIdx * BIOME_SECTION_COUNT + sectionY)
				);
			}
		}
		else if (rawBiomes != null && rawBiomes.length == 1024) {
			for (int sectionIdx = 0; sectionIdx < CHUNK_SECTIONS_IN_OLD_CHUNK; sectionIdx++) {
				int targetIdx = sectionIdx - bottomSectionY;
				int capturedIdx = sectionIdx;
				result[targetIdx] = fixBiomes(
						level,
						sectionY -> getClamped(rawBiomes, capturedIdx * BIOME_SECTION_COUNT + sectionY)
				);
			}

			if (overworld) {
				Dynamic<?> bottomBiomes = fixBiomes(
						level,
						sectionY -> getClamped(rawBiomes, sectionY % BLOCKS_PER_SECTION)
				);
				Dynamic<?> topBiomes = fixBiomes(
						level,
						sectionY -> getClamped(rawBiomes, sectionY % BLOCKS_PER_SECTION + UPPER_BIOME_SECTION_OFFSET)
				);

				for (int l = 0; l < 4; l++) {
					result[l] = bottomBiomes;
				}

				for (int l = 20; l < CHUNK_SECTIONS_IN_NEW_CHUNK; l++) {
					result[l] = topBiomes;
				}
			}
		}
		else {
			Arrays.fill(result, fixPalette(level.createList(Stream.of(level.createString(PLAINS_ID)))));
		}

		return result;
	}

	private static int getClamped(int[] is, int index) {
		return is[index] & 0xFF;
	}

	private static Dynamic<?> fixLevel(
			Dynamic<?> level,
			boolean overworld,
			boolean heightAlreadyUpdated,
			boolean atNoiseStatus,
			Supplier<ProtoChunkTickListFix.@Nullable PalettedSection> supplier
	) {
		level = level.remove("Biomes");
		if (!overworld) {
			return fixCarvingMasks(level, CHUNK_SECTIONS_IN_OLD_CHUNK, 0);
		}
		else if (heightAlreadyUpdated) {
			return fixCarvingMasks(level, CHUNK_SECTIONS_IN_NEW_CHUNK, 0);
		}
		else {
			level = fixHeightmaps(level);
			level = fixChunkSectionList(level, "LiquidsToBeTicked");
			level = fixChunkSectionList(level, "PostProcessing");
			level = fixChunkSectionList(level, "ToBeTicked");
			level = fixCarvingMasks(level, CHUNK_SECTIONS_IN_NEW_CHUNK, 4);
			level = level.update("UpgradeData", ChunkHeightAndBiomeFix::fixUpgradeData);

			if (!atNoiseStatus) {
				return level;
			}

			Optional<? extends Dynamic<?>> statusOptional = level.get("Status").result();

			if (statusOptional.isEmpty()) {
				return level;
			}

			Dynamic<?> statusDynamic = (Dynamic<?>) statusOptional.get();
			String statusName = statusDynamic.asString("");

			if ("empty".equals(statusName)) {
				return level;
			}

			level = level.set(
					"blending_data",
					level.createMap(ImmutableMap.of(
							level.createString("old_noise"),
							level.createBoolean(STATUSES_REQUIRING_OLD_NOISE.contains(statusName))
					))
			);

			if (SharedConstants.DISABLE_BELOW_ZERO_RETROGENERATION) {
				return level;
			}

			ProtoChunkTickListFix.PalettedSection groundSection = supplier.get();

			if (groundSection == null) {
				return level;
			}

			BitSet airBitSet = new BitSet(256);
			boolean hasBedrock = statusName.equals("noise");

			for (int x = 0; x < CHUNK_SECTIONS_IN_OLD_CHUNK; x++) {
				for (int z = 0; z < CHUNK_SECTIONS_IN_OLD_CHUNK; z++) {
					Dynamic<?> block = groundSection.get(z, 0, x);
					boolean isBedrock = block != null
							&& "minecraft:bedrock".equals(block.get("Name").asString(""));
					boolean isAir = block != null
							&& "minecraft:air".equals(block.get("Name").asString(""));

					if (isAir) {
						airBitSet.set(x * CHUNK_SECTIONS_IN_OLD_CHUNK + z);
					}

					hasBedrock |= isBedrock;
				}
			}

			if (hasBedrock && airBitSet.cardinality() != airBitSet.size()) {
				Dynamic<?> targetStatus = "full".equals(statusName)
						? level.createString("heightmaps")
						: statusDynamic;
				level = level.set(
						"below_zero_retrogen",
						level.createMap(
								ImmutableMap.of(
										level.createString("target_status"),
										targetStatus,
										level.createString("missing_bedrock"),
										level.createLongList(LongStream.of(airBitSet.toLongArray()))
								)
						)
				);
				level = level.set("Status", level.createString("empty"));
			}

			level = level.set("isLightOn", level.createBoolean(false));

			return level;
		}
	}

	private static <T> Dynamic<T> fixUpgradeData(Dynamic<T> upgradeData) {
		return upgradeData.update(
				"Indices", indices -> {
					Map<Dynamic<?>, Dynamic<?>> remapped = new HashMap<>();
					indices.getMapValues().ifSuccess(indicesMap -> indicesMap.forEach((key, value) -> {
						try {
							key.asString().result().map(Integer::parseInt).ifPresent(oldIndex -> {
								// Смещаем индекс секции на 4 (новый нижний предел -4 вместо 0)
								int newIndex = oldIndex - MIN_CHUNK_SECTION_Y;
								remapped.put(key.createString(Integer.toString(newIndex)), (Dynamic<?>) value);
							});
						}
						catch (NumberFormatException ignored) {
						}
					}));
					return indices.createMap(remapped);
				}
		);
	}

	private static Dynamic<?> fixCarvingMasks(Dynamic<?> level, int sectionsPerChunk, int oldBottomSectionY) {
		Dynamic<?> carvingMasks = level.get("CarvingMasks").orElseEmptyMap();
		carvingMasks = carvingMasks.updateMapValues(mask -> {
			long[] oldBits = BitSet.valueOf(((Dynamic) mask.getSecond()).asByteBuffer().array()).toLongArray();
			long[] newBits = new long[BIOME_ENTRIES_PER_CHUNK_SECTION * sectionsPerChunk];
			System.arraycopy(oldBits, 0, newBits, BIOME_SECTION_COUNT * oldBottomSectionY, oldBits.length);
			return Pair.of((Dynamic) mask.getFirst(), level.createLongList(LongStream.of(newBits)));
		});
		return level.set("CarvingMasks", carvingMasks);
	}

	private static Dynamic<?> fixChunkSectionList(Dynamic<?> level, String key) {
		List<Dynamic<?>>
				list =
				level.get(key).orElseEmptyList().asStream().collect(Collectors.toCollection(ArrayList::new));
		if (list.size() == CHUNK_SECTIONS_IN_NEW_CHUNK) {
			return level;
		}
		else {
			Dynamic<?> dynamic = level.emptyList();

			for (int i = 0; i < 4; i++) {
				list.add(0, dynamic);
				list.add(dynamic);
			}

			return level.set(key, level.createList(list.stream()));
		}
	}

	private static Dynamic<?> fixHeightmaps(Dynamic<?> level) {
		return level.update(
				"Heightmaps", heightmaps -> {
					for (String key : HEIGHTMAP_KEYS) {
						heightmaps = heightmaps.update(key, ChunkHeightAndBiomeFix::fixHeightmap);
					}

					return heightmaps;
				}
		);
	}

	private static Dynamic<?> fixHeightmap(Dynamic<?> heightmap) {
		return heightmap.createLongList(heightmap.asLongStream().map(packedEntry -> {
			long result = 0L;

			for (int bitOffset = 0; bitOffset + BIOME_BITS_PER_ENTRY <= BIOME_SECTION_COUNT; bitOffset += BIOME_BITS_PER_ENTRY) {
				long rawHeight = packedEntry >> bitOffset & BIOME_ENTRY_MASK;
				long shiftedHeight = rawHeight == 0L
						? 0L
						: Math.min(rawHeight + BIOME_SECTION_COUNT, BIOME_ENTRY_MASK);
				result |= shiftedHeight << bitOffset;
			}

			return result;
		}));
	}

	private static Dynamic<?> fixBiomes(Dynamic<?> level, Int2IntFunction biomeGetter) {
		Int2IntMap biomeIndexMap = new Int2IntLinkedOpenHashMap();

		for (int entryIdx = 0; entryIdx < BIOME_SECTION_COUNT; entryIdx++) {
			int rawId = biomeGetter.applyAsInt(entryIdx);

			if (!biomeIndexMap.containsKey(rawId)) {
				biomeIndexMap.put(rawId, biomeIndexMap.size());
			}
		}

		Dynamic<?> palette = level.createList(
				biomeIndexMap
						.keySet()
						.stream()
						.map(rawBiomeId -> level.createString(
								(String) RAW_BIOME_IDS.getOrDefault(rawBiomeId, PLAINS_ID)
						))
		);
		int bitsPerEntry = ceilLog2(biomeIndexMap.size());

		if (bitsPerEntry == 0) {
			return fixPalette(palette);
		}

		int entriesPerLong = 64 / bitsPerEntry;
		int longsCount = (64 + entriesPerLong - 1) / entriesPerLong;
		long[] packedData = new long[longsCount];
		int longIdx = 0;
		int bitOffset = 0;

		for (int entryIdx = 0; entryIdx < BIOME_SECTION_COUNT; entryIdx++) {
			int rawId = biomeGetter.applyAsInt(entryIdx);
			packedData[longIdx] |= (long) biomeIndexMap.get(rawId) << bitOffset;
			bitOffset += bitsPerEntry;

			if (bitOffset + bitsPerEntry > BIOME_SECTION_COUNT) {
				longIdx++;
				bitOffset = 0;
			}
		}

		Dynamic<?> data = level.createLongList(Arrays.stream(packedData));
		return fixPaletteWithData(palette, data);
	}

	private static Dynamic<?> fixPalette(Dynamic<?> palette) {
		return palette.createMap(ImmutableMap.of(palette.createString("palette"), palette));
	}

	private static Dynamic<?> fixPaletteWithData(Dynamic<?> palette, Dynamic<?> data) {
		return palette.createMap(ImmutableMap.of(
				palette.createString("palette"),
				palette,
				palette.createString("data"),
				data
		));
	}

	private static Dynamic<?> fixPalette(Dynamic<?> dynamic, Dynamic<?> dynamic2) {
		List<Dynamic<?>> list = dynamic.asStream().collect(Collectors.toCollection(ArrayList::new));
		if (list.size() == 1) {
			return fixPalette(dynamic);
		}
		else {
			dynamic = expandPaletteToFitData(dynamic, dynamic2, list);
			return fixPaletteWithData(dynamic, dynamic2);
		}
	}

	private static Dynamic<?> expandPaletteToFitData(Dynamic<?> palette, Dynamic<?> data, List<Dynamic<?>> entries) {
		long totalBits = data.asLongStream().count() * 64L;
		long bitsPerEntry = totalBits / 4096L;
		int currentSize = entries.size();
		int requiredBits = ceilLog2(currentSize);

		if (bitsPerEntry <= requiredBits) {
			return palette;
		}

		Dynamic<?> airEntry = palette.createMap(ImmutableMap.of(
				palette.createString("Name"),
				palette.createString("minecraft:air")
		));
		int targetSize = (1 << (int) (bitsPerEntry - 1L)) + 1;
		int entriesToAdd = targetSize - currentSize;

		for (int i = 0; i < entriesToAdd; i++) {
			entries.add(airEntry);
		}

		return palette.createList(entries.stream());
	}

	public static int ceilLog2(int value) {
		return value == 0 ? 0 : (int) Math.ceil(Math.log(value) / Math.log(2.0));
	}

	static {
		RAW_BIOME_IDS.put(0, "minecraft:ocean");
		RAW_BIOME_IDS.put(1, "minecraft:plains");
		RAW_BIOME_IDS.put(2, "minecraft:desert");
		RAW_BIOME_IDS.put(3, "minecraft:mountains");
		RAW_BIOME_IDS.put(4, "minecraft:forest");
		RAW_BIOME_IDS.put(5, "minecraft:taiga");
		RAW_BIOME_IDS.put(6, "minecraft:swamp");
		RAW_BIOME_IDS.put(7, "minecraft:river");
		RAW_BIOME_IDS.put(8, "minecraft:nether_wastes");
		RAW_BIOME_IDS.put(9, "minecraft:the_end");
		RAW_BIOME_IDS.put(10, "minecraft:frozen_ocean");
		RAW_BIOME_IDS.put(11, "minecraft:frozen_river");
		RAW_BIOME_IDS.put(12, "minecraft:snowy_tundra");
		RAW_BIOME_IDS.put(13, "minecraft:snowy_mountains");
		RAW_BIOME_IDS.put(14, "minecraft:mushroom_fields");
		RAW_BIOME_IDS.put(15, "minecraft:mushroom_field_shore");
		RAW_BIOME_IDS.put(16, "minecraft:beach");
		RAW_BIOME_IDS.put(17, "minecraft:desert_hills");
		RAW_BIOME_IDS.put(18, "minecraft:wooded_hills");
		RAW_BIOME_IDS.put(19, "minecraft:taiga_hills");
		RAW_BIOME_IDS.put(20, "minecraft:mountain_edge");
		RAW_BIOME_IDS.put(21, "minecraft:jungle");
		RAW_BIOME_IDS.put(22, "minecraft:jungle_hills");
		RAW_BIOME_IDS.put(23, "minecraft:jungle_edge");
		RAW_BIOME_IDS.put(DEEP_OCEAN_BIOME_ID, "minecraft:deep_ocean");
		RAW_BIOME_IDS.put(25, "minecraft:stone_shore");
		RAW_BIOME_IDS.put(26, "minecraft:snowy_beach");
		RAW_BIOME_IDS.put(27, "minecraft:birch_forest");
		RAW_BIOME_IDS.put(28, "minecraft:birch_forest_hills");
		RAW_BIOME_IDS.put(29, "minecraft:dark_forest");
		RAW_BIOME_IDS.put(30, "minecraft:snowy_taiga");
		RAW_BIOME_IDS.put(31, "minecraft:snowy_taiga_hills");
		RAW_BIOME_IDS.put(32, "minecraft:giant_tree_taiga");
		RAW_BIOME_IDS.put(33, "minecraft:giant_tree_taiga_hills");
		RAW_BIOME_IDS.put(34, "minecraft:wooded_mountains");
		RAW_BIOME_IDS.put(35, "minecraft:savanna");
		RAW_BIOME_IDS.put(36, "minecraft:savanna_plateau");
		RAW_BIOME_IDS.put(37, "minecraft:badlands");
		RAW_BIOME_IDS.put(38, "minecraft:wooded_badlands_plateau");
		RAW_BIOME_IDS.put(39, "minecraft:badlands_plateau");
		RAW_BIOME_IDS.put(40, "minecraft:small_end_islands");
		RAW_BIOME_IDS.put(41, "minecraft:end_midlands");
		RAW_BIOME_IDS.put(42, "minecraft:end_highlands");
		RAW_BIOME_IDS.put(43, "minecraft:end_barrens");
		RAW_BIOME_IDS.put(44, "minecraft:warm_ocean");
		RAW_BIOME_IDS.put(45, "minecraft:lukewarm_ocean");
		RAW_BIOME_IDS.put(46, "minecraft:cold_ocean");
		RAW_BIOME_IDS.put(47, "minecraft:deep_warm_ocean");
		RAW_BIOME_IDS.put(48, "minecraft:deep_lukewarm_ocean");
		RAW_BIOME_IDS.put(49, "minecraft:deep_cold_ocean");
		RAW_BIOME_IDS.put(50, "minecraft:deep_frozen_ocean");
		RAW_BIOME_IDS.put(127, "minecraft:the_void");
		RAW_BIOME_IDS.put(129, "minecraft:sunflower_plains");
		RAW_BIOME_IDS.put(130, "minecraft:desert_lakes");
		RAW_BIOME_IDS.put(131, "minecraft:gravelly_mountains");
		RAW_BIOME_IDS.put(132, "minecraft:flower_forest");
		RAW_BIOME_IDS.put(133, "minecraft:taiga_mountains");
		RAW_BIOME_IDS.put(134, "minecraft:swamp_hills");
		RAW_BIOME_IDS.put(140, "minecraft:ice_spikes");
		RAW_BIOME_IDS.put(149, "minecraft:modified_jungle");
		RAW_BIOME_IDS.put(151, "minecraft:modified_jungle_edge");
		RAW_BIOME_IDS.put(155, "minecraft:tall_birch_forest");
		RAW_BIOME_IDS.put(156, "minecraft:tall_birch_hills");
		RAW_BIOME_IDS.put(157, "minecraft:dark_forest_hills");
		RAW_BIOME_IDS.put(158, "minecraft:snowy_taiga_mountains");
		RAW_BIOME_IDS.put(160, "minecraft:giant_spruce_taiga");
		RAW_BIOME_IDS.put(161, "minecraft:giant_spruce_taiga_hills");
		RAW_BIOME_IDS.put(162, "minecraft:modified_gravelly_mountains");
		RAW_BIOME_IDS.put(163, "minecraft:shattered_savanna");
		RAW_BIOME_IDS.put(164, "minecraft:shattered_savanna_plateau");
		RAW_BIOME_IDS.put(165, "minecraft:eroded_badlands");
		RAW_BIOME_IDS.put(166, "minecraft:modified_wooded_badlands_plateau");
		RAW_BIOME_IDS.put(167, "minecraft:modified_badlands_plateau");
		RAW_BIOME_IDS.put(168, "minecraft:bamboo_jungle");
		RAW_BIOME_IDS.put(169, "minecraft:bamboo_jungle_hills");
		RAW_BIOME_IDS.put(170, "minecraft:soul_sand_valley");
		RAW_BIOME_IDS.put(171, "minecraft:crimson_forest");
		RAW_BIOME_IDS.put(172, "minecraft:warped_forest");
		RAW_BIOME_IDS.put(173, "minecraft:basalt_deltas");
		RAW_BIOME_IDS.put(174, "minecraft:dripstone_caves");
		RAW_BIOME_IDS.put(175, "minecraft:lush_caves");
		RAW_BIOME_IDS.put(177, "minecraft:meadow");
		RAW_BIOME_IDS.put(178, "minecraft:grove");
		RAW_BIOME_IDS.put(179, "minecraft:snowy_slopes");
		RAW_BIOME_IDS.put(180, "minecraft:snowcapped_peaks");
		RAW_BIOME_IDS.put(181, "minecraft:lofty_peaks");
		RAW_BIOME_IDS.put(182, "minecraft:stony_peaks");
	}
}
