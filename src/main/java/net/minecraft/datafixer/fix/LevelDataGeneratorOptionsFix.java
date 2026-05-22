package net.minecraft.datafixer.fix;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Исправляет данные в формате DataFixer.
 */
public class LevelDataGeneratorOptionsFix extends DataFix {

	static final Map<String, String> NUMERICAL_IDS_TO_BIOME_IDS = Map.ofEntries(
			Map.entry("0", "minecraft:ocean"),
			Map.entry("1", "minecraft:plains"),
			Map.entry("2", "minecraft:desert"),
			Map.entry("3", "minecraft:mountains"),
			Map.entry("4", "minecraft:forest"),
			Map.entry("5", "minecraft:taiga"),
			Map.entry("6", "minecraft:swamp"),
			Map.entry("7", "minecraft:river"),
			Map.entry("8", "minecraft:nether"),
			Map.entry("9", "minecraft:the_end"),
			Map.entry("10", "minecraft:frozen_ocean"),
			Map.entry("11", "minecraft:frozen_river"),
			Map.entry("12", "minecraft:snowy_tundra"),
			Map.entry("13", "minecraft:snowy_mountains"),
			Map.entry("14", "minecraft:mushroom_fields"),
			Map.entry("15", "minecraft:mushroom_field_shore"),
			Map.entry("16", "minecraft:beach"),
			Map.entry("17", "minecraft:desert_hills"),
			Map.entry("18", "minecraft:wooded_hills"),
			Map.entry("19", "minecraft:taiga_hills"),
			Map.entry("20", "minecraft:mountain_edge"),
			Map.entry("21", "minecraft:jungle"),
			Map.entry("22", "minecraft:jungle_hills"),
			Map.entry("23", "minecraft:jungle_edge"),
			Map.entry("24", "minecraft:deep_ocean"),
			Map.entry("25", "minecraft:stone_shore"),
			Map.entry("26", "minecraft:snowy_beach"),
			Map.entry("27", "minecraft:birch_forest"),
			Map.entry("28", "minecraft:birch_forest_hills"),
			Map.entry("29", "minecraft:dark_forest"),
			Map.entry("30", "minecraft:snowy_taiga"),
			Map.entry("31", "minecraft:snowy_taiga_hills"),
			Map.entry("32", "minecraft:giant_tree_taiga"),
			Map.entry("33", "minecraft:giant_tree_taiga_hills"),
			Map.entry("34", "minecraft:wooded_mountains"),
			Map.entry("35", "minecraft:savanna"),
			Map.entry("36", "minecraft:savanna_plateau"),
			Map.entry("37", "minecraft:badlands"),
			Map.entry("38", "minecraft:wooded_badlands_plateau"),
			Map.entry("39", "minecraft:badlands_plateau"),
			Map.entry("40", "minecraft:small_end_islands"),
			Map.entry("41", "minecraft:end_midlands"),
			Map.entry("42", "minecraft:end_highlands"),
			Map.entry("43", "minecraft:end_barrens"),
			Map.entry("44", "minecraft:warm_ocean"),
			Map.entry("45", "minecraft:lukewarm_ocean"),
			Map.entry("46", "minecraft:cold_ocean"),
			Map.entry("47", "minecraft:deep_warm_ocean"),
			Map.entry("48", "minecraft:deep_lukewarm_ocean"),
			Map.entry("49", "minecraft:deep_cold_ocean"),
			Map.entry("50", "minecraft:deep_frozen_ocean"),
			Map.entry("127", "minecraft:the_void"),
			Map.entry("129", "minecraft:sunflower_plains"),
			Map.entry("130", "minecraft:desert_lakes"),
			Map.entry("131", "minecraft:gravelly_mountains"),
			Map.entry("132", "minecraft:flower_forest"),
			Map.entry("133", "minecraft:taiga_mountains"),
			Map.entry("134", "minecraft:swamp_hills"),
			Map.entry("140", "minecraft:ice_spikes"),
			Map.entry("149", "minecraft:modified_jungle"),
			Map.entry("151", "minecraft:modified_jungle_edge"),
			Map.entry("155", "minecraft:tall_birch_forest"),
			Map.entry("156", "minecraft:tall_birch_hills"),
			Map.entry("157", "minecraft:dark_forest_hills"),
			Map.entry("158", "minecraft:snowy_taiga_mountains"),
			Map.entry("160", "minecraft:giant_spruce_taiga"),
			Map.entry("161", "minecraft:giant_spruce_taiga_hills"),
			Map.entry("162", "minecraft:modified_gravelly_mountains"),
			Map.entry("163", "minecraft:shattered_savanna"),
			Map.entry("164", "minecraft:shattered_savanna_plateau"),
			Map.entry("165", "minecraft:eroded_badlands"),
			Map.entry("166", "minecraft:modified_wooded_badlands_plateau"),
			Map.entry("167", "minecraft:modified_badlands_plateau")
	);
	public static final String GENERATOR_OPTIONS_KEY = "generatorOptions";

	public LevelDataGeneratorOptionsFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = getOutputSchema().getType(TypeReferences.LEVEL);
		return fixTypeEverywhereTyped(
				"LevelDataGeneratorOptionsFix",
				getInputSchema().getType(TypeReferences.LEVEL),
				type,
				levelTyped -> Util.apply(
						levelTyped, type, levelDynamic -> {
							Optional<String> optional = levelDynamic.get("generatorOptions").asString().result();
							if ("flat".equalsIgnoreCase(levelDynamic.get("generatorName").asString(""))) {
								String string = optional.orElse("");
								return levelDynamic.set(
										"generatorOptions",
										fixGeneratorOptions(string, levelDynamic.getOps())
								);
							}
							else if ("buffet".equalsIgnoreCase(levelDynamic.get("generatorName").asString(""))
									&& optional.isPresent()) {
								JsonElement jsonElement = LenientJsonParser.parse(optional.get());
								return levelDynamic.set(
										"generatorOptions",
										new Dynamic(JsonOps.INSTANCE, jsonElement).convert(levelDynamic.getOps())
								);
							}
							else {
								return levelDynamic;
							}
						}
				)
		);
	}

	private static <T> Dynamic<T> fixGeneratorOptions(String generatorOptions, DynamicOps<T> levelDynamicOps) {
		Iterator<String> iterator = Splitter.on(';').split(generatorOptions).iterator();
		String string = "minecraft:plains";
		Map<String, Map<String, String>> map = new HashMap<>();
		List<Pair<Integer, String>> list;
		if (!generatorOptions.isEmpty() && iterator.hasNext()) {
			list = parseFlatLayers(iterator.next());
			if (!list.isEmpty()) {
				if (iterator.hasNext()) {
					string = NUMERICAL_IDS_TO_BIOME_IDS.getOrDefault(iterator.next(), "minecraft:plains");
				}

				if (iterator.hasNext()) {
					String[] strings = iterator.next().toLowerCase(Locale.ROOT).split(",");

					for (String string2 : strings) {
						String[] strings2 = string2.split("\\(", 2);
						if (!strings2[0].isEmpty()) {
							map.put(strings2[0], new HashMap<>());
							if (strings2.length > 1 && strings2[1].endsWith(")") && strings2[1].length() > 1) {
								String[] strings3 = strings2[1].substring(0, strings2[1].length() - 1).split(" ");

								for (String string3 : strings3) {
									String[] strings4 = string3.split("=", 2);
									if (strings4.length == 2) {
										map.get(strings2[0]).put(strings4[0], strings4[1]);
									}
								}
							}
						}
					}
				}
				else {
					map.put("village", new HashMap<>());
				}
			}
		}
		else {
			list = Lists.newArrayList();
			list.add(Pair.of(1, "minecraft:bedrock"));
			list.add(Pair.of(2, "minecraft:dirt"));
			list.add(Pair.of(1, "minecraft:grass_block"));
			map.put("village", new HashMap<>());
		}

		T object = (T) levelDynamicOps.createList(
				list.stream()
				    .map(
						    pair -> levelDynamicOps.createMap(
								    ImmutableMap.of(
										    levelDynamicOps.createString("height"),
										    levelDynamicOps.createInt((Integer) pair.getFirst()),
										    levelDynamicOps.createString("block"),
										    levelDynamicOps.createString((String) pair.getSecond())
								    )
						    )
				    )
		);
		T object2 = (T) levelDynamicOps.createMap(
				map.entrySet()
				   .stream()
				   .map(
						   entry -> Pair.of(
								   levelDynamicOps.createString(entry.getKey().toLowerCase(Locale.ROOT)),
								   levelDynamicOps.createMap(
										   entry.getValue()
										        .entrySet()
										        .stream()
										        .map(entryx -> Pair.of(
												        levelDynamicOps.createString((String) entryx.getKey()),
												        levelDynamicOps.createString((String) entryx.getValue())
										        ))
										        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))
								   )
						   )
				   )
				   .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))
		);
		return new Dynamic(
				levelDynamicOps,
				levelDynamicOps.createMap(
						ImmutableMap.of(
								levelDynamicOps.createString("layers"),
								object,
								levelDynamicOps.createString("biome"),
								levelDynamicOps.createString(string),
								levelDynamicOps.createString("structures"),
								object2
						)
				)
		);
	}

	private static @Nullable Pair<Integer, String> parseFlatLayer(String layer) {
		String[] strings = layer.split("\\*", 2);
		int i;
		if (strings.length == 2) {
			try {
				i = Integer.parseInt(strings[0]);
			}
			catch (NumberFormatException var4) {
				return null;
			}
		}
		else {
			i = 1;
		}

		String string = strings[strings.length - 1];
		return Pair.of(i, string);
	}

	private static List<Pair<Integer, String>> parseFlatLayers(String layers) {
		List<Pair<Integer, String>> list = Lists.newArrayList();
		String[] strings = layers.split(",");

		for (String string : strings) {
			Pair<Integer, String> pair = parseFlatLayer(string);
			if (pair == null) {
				return Collections.emptyList();
			}

			list.add(pair);
		}

		return list;
	}
}
