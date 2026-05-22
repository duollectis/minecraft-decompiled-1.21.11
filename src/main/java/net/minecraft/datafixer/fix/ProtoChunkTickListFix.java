package net.minecraft.datafixer.fix;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.datafixer.TypeReferences;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Конвертирует устаревшие списки тиков блоков ({@code ToBeTicked}, {@code LiquidsToBeTicked})
 * в новый формат {@code block_ticks} / {@code fluid_ticks}, используя паллетизированные
 * данные секций для восстановления идентификаторов блоков по локальным позициям.
 */
public class ProtoChunkTickListFix extends DataFix {

	private static final int CHUNK_EDGE_LENGTH = 16;
	private static final ImmutableSet<String> ALWAYS_WATERLOGGED_BLOCK_IDS = ImmutableSet.of(
			"minecraft:bubble_column",
			"minecraft:kelp",
			"minecraft:kelp_plant",
			"minecraft:seagrass",
			"minecraft:tall_seagrass"
	);

	public ProtoChunkTickListFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = chunkType.findField("Level");
		OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
		OpticFinder<?> sectionFinder = ((ListType) sectionsFinder.type()).getElement().finder();
		OpticFinder<?> blockStatesFinder = sectionFinder.type().findField("block_states");
		OpticFinder<?> biomesFinder = sectionFinder.type().findField("biomes");
		OpticFinder<?> paletteFinder = blockStatesFinder.type().findField("palette");
		OpticFinder<?> tileTicksFinder = levelFinder.type().findField("TileTicks");

		return fixTypeEverywhereTyped(
				"ChunkProtoTickListFix",
				chunkType,
				chunkTyped -> chunkTyped.updateTyped(
						levelFinder,
						levelTyped -> {
							levelTyped = levelTyped.update(
									DSL.remainderFinder(),
									levelDynamic -> (Dynamic<?>) DataFixUtils.orElse(
											levelDynamic.get("LiquidTicks")
											            .result()
											            .map(liquidTicks -> levelDynamic
													            .set("fluid_ticks", liquidTicks)
													            .remove("LiquidTicks")),
											levelDynamic
									)
							);

							Dynamic<?> levelDynamic = (Dynamic<?>) levelTyped.get(DSL.remainderFinder());
							MutableInt minSectionY = new MutableInt();
							Int2ObjectMap<Supplier<PalettedSection>> sectionsByY = new Int2ObjectArrayMap<>();

							levelTyped.getOptionalTyped(sectionsFinder)
							          .ifPresent(sectionsTyped -> sectionsTyped
									          .getAllTyped(sectionFinder)
									          .forEach(sectionTyped -> {
										          Dynamic<?> sectionDynamic = (Dynamic<?>) sectionTyped.get(
												          DSL.remainderFinder()
										          );
										          int sectionY = sectionDynamic.get("Y").asInt(Integer.MAX_VALUE);

										          if (sectionY == Integer.MAX_VALUE) {
											          return;
										          }

										          if (sectionTyped.getOptionalTyped(biomesFinder).isPresent()) {
											          minSectionY.setValue(
													          Math.min(sectionY, minSectionY.intValue())
											          );
										          }

										          sectionTyped.getOptionalTyped(blockStatesFinder)
										                      .ifPresent(blockStatesTyped -> sectionsByY.put(
												                      sectionY,
												                      Suppliers.memoize(() -> {
													                      List<? extends Dynamic<?>> palette =
															                      blockStatesTyped
																		                      .getOptionalTyped(paletteFinder)
																		                      .map(paletteTyped -> paletteTyped
																				                      .write()
																				                      .result()
																				                      .map(d -> d.asList(Function.identity()))
																				                      .orElse(Collections.emptyList())
																		                      )
																		                      .orElse(Collections.emptyList());
													                      long[] data = ((Dynamic<?>) blockStatesTyped.get(
															                      DSL.remainderFinder()
													                      ))
															                      .get("data")
															                      .asLongStream()
															                      .toArray();
													                      return new PalettedSection(palette, data);
												                      })
										                      ));
									          })
							          );

							byte bottomSectionY = minSectionY.byteValue();
							levelTyped = levelTyped.update(
									DSL.remainderFinder(),
									d -> d.update("yPos", yPos -> yPos.createByte(bottomSectionY))
							);

							boolean hasTileTicks = levelTyped.getOptionalTyped(tileTicksFinder).isPresent();
							boolean hasFluidTicks = levelDynamic.get("fluid_ticks").result().isPresent();

							if (hasTileTicks || hasFluidTicks) {
								return levelTyped;
							}

							int chunkX = levelDynamic.get("xPos").asInt(0);
							int chunkZ = levelDynamic.get("zPos").asInt(0);
							Dynamic<?> fluidTicks = fixToBeTicked(
									levelDynamic,
									sectionsByY,
									bottomSectionY,
									chunkX,
									chunkZ,
									"LiquidsToBeTicked",
									ProtoChunkTickListFix::getFluidBlockIdToBeTicked
							);
							Dynamic<?> blockTicks = fixToBeTicked(
									levelDynamic,
									sectionsByY,
									bottomSectionY,
									chunkX,
									chunkZ,
									"ToBeTicked",
									ProtoChunkTickListFix::getBlockIdToBeTicked
							);
							Optional<? extends Pair<? extends Typed<?>, ?>> tileTicksOptional =
									tileTicksFinder.type().readTyped(blockTicks).result();

							if (tileTicksOptional.isPresent()) {
								levelTyped = levelTyped.set(
										tileTicksFinder,
										(Typed<?>) tileTicksOptional.get().getFirst()
								);
							}

							return levelTyped.update(
									DSL.remainderFinder(),
									d -> d.remove("ToBeTicked")
									      .remove("LiquidsToBeTicked")
									      .set("fluid_ticks", fluidTicks)
							);
						}
				)
		);
	}

	private Dynamic<?> fixToBeTicked(
			Dynamic<?> levelDynamic,
			Int2ObjectMap<Supplier<PalettedSection>> sectionsByY,
			byte bottomSectionY,
			int chunkX,
			int chunkZ,
			String key,
			Function<Dynamic<?>, String> blockIdGetter
	) {
		Stream<Dynamic<?>> tickStream = Stream.empty();
		List<? extends Dynamic<?>> sectionTickLists = levelDynamic.get(key).asList(Function.identity());

		for (int listIdx = 0; listIdx < sectionTickLists.size(); listIdx++) {
			int absoluteSectionY = listIdx + bottomSectionY;
			Supplier<PalettedSection> sectionSupplier = sectionsByY.get(absoluteSectionY);
			Stream<? extends Dynamic<?>> sectionTicks = sectionTickLists.get(listIdx)
			                                                             .asStream()
			                                                             .mapToInt(d -> d.asShort((short) -1))
			                                                             .filter(packedPos -> packedPos > 0)
			                                                             .mapToObj(packedPos -> createTileTickObject(
					                                                             levelDynamic,
					                                                             sectionSupplier,
					                                                             chunkX,
					                                                             absoluteSectionY,
					                                                             chunkZ,
					                                                             packedPos,
					                                                             blockIdGetter
			                                                             ));
			tickStream = Stream.concat(tickStream, sectionTicks);
		}

		return levelDynamic.createList(tickStream);
	}

	private static String getBlockIdToBeTicked(@Nullable Dynamic<?> blockState) {
		return blockState != null ? blockState.get("Name").asString("minecraft:air") : "minecraft:air";
	}

	private static String getFluidBlockIdToBeTicked(@Nullable Dynamic<?> blockState) {
		if (blockState == null) {
			return "minecraft:empty";
		}

		String blockName = blockState.get("Name").asString("");

		if ("minecraft:water".equals(blockName)) {
			return blockState.get("Properties").get("level").asInt(0) == 0
					? "minecraft:water"
					: "minecraft:flowing_water";
		}

		if ("minecraft:lava".equals(blockName)) {
			return blockState.get("Properties").get("level").asInt(0) == 0
					? "minecraft:lava"
					: "minecraft:flowing_lava";
		}

		boolean isAlwaysWaterlogged = ALWAYS_WATERLOGGED_BLOCK_IDS.contains(blockName);
		boolean isWaterlogged = blockState.get("Properties").get("waterlogged").asBoolean(false);

		return isAlwaysWaterlogged || isWaterlogged ? "minecraft:water" : "minecraft:empty";
	}

	private Dynamic<?> createTileTickObject(
			Dynamic<?> levelDynamic,
			@Nullable Supplier<PalettedSection> sectionSupplier,
			int sectionX,
			int sectionY,
			int sectionZ,
			int packedLocalPos,
			Function<Dynamic<?>, String> blockIdGetter
	) {
		int localX = packedLocalPos & 15;
		int localY = packedLocalPos >>> 4 & 15;
		int localZ = packedLocalPos >>> 8 & 15;
		String blockId = blockIdGetter.apply(
				sectionSupplier != null ? sectionSupplier.get().get(localX, localY, localZ) : null
		);

		return levelDynamic.createMap(
				ImmutableMap.<Dynamic<?>, Dynamic<?>>builder()
				            .put(levelDynamic.createString("i"), levelDynamic.createString(blockId))
				            .put(levelDynamic.createString("x"), levelDynamic.createInt(sectionX * CHUNK_EDGE_LENGTH + localX))
				            .put(levelDynamic.createString("y"), levelDynamic.createInt(sectionY * CHUNK_EDGE_LENGTH + localY))
				            .put(levelDynamic.createString("z"), levelDynamic.createInt(sectionZ * CHUNK_EDGE_LENGTH + localZ))
				            .put(levelDynamic.createString("t"), levelDynamic.createInt(0))
				            .put(levelDynamic.createString("p"), levelDynamic.createInt(0))
				            .build()
		);
	}

	/**
	 * Паллетизированная секция блоков чанка: хранит палитру блок-стейтов и упакованный
	 * массив индексов для быстрого доступа к блоку по локальным координатам.
	 */
	public static final class PalettedSection {

		private static final long MIN_UNIT_SIZE = 4L;
		private final List<? extends Dynamic<?>> palette;
		private final long[] data;
		private final int unitSize;
		private final long unitMask;
		private final int unitsPerLong;

		public PalettedSection(List<? extends Dynamic<?>> palette, long[] data) {
			this.palette = palette;
			this.data = data;
			this.unitSize = Math.max(4, ChunkHeightAndBiomeFix.ceilLog2(palette.size()));
			this.unitMask = (1L << this.unitSize) - 1L;
			this.unitsPerLong = (char) (64 / this.unitSize);
		}

		public @Nullable Dynamic<?> get(int localX, int localY, int localZ) {
			int paletteSize = palette.size();

			if (paletteSize < 1) {
				return null;
			}

			if (paletteSize == 1) {
				return palette.getFirst();
			}

			int packedPos = packLocalPos(localX, localY, localZ);
			int longIndex = packedPos / unitsPerLong;

			if (longIndex < 0 || longIndex >= data.length) {
				return null;
			}

			long packedLong = data[longIndex];
			int bitOffset = (packedPos - longIndex * unitsPerLong) * unitSize;
			int paletteIndex = (int) (packedLong >> bitOffset & unitMask);

			return paletteIndex >= 0 && paletteIndex < paletteSize ? palette.get(paletteIndex) : null;
		}

		private int packLocalPos(int localX, int localY, int localZ) {
			return (localY << 4 | localZ) << 4 | localX;
		}

		public List<? extends Dynamic<?>> getPalette() {
			return Collections.unmodifiableList(palette);
		}

		public long[] getData() {
			return data.clone();
		}
	}
}
