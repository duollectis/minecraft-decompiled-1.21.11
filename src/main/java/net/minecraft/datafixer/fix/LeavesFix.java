package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.WordPackedArray;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Мигрирует данные листвы (leaves) в чанках из старого формата блок-состояний в новый.
 * <p>
 * Вычисляет расстояние от каждого листового блока до ближайшего бревна (log) методом BFS
 * по 6 осям и проставляет свойства {@code distance} и {@code persistent}. Также обновляет
 * флаги {@code UpgradeData.Sides} для граничных чанков, требующих повторной обработки.
 */
public class LeavesFix extends DataFix {

	private static final int SIDE_FLAG_NORTH_WEST = 128;
	private static final int SIDE_FLAG_WEST = 64;
	private static final int SIDE_FLAG_SOUTH_WEST = 32;
	private static final int SIDE_FLAG_SOUTH = 16;
	private static final int SIDE_FLAG_SOUTH_EAST = 8;
	private static final int SIDE_FLAG_EAST = 4;
	private static final int SIDE_FLAG_NORTH_EAST = 2;
	private static final int SIDE_FLAG_NORTH = 1;
	private static final int[][]
			AXIAL_OFFSETS =
			new int[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
	private static final int DISTANCE_MASK = 7;
	private static final int BITS_PER_BLOCK = 12;
	private static final int BLOCKS_PER_SECTION = 4096;
	static final Object2IntMap<String> LEAVES_MAP = buildLeavesMap();
	static final Set<String> LOGS_MAP = ImmutableSet.of(
			"minecraft:acacia_bark",
			"minecraft:birch_bark",
			"minecraft:dark_oak_bark",
			"minecraft:jungle_bark",
			"minecraft:oak_bark",
			"minecraft:spruce_bark",
			new String[]{
					"minecraft:acacia_log",
					"minecraft:birch_log",
					"minecraft:dark_oak_log",
					"minecraft:jungle_log",
					"minecraft:oak_log",
					"minecraft:spruce_log",
					"minecraft:stripped_acacia_log",
					"minecraft:stripped_birch_log",
					"minecraft:stripped_dark_oak_log",
					"minecraft:stripped_jungle_log",
					"minecraft:stripped_oak_log",
					"minecraft:stripped_spruce_log"
			}
	);

	public LeavesFix(Schema schema, boolean outputChanges) {
		super(schema, outputChanges);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = chunkType.findField("Level");
		OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
		Type<?> sectionsType = sectionsFinder.type();
		if (!(sectionsType instanceof ListType)) {
			throw new IllegalStateException("Expecting sections to be a list.");
		}

		Type<?> sectionElementType = ((ListType) sectionsType).getElement();
		OpticFinder<?> sectionFinder = DSL.typeFinder(sectionElementType);
		return fixTypeEverywhereTyped(
				"Leaves fix",
				chunkType,
				chunkTyped -> chunkTyped.updateTyped(
						levelFinder,
						levelTyped -> {
							int[] boundaryFlags = new int[]{0};
							Typed<?> typed = levelTyped.updateTyped(
									sectionsFinder,
									sectionsTyped -> {
										Int2ObjectMap<LeavesFix.LeavesLogFixer> fixersByY =
												new Int2ObjectOpenHashMap<>(
														sectionsTyped.getAllTyped(sectionFinder)
														             .stream()
														             .map(sectionTyped -> new LeavesFix.LeavesLogFixer(
																             (Typed<?>) sectionTyped,
																             getInputSchema()
														             ))
														             .collect(Collectors.toMap(
																             LeavesFix.ListFixer::getY,
																             fixer -> (LeavesFix.LeavesLogFixer) fixer
														             ))
												);
										if (fixersByY.values().stream().allMatch(LeavesFix.ListFixer::isFixed)) {
											return sectionsTyped;
										}

										List<IntSet> distanceSets = Lists.newArrayList();
										for (int dist = 0; dist < 7; dist++) {
											distanceSets.add(new IntOpenHashSet());
										}

										for (LeavesFix.LeavesLogFixer fixer : fixersByY.values()) {
											if (fixer.isFixed()) {
												continue;
											}

											for (int blockIndex = 0; blockIndex < BLOCKS_PER_SECTION; blockIndex++) {
												int stateIndex = fixer.blockStateAt(blockIndex);
												if (fixer.isLog(stateIndex)) {
													distanceSets.get(0).add(fixer.getY() << BITS_PER_BLOCK | blockIndex);
												}
												else if (fixer.isLeaf(stateIndex)) {
													int localX = this.getX(blockIndex);
													int localZ = this.getZ(blockIndex);
													boundaryFlags[0] |= getBoundaryClassBit(
															localX == 0,
															localX == 15,
															localZ == 0,
															localZ == 15
													);
												}
											}
										}

										for (int dist = 1; dist < 7; dist++) {
											IntSet currentSet = distanceSets.get(dist - 1);
											IntSet nextSet = distanceSets.get(dist);
											IntIterator iterator = currentSet.iterator();

											while (iterator.hasNext()) {
												int packed = iterator.nextInt();
												int localX = this.getX(packed);
												int globalY = this.getY(packed);
												int localZ = this.getZ(packed);

												for (int[] offset : AXIAL_OFFSETS) {
													int nx = localX + offset[0];
													int ny = globalY + offset[1];
													int nz = localZ + offset[2];
													if (nx >= 0 && nx <= 15 && nz >= 0 && nz <= 15 && ny >= 0 && ny <= 255) {
														LeavesFix.LeavesLogFixer neighborFixer =
																(LeavesFix.LeavesLogFixer) fixersByY.get(ny >> 4);
														if (neighborFixer == null || neighborFixer.isFixed()) {
															continue;
														}

														int neighborPos = packLocalPos(nx, ny & 15, nz);
														int neighborState = neighborFixer.blockStateAt(neighborPos);
														if (neighborFixer.isLeaf(neighborState)
																&& neighborFixer.getDistanceToLog(neighborState) > dist) {
															neighborFixer.computeLeafStates(neighborPos, neighborState, dist);
															nextSet.add(packLocalPos(nx, ny, nz));
														}
													}
												}
											}
										}

										return sectionsTyped.updateTyped(
												sectionFinder,
												sectionTyped -> fixersByY.get(
														((Dynamic<?>) sectionTyped.get(DSL.remainderFinder()))
																.get("Y")
																.asInt(0)
												).finalizeFix(sectionTyped)
										);
									}
							);
							if (boundaryFlags[0] != 0) {
								typed = typed.update(
										DSL.remainderFinder(), dynamic -> {
											Dynamic<?> upgradeData = (Dynamic<?>) DataFixUtils.orElse(
													dynamic.get("UpgradeData").result(),
													dynamic.emptyMap()
											);
											return dynamic.set(
													"UpgradeData",
													upgradeData.set(
															"Sides",
															dynamic.createByte((byte) (
																	upgradeData.get("Sides").asByte((byte) 0) | boundaryFlags[0]
															))
													)
											);
										}
								);
							}

							return typed;
						}
				)
		);
	}

	public static int packLocalPos(int localX, int localY, int localZ) {
		return localY << 8 | localZ << 4 | localX;
	}

	private int getX(int packedLocalPos) {
		return packedLocalPos & 15;
	}

	private int getY(int packedLocalPos) {
		return packedLocalPos >> 8 & 0xFF;
	}

	private int getZ(int packedLocalPos) {
		return packedLocalPos >> 4 & 15;
	}

	public static int getBoundaryClassBit(
			boolean westernmost,
			boolean easternmost,
			boolean northernmost,
			boolean southernmost
	) {
		int flags = 0;
		if (northernmost) {
			if (easternmost) {
				flags |= SIDE_FLAG_NORTH_EAST;
			}
			else if (westernmost) {
				flags |= SIDE_FLAG_NORTH_WEST;
			}
			else {
				flags |= SIDE_FLAG_NORTH;
			}
		}
		else if (southernmost) {
			if (westernmost) {
				flags |= SIDE_FLAG_SOUTH_WEST;
			}
			else if (easternmost) {
				flags |= SIDE_FLAG_SOUTH_EAST;
			}
			else {
				flags |= SIDE_FLAG_SOUTH;
			}
		}
		else if (easternmost) {
			flags |= SIDE_FLAG_EAST;
		}
		else if (westernmost) {
			flags |= SIDE_FLAG_WEST;
		}

		return flags;
	}

	/**
	 * Вычисляет и обновляет состояния листовых блоков в одной секции чанка,
	 * проставляя корректные значения {@code distance} и {@code persistent}.
	 */
	public static final class LeavesLogFixer extends LeavesFix.ListFixer {

		private static final String PERSISTENT = "persistent";
		private static final String DECAYABLE = "decayable";
		private static final String DISTANCE = "distance";
		private @Nullable IntSet leafIndices;
		private @Nullable IntSet logIndices;
		private @Nullable Int2IntMap leafStates;

		public LeavesLogFixer(Typed<?> typed, Schema schema) {
			super(typed, schema);
		}

		@Override
		protected boolean computeIsFixed() {
			this.leafIndices = new IntOpenHashSet();
			this.logIndices = new IntOpenHashSet();
			this.leafStates = new Int2IntOpenHashMap();

			for (int i = 0; i < this.properties.size(); i++) {
				Dynamic<?> dynamic = this.properties.get(i);
				String string = dynamic.get("Name").asString("");
				if (LeavesFix.LEAVES_MAP.containsKey(string)) {
					boolean bl = Objects.equals(dynamic.get("Properties").get("decayable").asString(""), "false");
					this.leafIndices.add(i);
					this.leafStates.put(this.computeFlags(string, bl, 7), i);
					this.properties.set(i, this.createLeafProperties(dynamic, string, bl, 7));
				}

				if (LeavesFix.LOGS_MAP.contains(string)) {
					this.logIndices.add(i);
				}
			}

			return this.leafIndices.isEmpty() && this.logIndices.isEmpty();
		}

		private Dynamic<?> createLeafProperties(Dynamic<?> tag, String name, boolean persistent, int distance) {
			Dynamic<?> dynamic = tag.emptyMap();
			dynamic = dynamic.set("persistent", dynamic.createString(persistent ? "true" : "false"));
			dynamic = dynamic.set("distance", dynamic.createString(Integer.toString(distance)));
			Dynamic<?> dynamic2 = tag.emptyMap();
			dynamic2 = dynamic2.set("Properties", dynamic);
			return dynamic2.set("Name", dynamic2.createString(name));
		}

		public boolean isLog(int index) {
			return this.logIndices.contains(index);
		}

		public boolean isLeaf(int index) {
			return this.leafIndices.contains(index);
		}

		int getDistanceToLog(int index) {
			return this.isLog(index) ? 0 : Integer.parseInt(this.properties
			                                                .get(index)
			                                                .get("Properties")
			                                                .get("distance")
			                                                .asString(""));
		}

		void computeLeafStates(int packedLocalPos, int propertyIndex, int distance) {
			Dynamic<?> dynamic = this.properties.get(propertyIndex);
			String string = dynamic.get("Name").asString("");
			boolean bl = Objects.equals(dynamic.get("Properties").get("persistent").asString(""), "true");
			int i = this.computeFlags(string, bl, distance);
			if (!this.leafStates.containsKey(i)) {
				int j = this.properties.size();
				this.leafIndices.add(j);
				this.leafStates.put(i, j);
				this.properties.add(this.createLeafProperties(dynamic, string, bl, distance));
			}

			int j = this.leafStates.get(i);
			if (1 << this.blockStateMap.getUnitSize() <= j) {
				WordPackedArray wordPackedArray = new WordPackedArray(this.blockStateMap.getUnitSize() + 1, BLOCKS_PER_SECTION);

				for (int k = 0; k < BLOCKS_PER_SECTION; k++) {
					wordPackedArray.set(k, this.blockStateMap.get(k));
				}

				this.blockStateMap = wordPackedArray;
			}

			this.blockStateMap.set(packedLocalPos, j);
		}
	}

	/**
	 * Базовый класс для исправления палитры блок-состояний в одной секции чанка (16×16×16 блоков).
	 * Загружает палитру и упакованный массив {@code BlockStates} из NBT и предоставляет
	 * методы для чтения и записи индексов состояний.
	 */
	public abstract static class ListFixer {

		protected static final String BLOCK_STATES_KEY = "BlockStates";
		protected static final String NAME_KEY = "Name";
		protected static final String PROPERTIES_KEY = "Properties";
		private final Type<Pair<String, Dynamic<?>>>
				blockStateType =
				DSL.named(TypeReferences.BLOCK_STATE.typeName(), DSL.remainderType());
		protected final OpticFinder<List<Pair<String, Dynamic<?>>>>
				paletteFinder =
				DSL.fieldFinder("Palette", DSL.list(this.blockStateType));
		protected final List<Dynamic<?>> properties;
		protected final int y;
		protected @Nullable WordPackedArray blockStateMap;

		public ListFixer(Typed<?> sectionTyped, Schema inputSchema) {
			if (!Objects.equals(inputSchema.getType(TypeReferences.BLOCK_STATE), this.blockStateType)) {
				throw new IllegalStateException("Block state type is not what was expected.");
			}
			else {
				Optional<List<Pair<String, Dynamic<?>>>> optional = sectionTyped.getOptional(this.paletteFinder);
				this.properties =
						optional
								.<List<Dynamic<?>>>map(palettes -> palettes
										.stream()
										.<Dynamic<?>>map(Pair::getSecond)
										.collect(Collectors.toList()))
								.orElse(ImmutableList.of());
				Dynamic<?> dynamic = (Dynamic<?>) sectionTyped.get(DSL.remainderFinder());
				this.y = dynamic.get("Y").asInt(0);
				this.computeFixableBlockStates(dynamic);
			}
		}

		protected void computeFixableBlockStates(Dynamic<?> dynamic) {
			if (this.computeIsFixed()) {
				this.blockStateMap = null;
			}
			else {
				long[] ls = dynamic.get("BlockStates").asLongStream().toArray();
				int i = Math.max(4, DataFixUtils.ceillog2(this.properties.size()));
				this.blockStateMap = new WordPackedArray(i, BLOCKS_PER_SECTION, ls);
			}
		}

		@SuppressWarnings("unchecked")
		public Typed<?> finalizeFix(Typed<?> typed) {
			return this.isFixed()
			       ? typed
			       : typed.update(
					              DSL.remainderFinder(),
					              remainder -> remainder.set(
							              "BlockStates",
							              remainder.createLongList(Arrays.stream(this.blockStateMap.getAlignedArray()))
					              )
			              )
			              .set(
					              (OpticFinder) this.paletteFinder,
					              this.properties
					              .stream()
					              .map(propertiesDynamic -> Pair.of(
							              TypeReferences.BLOCK_STATE.typeName(),
							              propertiesDynamic
					              ))
					              .collect(Collectors.toList())
			              );
		}

		public boolean isFixed() {
			return this.blockStateMap == null;
		}

		public int blockStateAt(int index) {
			return this.blockStateMap.get(index);
		}

		protected int computeFlags(String leafBlockName, boolean persistent, int distance) {
			return LeavesFix.LEAVES_MAP.get(leafBlockName) << 5 | (persistent ? SIDE_FLAG_SOUTH : 0) | distance;
		}

		int getY() {
			return this.y;
		}

		protected abstract boolean computeIsFixed();
	}

	private static Object2IntMap<String> buildLeavesMap() {
		Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<>();
		map.defaultReturnValue(-1);
		map.put("minecraft:acacia_leaves", 0);
		map.put("minecraft:birch_leaves", 1);
		map.put("minecraft:dark_oak_leaves", 2);
		map.put("minecraft:jungle_leaves", 3);
		map.put("minecraft:oak_leaves", 4);
		map.put("minecraft:spruce_leaves", 5);
		return map;
	}
}
