package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.datafixer.TypeReferences;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Конвертирует старый формат чанка в формат протo-чанка: определяет статус генерации
 * по флагам {@code TerrainPopulated}/{@code LightPopulated}, конвертирует биомы из байтового
 * массива в int-массив и преобразует {@code TileTicks} в {@code ToBeTicked} по секциям.
 */
public class ChunkToProtoChunkFix extends DataFix {

	private static final int SECTION_COUNT = 16;
	private static final int BIOME_ARRAY_SIZE = 256;
	private static final int BYTE_MASK = 0xFF;
	private static final int SECTION_Y_SHIFT = 4;
	private static final int LOCAL_COORD_MASK = 15;

	public ChunkToProtoChunkFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	public TypeRewriteRule makeRule() {
		return writeFixAndRead(
				"ChunkToProtoChunkFix",
				getInputSchema().getType(TypeReferences.CHUNK),
				getOutputSchema().getType(TypeReferences.CHUNK),
				chunkDynamic -> chunkDynamic.update("Level", ChunkToProtoChunkFix::fixLevel)
		);
	}

	private static <T> Dynamic<T> fixLevel(Dynamic<T> level) {
		boolean terrainPopulated = level.get("TerrainPopulated").asBoolean(false);
		boolean lightPopulated = level.get("LightPopulated").asNumber().result().isEmpty()
				|| level.get("LightPopulated").asBoolean(false);

		String status;
		if (terrainPopulated) {
			status = lightPopulated ? "mobs_spawned" : "decorated";
		}
		else {
			status = "carved";
		}

		return fixTileTicks(fixBiomes(level))
				.set("Status", level.createString(status))
				.set("hasLegacyStructureData", level.createBoolean(true));
	}

	private static <T> Dynamic<T> fixBiomes(Dynamic<T> level) {
		return level.update(
				"Biomes",
				biomes -> (Dynamic) DataFixUtils.orElse(
						biomes.asByteBufferOpt().result().map(byteBuffer -> {
							int[] biomeIds = new int[BIOME_ARRAY_SIZE];

							for (int i = 0; i < biomeIds.length; i++) {
								if (i < byteBuffer.capacity()) {
									biomeIds[i] = byteBuffer.get(i) & BYTE_MASK;
								}
							}

							return level.createIntList(Arrays.stream(biomeIds));
						}),
						biomes
				)
		);
	}

	private static <T> Dynamic<T> fixTileTicks(Dynamic<T> level) {
		return (Dynamic<T>) DataFixUtils.orElse(
				level.get("TileTicks")
						.asStreamOpt()
						.result()
						.map(tileTicks -> {
							List<ShortList> ticksBySection = IntStream
									.range(0, SECTION_COUNT)
									.mapToObj(sectionY -> (ShortList) new ShortArrayList())
									.collect(Collectors.toList());

							tileTicks.forEach(tick -> {
								int x = tick.get("x").asInt(0);
								int y = tick.get("y").asInt(0);
								int z = tick.get("z").asInt(0);
								ticksBySection.get(y >> SECTION_Y_SHIFT).add(packChunkSectionPos(x, y, z));
							});

							return level.remove("TileTicks")
									.set(
											"ToBeTicked",
											level.createList(
													ticksBySection.stream().map(
															section -> level.createList(
																	section.intStream().mapToObj(
																			packed -> level.createShort((short) packed)
																	)
															)
													)
											)
									);
						}),
				level
		);
	}

	private static short packChunkSectionPos(int x, int y, int z) {
		return (short) (x & LOCAL_COORD_MASK | (y & LOCAL_COORD_MASK) << 4 | (z & LOCAL_COORD_MASK) << 8);
	}
}
