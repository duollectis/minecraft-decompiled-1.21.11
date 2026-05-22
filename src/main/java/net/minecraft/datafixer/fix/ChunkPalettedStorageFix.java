package net.minecraft.datafixer.fix;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.WordPackedArray;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Конвертирует старый формат хранения блоков чанка (числовые ID + nibble-массивы)
 * в новый паллетизированный формат {@code Palette} + {@code BlockStates}.
 * Также обрабатывает специальные блоки: кровати, баннеры, двери, черепа, горшки с цветами.
 */
public class ChunkPalettedStorageFix extends DataFix {

	private static final int SIDE_FLAG_NORTH_WEST = 128;
	private static final int SIDE_FLAG_WEST = 64;
	private static final int SIDE_FLAG_SOUTH_WEST = 32;
	private static final int SIDE_FLAG_SOUTH = 16;
	private static final int SIDE_FLAG_SOUTH_EAST = 8;
	private static final int SIDE_FLAG_EAST = 4;
	private static final int SIDE_FLAG_NORTH_EAST = 2;
	private static final int SIDE_FLAG_NORTH = 1;
	static final Logger LOGGER = LogUtils.getLogger();
	private static final int BLOCKS_PER_SECTION = 4096;

	public ChunkPalettedStorageFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	public static String getName(Dynamic<?> dynamic) {
		return dynamic.get("Name").asString("");
	}

	public static String getProperty(Dynamic<?> dynamic, String propertyKey) {
		return dynamic.get("Properties").get(propertyKey).asString("");
	}

	public static int addTo(Int2ObjectBiMap<Dynamic<?>> int2ObjectBiMap, Dynamic<?> dynamic) {
		int i = int2ObjectBiMap.getRawId(dynamic);
		if (i == -1) {
			i = int2ObjectBiMap.add(dynamic);
		}

		return i;
	}

	private Dynamic<?> fixChunk(Dynamic<?> chunkDynamic) {
		Optional<? extends Dynamic<?>> optional = chunkDynamic.get("Level").result();
		return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent()
		       ? chunkDynamic.set("Level", new ChunkPalettedStorageFix.Level((Dynamic<?>) optional.get()).transform())
		       : chunkDynamic;
	}

	@Override
	public TypeRewriteRule makeRule() {
		Type<?> inputType = getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> outputType = getOutputSchema().getType(TypeReferences.CHUNK);
		return writeFixAndRead("ChunkPalettedStorageFix", inputType, outputType, this::fixChunk);
	}

	public static int getSideToUpgradeFlag(boolean west, boolean east, boolean north, boolean south) {
		int i = 0;
		if (north) {
			if (east) {
				i |= 2;
			}
			else if (west) {
				i |= SIDE_FLAG_NORTH_WEST;
			}
			else {
				i |= 1;
			}
		}
		else if (south) {
			if (west) {
				i |= SIDE_FLAG_SOUTH_WEST;
			}
			else if (east) {
				i |= 8;
			}
			else {
				i |= SIDE_FLAG_SOUTH;
			}
		}
		else if (east) {
			i |= 4;
		}
		else if (west) {
			i |= SIDE_FLAG_WEST;
		}

		return i;
	}

	/**
	 * Компактный nibble-массив (4 бита на элемент) для хранения данных освещения и блоков.
	 */
	static class ChunkNibbleArray {

		private static final int CONTENTS_LENGTH = 2048;
		private static final int NIBBLE_BITS = 4;
		private final byte[] contents;

		public ChunkNibbleArray() {
			this.contents = new byte[CONTENTS_LENGTH];
		}

		public ChunkNibbleArray(byte[] contents) {
			this.contents = contents;
			if (contents.length != CONTENTS_LENGTH) {
				throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + contents.length);
			}
		}

		public int get(int x, int y, int z) {
			int i = this.getRawIndex(y << 8 | z << 4 | x);
			return this.usesLowNibble(y << 8 | z << 4 | x) ? this.contents[i] & 15 : this.contents[i] >> 4 & 15;
		}

		private boolean usesLowNibble(int index) {
			return (index & 1) == 0;
		}

		private int getRawIndex(int index) {
			return index >> 1;
		}
	}

	public enum Facing {
		DOWN(ChunkPalettedStorageFix.Facing.Direction.NEGATIVE, ChunkPalettedStorageFix.Facing.Axis.Y),
		UP(ChunkPalettedStorageFix.Facing.Direction.POSITIVE, ChunkPalettedStorageFix.Facing.Axis.Y),
		NORTH(ChunkPalettedStorageFix.Facing.Direction.NEGATIVE, ChunkPalettedStorageFix.Facing.Axis.Z),
		SOUTH(ChunkPalettedStorageFix.Facing.Direction.POSITIVE, ChunkPalettedStorageFix.Facing.Axis.Z),
		WEST(ChunkPalettedStorageFix.Facing.Direction.NEGATIVE, ChunkPalettedStorageFix.Facing.Axis.X),
		EAST(ChunkPalettedStorageFix.Facing.Direction.POSITIVE, ChunkPalettedStorageFix.Facing.Axis.X);

		private final ChunkPalettedStorageFix.Facing.Axis axis;
		private final ChunkPalettedStorageFix.Facing.Direction direction;

		private Facing(
				final ChunkPalettedStorageFix.Facing.Direction direction,
				final ChunkPalettedStorageFix.Facing.Axis axis
		) {
			this.axis = axis;
			this.direction = direction;
		}

		public ChunkPalettedStorageFix.Facing.Direction getDirection() {
			return this.direction;
		}

		public ChunkPalettedStorageFix.Facing.Axis getAxis() {
			return this.axis;
		}

		public enum Axis {
			X,
			Y,
			Z;
		}

		public enum Direction {
			POSITIVE(1),
			NEGATIVE(-1);

			private final int offset;

			private Direction(final int offset) {
				this.offset = offset;
			}

			public int getOffset() {
				return this.offset;
			}
		}
	}

	/**
	 * Представляет данные уровня чанка во время миграции: хранит секции блоков
	 * и блок-сущности, выполняет контекстно-зависимые обновления (кровати, двери и т.д.).
	 */
	static final class Level {

		private int sidesToUpgrade;
		private final ChunkPalettedStorageFix.@Nullable Section[] sections = new ChunkPalettedStorageFix.Section[SIDE_FLAG_SOUTH];
		private final Dynamic<?> level;
		private final int x;
		private final int z;
		private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap(SIDE_FLAG_SOUTH);

		public Level(Dynamic<?> chunkTag) {
			this.level = chunkTag;
			this.x = chunkTag.get("xPos").asInt(0) << 4;
			this.z = chunkTag.get("zPos").asInt(0) << 4;
			chunkTag.get("TileEntities")
			        .asStreamOpt()
			        .ifSuccess(stream -> stream.forEach(blockEntityTag -> {
				        int localX = blockEntityTag.get("x").asInt(0) - this.x & 15;
				        int localY = blockEntityTag.get("y").asInt(0);
				        int localZ = blockEntityTag.get("z").asInt(0) - this.z & 15;
				        int packedPos = localY << 8 | localZ << 4 | localX;
	
				        if (this.blockEntities.put(packedPos, blockEntityTag) != null) {
					        ChunkPalettedStorageFix.LOGGER.warn(
							        "In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]",
							        this.x, this.z, localX, localY, localZ
					        );
				        }
			        }));
	
			boolean convertedFromAlpha = chunkTag.get("convertedFromAlphaFormat").asBoolean(false);
			chunkTag.get("Sections").asStreamOpt().ifSuccess(stream -> stream.forEach(sectionTag -> {
				ChunkPalettedStorageFix.Section section = new ChunkPalettedStorageFix.Section((Dynamic<?>) sectionTag);
				this.sidesToUpgrade = section.visit(this.sidesToUpgrade);
				this.sections[section.y] = section;
			}));

			for (ChunkPalettedStorageFix.Section section : this.sections) {
				if (section == null) {
					continue;
				}
	
				int sectionBaseY = section.y << 12;
	
				for (Entry<IntList> entry : section.inPlaceUpdates.int2ObjectEntrySet()) {
					switch (entry.getIntKey()) {
						case 2:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(block))) {
									String above = ChunkPalettedStorageFix.getName(
										this.getBlock(adjacentTo(pos, ChunkPalettedStorageFix.Facing.UP))
									);
	
									if ("minecraft:snow".equals(above) || "minecraft:snow_layer".equals(above)) {
										this.setBlock(pos, ChunkPalettedStorageFix.Mapping.SNOWY_GRASS_BLOCK_STATE);
									}
								}
							}
							break;
						case 3:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(block))) {
									String above = ChunkPalettedStorageFix.getName(
										this.getBlock(adjacentTo(pos, ChunkPalettedStorageFix.Facing.UP))
									);
	
									if ("minecraft:snow".equals(above) || "minecraft:snow_layer".equals(above)) {
										this.setBlock(pos, ChunkPalettedStorageFix.Mapping.SNOWY_PODZOL_STATE);
									}
								}
							}
							break;
						case 25:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> blockEntity = this.removeBlockEntity(pos);
	
								if (blockEntity != null) {
									String key = Boolean.toString(blockEntity.get("powered").asBoolean(false))
										+ (byte) Math.min(Math.max(blockEntity.get("note").asInt(0), 0), 24);
									this.setBlock(
										pos,
										ChunkPalettedStorageFix.Mapping.NOTE_BLOCK_IDS_TO_STATES
											.getOrDefault(key, ChunkPalettedStorageFix.Mapping.NOTE_BLOCK_IDS_TO_STATES.get("false0"))
									);
								}
							}
							break;
						case 26:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> blockEntity = this.getBlockEntity(pos);
								Dynamic<?> block = this.getBlock(pos);
	
								if (blockEntity != null) {
									int color = blockEntity.get("color").asInt(0);
	
									if (color != 14 && color >= 0 && color < SIDE_FLAG_SOUTH) {
										String key = ChunkPalettedStorageFix.getProperty(block, "facing")
											+ ChunkPalettedStorageFix.getProperty(block, "occupied")
											+ ChunkPalettedStorageFix.getProperty(block, "part")
											+ color;
	
										if (ChunkPalettedStorageFix.Mapping.BED_IDS_TO_STATES.containsKey(key)) {
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.BED_IDS_TO_STATES.get(key));
										}
									}
								}
							}
							break;
						case SIDE_FLAG_WEST:
						case 71:
						case 193:
						case 194:
						case 195:
						case 196:
						case 197:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if (ChunkPalettedStorageFix.getName(block).endsWith("_door")
									&& "lower".equals(ChunkPalettedStorageFix.getProperty(block, "half"))
								) {
									int upperPos = adjacentTo(pos, ChunkPalettedStorageFix.Facing.UP);
									Dynamic<?> upperBlock = this.getBlock(upperPos);
									String doorId = ChunkPalettedStorageFix.getName(block);
	
									if (doorId.equals(ChunkPalettedStorageFix.getName(upperBlock))) {
										String facing = ChunkPalettedStorageFix.getProperty(block, "facing");
										String open = ChunkPalettedStorageFix.getProperty(block, "open");
										String hinge = convertedFromAlpha
											? "left"
											: ChunkPalettedStorageFix.getProperty(upperBlock, "hinge");
										String powered = convertedFromAlpha
											? "false"
											: ChunkPalettedStorageFix.getProperty(upperBlock, "powered");
	
										this.setBlock(
											pos,
											ChunkPalettedStorageFix.Mapping.DOOR_IDS_TO_STATES.get(
												doorId + facing + "lower" + hinge + open + powered
											)
										);
										this.setBlock(
											upperPos,
											ChunkPalettedStorageFix.Mapping.DOOR_IDS_TO_STATES.get(
												doorId + facing + "upper" + hinge + open + powered
											)
										);
									}
								}
							}
							break;
						case 86:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(block))) {
									String below = ChunkPalettedStorageFix.getName(
										this.getBlock(adjacentTo(pos, ChunkPalettedStorageFix.Facing.DOWN))
									);
	
									if ("minecraft:grass_block".equals(below) || "minecraft:dirt".equals(below)) {
										this.setBlock(pos, ChunkPalettedStorageFix.Mapping.PUMPKIN_STATE);
									}
								}
							}
							break;
						case 110:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(block))) {
									String above = ChunkPalettedStorageFix.getName(
										this.getBlock(adjacentTo(pos, ChunkPalettedStorageFix.Facing.UP))
									);
	
									if ("minecraft:snow".equals(above) || "minecraft:snow_layer".equals(above)) {
										this.setBlock(pos, ChunkPalettedStorageFix.Mapping.SNOWY_MYCELIUM_STATE);
									}
								}
							}
							break;
						case 140:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> blockEntity = this.removeBlockEntity(pos);
	
								if (blockEntity != null) {
									String key = blockEntity.get("Item").asString("") + blockEntity.get("Data").asInt(0);
									this.setBlock(
										pos,
										ChunkPalettedStorageFix.Mapping.PLANT_TO_FLOWER_POT_STATES
											.getOrDefault(key, ChunkPalettedStorageFix.Mapping.PLANT_TO_FLOWER_POT_STATES.get("minecraft:air0"))
									);
								}
							}
							break;
						case 144:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> blockEntity = this.getBlockEntity(pos);
	
								if (blockEntity != null) {
									String skullType = String.valueOf(blockEntity.get("SkullType").asInt(0));
									String facing = ChunkPalettedStorageFix.getProperty(this.getBlock(pos), "facing");
									String skullKey = ("up".equals(facing) || "down".equals(facing))
										? skullType + blockEntity.get("Rot").asInt(0)
										: skullType + facing;
	
									blockEntity.remove("SkullType");
									blockEntity.remove("facing");
									blockEntity.remove("Rot");
									this.setBlock(
										pos,
										ChunkPalettedStorageFix.Mapping.SKULL_IDS_TO_STATES
											.getOrDefault(skullKey, ChunkPalettedStorageFix.Mapping.SKULL_IDS_TO_STATES.get("0north"))
									);
								}
							}
							break;
						case 175:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> block = this.getBlock(pos);
	
								if ("upper".equals(ChunkPalettedStorageFix.getProperty(block, "half"))) {
									Dynamic<?> below = this.getBlock(adjacentTo(pos, ChunkPalettedStorageFix.Facing.DOWN));
									String belowName = ChunkPalettedStorageFix.getName(below);
	
									switch (belowName) {
										case "minecraft:sunflower":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_SUNFLOWER_STATE);
											break;
										case "minecraft:lilac":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_LILAC_STATE);
											break;
										case "minecraft:tall_grass":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_TALL_GRASS_STATE);
											break;
										case "minecraft:large_fern":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_LARGE_FERN_STATE);
											break;
										case "minecraft:rose_bush":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_ROSE_BUSH_STATE);
											break;
										case "minecraft:peony":
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.UPPER_HALF_PEONY_STATE);
											break;
									}
								}
							}
							break;
						case 176:
						case 177:
							for (int packedPos : entry.getValue()) {
								int pos = packedPos | sectionBaseY;
								Dynamic<?> blockEntity = this.getBlockEntity(pos);
								Dynamic<?> block = this.getBlock(pos);
	
								if (blockEntity != null) {
									int base = blockEntity.get("Base").asInt(0);
	
									if (base != 15 && base >= 0 && base < SIDE_FLAG_SOUTH) {
										String key = ChunkPalettedStorageFix.getProperty(
											block,
											entry.getIntKey() == 176 ? "rotation" : "facing"
										) + "_" + base;
	
										if (ChunkPalettedStorageFix.Mapping.BANNER_IDS_TO_STATES.containsKey(key)) {
											this.setBlock(pos, ChunkPalettedStorageFix.Mapping.BANNER_IDS_TO_STATES.get(key));
										}
									}
								}
							}
					}
				}
			}
		}

		private @Nullable Dynamic<?> getBlockEntity(int packedLocalPos) {
			return (Dynamic<?>) this.blockEntities.get(packedLocalPos);
		}

		private @Nullable Dynamic<?> removeBlockEntity(int packedLocalPos) {
			return (Dynamic<?>) this.blockEntities.remove(packedLocalPos);
		}

		public static int adjacentTo(int packedLocalPos, ChunkPalettedStorageFix.Facing direction) {
			return switch (direction.getAxis()) {
				case X -> {
					int i = (packedLocalPos & 15) + direction.getDirection().getOffset();
					yield i >= 0 && i <= 15 ? packedLocalPos & -SIDE_FLAG_SOUTH | i : -1;
				}
				case Y -> {
					int i = (packedLocalPos >> 8) + direction.getDirection().getOffset();
					yield i >= 0 && i <= 255 ? packedLocalPos & 0xFF | i << 8 : -1;
				}
				case Z -> {
					int i = (packedLocalPos >> 4 & 15) + direction.getDirection().getOffset();
					yield i >= 0 && i <= 15 ? packedLocalPos & -241 | i << 4 : -1;
				}
			};
		}

		private void setBlock(int packedLocalPos, Dynamic<?> dynamic) {
			if (packedLocalPos >= 0 && packedLocalPos <= 65535) {
				ChunkPalettedStorageFix.Section section = this.getSection(packedLocalPos);
				if (section != null) {
					section.setBlock(packedLocalPos & 4095, dynamic);
				}
			}
		}

		private ChunkPalettedStorageFix.@Nullable Section getSection(int packedLocalPos) {
			int i = packedLocalPos >> 12;
			return i < this.sections.length ? this.sections[i] : null;
		}

		public Dynamic<?> getBlock(int packedLocalPos) {
			if (packedLocalPos >= 0 && packedLocalPos <= 65535) {
				ChunkPalettedStorageFix.Section section = this.getSection(packedLocalPos);
				return section == null ? ChunkPalettedStorageFix.Mapping.AIR_STATE
				                       : section.getBlock(packedLocalPos & 4095);
			}
			else {
				return ChunkPalettedStorageFix.Mapping.AIR_STATE;
			}
		}

		public Dynamic<?> transform() {
			Dynamic<?> dynamic = this.level;
			if (this.blockEntities.isEmpty()) {
				dynamic = dynamic.remove("TileEntities");
			}
			else {
				dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
			}

			Dynamic<?> dynamic2 = dynamic.emptyMap();
			List<Dynamic<?>> list = Lists.newArrayList();

			for (ChunkPalettedStorageFix.Section section : this.sections) {
				if (section != null) {
					list.add(section.transform());
					dynamic2 =
							dynamic2.set(
									String.valueOf(section.y),
									dynamic2.createIntList(Arrays.stream(section.innerPositions.toIntArray()))
							);
				}
			}

			Dynamic<?> dynamic3 = dynamic.emptyMap();
			dynamic3 = dynamic3.set("Sides", dynamic3.createByte((byte) this.sidesToUpgrade));
			dynamic3 = dynamic3.set("Indices", dynamic2);
			return dynamic.set("UpgradeData", dynamic3).set("Sections", dynamic3.createList(list.stream()));
		}
	}

	/**
 * Содержит маппинги для преобразования данных.
 */
	static class Mapping {

		static final BitSet STAIR_BLOCKS = new BitSet(256);
		static final BitSet DOOR_BLOCKS = new BitSet(256);
		static final Dynamic<?> PUMPKIN_STATE = FixUtil.createBlockState("minecraft:pumpkin");
		static final Dynamic<?>
				SNOWY_PODZOL_STATE =
				FixUtil.createBlockState("minecraft:podzol", Map.of("snowy", "true"));
		static final Dynamic<?>
				SNOWY_GRASS_BLOCK_STATE =
				FixUtil.createBlockState("minecraft:grass_block", Map.of("snowy", "true"));
		static final Dynamic<?>
				SNOWY_MYCELIUM_STATE =
				FixUtil.createBlockState("minecraft:mycelium", Map.of("snowy", "true"));
		static final Dynamic<?>
				UPPER_HALF_SUNFLOWER_STATE =
				FixUtil.createBlockState("minecraft:sunflower", Map.of("half", "upper"));
		static final Dynamic<?>
				UPPER_HALF_LILAC_STATE =
				FixUtil.createBlockState("minecraft:lilac", Map.of("half", "upper"));
		static final Dynamic<?>
				UPPER_HALF_TALL_GRASS_STATE =
				FixUtil.createBlockState("minecraft:tall_grass", Map.of("half", "upper"));
		static final Dynamic<?>
				UPPER_HALF_LARGE_FERN_STATE =
				FixUtil.createBlockState("minecraft:large_fern", Map.of("half", "upper"));
		static final Dynamic<?>
				UPPER_HALF_ROSE_BUSH_STATE =
				FixUtil.createBlockState("minecraft:rose_bush", Map.of("half", "upper"));
		static final Dynamic<?>
				UPPER_HALF_PEONY_STATE =
				FixUtil.createBlockState("minecraft:peony", Map.of("half", "upper"));
		static final Map<String, Dynamic<?>> PLANT_TO_FLOWER_POT_STATES = Map.ofEntries(
			Map.entry("minecraft:air0", FixUtil.createBlockState("minecraft:flower_pot")),
			Map.entry("minecraft:red_flower0", FixUtil.createBlockState("minecraft:potted_poppy")),
			Map.entry("minecraft:red_flower1", FixUtil.createBlockState("minecraft:potted_blue_orchid")),
			Map.entry("minecraft:red_flower2", FixUtil.createBlockState("minecraft:potted_allium")),
			Map.entry("minecraft:red_flower3", FixUtil.createBlockState("minecraft:potted_azure_bluet")),
			Map.entry("minecraft:red_flower4", FixUtil.createBlockState("minecraft:potted_red_tulip")),
			Map.entry("minecraft:red_flower5", FixUtil.createBlockState("minecraft:potted_orange_tulip")),
			Map.entry("minecraft:red_flower6", FixUtil.createBlockState("minecraft:potted_white_tulip")),
			Map.entry("minecraft:red_flower7", FixUtil.createBlockState("minecraft:potted_pink_tulip")),
			Map.entry("minecraft:red_flower8", FixUtil.createBlockState("minecraft:potted_oxeye_daisy")),
			Map.entry("minecraft:yellow_flower0", FixUtil.createBlockState("minecraft:potted_dandelion")),
			Map.entry("minecraft:sapling0", FixUtil.createBlockState("minecraft:potted_oak_sapling")),
			Map.entry("minecraft:sapling1", FixUtil.createBlockState("minecraft:potted_spruce_sapling")),
			Map.entry("minecraft:sapling2", FixUtil.createBlockState("minecraft:potted_birch_sapling")),
			Map.entry("minecraft:sapling3", FixUtil.createBlockState("minecraft:potted_jungle_sapling")),
			Map.entry("minecraft:sapling4", FixUtil.createBlockState("minecraft:potted_acacia_sapling")),
			Map.entry("minecraft:sapling5", FixUtil.createBlockState("minecraft:potted_dark_oak_sapling")),
			Map.entry("minecraft:red_mushroom0", FixUtil.createBlockState("minecraft:potted_red_mushroom")),
			Map.entry("minecraft:brown_mushroom0", FixUtil.createBlockState("minecraft:potted_brown_mushroom")),
			Map.entry("minecraft:deadbush0", FixUtil.createBlockState("minecraft:potted_dead_bush")),
			Map.entry("minecraft:tallgrass2", FixUtil.createBlockState("minecraft:potted_fern")),
			Map.entry("minecraft:cactus0", FixUtil.createBlockState("minecraft:potted_cactus"))
		);
		static final Map<String, Dynamic<?>> SKULL_IDS_TO_STATES = buildSkullStates();
		static final Map<String, Dynamic<?>> DOOR_IDS_TO_STATES = buildDoorStates();
		static final Map<String, Dynamic<?>> NOTE_BLOCK_IDS_TO_STATES = buildNoteBlockStates();
		private static final Int2ObjectMap<String> COLORS_BY_IDS = buildColorsByIds();
		static final Map<String, Dynamic<?>> BED_IDS_TO_STATES = buildBedStates();
		static final Map<String, Dynamic<?>> BANNER_IDS_TO_STATES = buildBannerStates();
		static final Dynamic<?> AIR_STATE = FixUtil.createBlockState("minecraft:air");

		private Mapping() {
		}

		private static Map<String, Dynamic<?>> buildSkullStates() {
			Map<String, Dynamic<?>> map = new HashMap<>();
			skull(map, 0, "skeleton", "skull");
			skull(map, 1, "wither_skeleton", "skull");
			skull(map, 2, "zombie", "head");
			skull(map, 3, "player", "head");
			skull(map, 4, "creeper", "head");
			skull(map, 5, "dragon", "head");
			return Collections.unmodifiableMap(map);
		}

		private static Map<String, Dynamic<?>> buildDoorStates() {
			Map<String, Dynamic<?>> map = new HashMap<>();
			door(map, "oak_door");
			door(map, "iron_door");
			door(map, "spruce_door");
			door(map, "birch_door");
			door(map, "jungle_door");
			door(map, "acacia_door");
			door(map, "dark_oak_door");
			return Collections.unmodifiableMap(map);
		}

		private static Map<String, Dynamic<?>> buildNoteBlockStates() {
			Map<String, Dynamic<?>> map = new HashMap<>();

			for (int i = 0; i < 26; i++) {
				map.put(
					"true" + i,
					FixUtil.createBlockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i)))
				);
				map.put(
					"false" + i,
					FixUtil.createBlockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i)))
				);
			}

			return Collections.unmodifiableMap(map);
		}

		private static Int2ObjectMap<String> buildColorsByIds() {
			Int2ObjectMap<String> map = new Int2ObjectOpenHashMap<>();
			map.put(0, "white");
			map.put(1, "orange");
			map.put(2, "magenta");
			map.put(3, "light_blue");
			map.put(4, "yellow");
			map.put(5, "lime");
			map.put(6, "pink");
			map.put(7, "gray");
			map.put(8, "light_gray");
			map.put(9, "cyan");
			map.put(10, "purple");
			map.put(11, "blue");
			map.put(12, "brown");
			map.put(13, "green");
			map.put(14, "red");
			map.put(15, "black");
			return Int2ObjectMaps.unmodifiable(map);
		}

		private static Map<String, Dynamic<?>> buildBedStates() {
			Map<String, Dynamic<?>> map = new HashMap<>();

			for (Entry<String> entry : COLORS_BY_IDS.int2ObjectEntrySet()) {
				if (!Objects.equals(entry.getValue(), "red")) {
					bed(map, entry.getIntKey(), entry.getValue());
				}
			}

			return Collections.unmodifiableMap(map);
		}

		private static Map<String, Dynamic<?>> buildBannerStates() {
			Map<String, Dynamic<?>> map = new HashMap<>();

			for (Entry<String> entry : COLORS_BY_IDS.int2ObjectEntrySet()) {
				if (!Objects.equals(entry.getValue(), "white")) {
					banner(map, 15 - entry.getIntKey(), entry.getValue());
				}
			}

			return Collections.unmodifiableMap(map);
		}

		private static void skull(Map<String, Dynamic<?>> map, int id, String entity, String type) {
			map.put(
					id + "north",
					FixUtil.createBlockState("minecraft:" + entity + "_wall_" + type, Map.of("facing", "north"))
			);
			map.put(
					id + "east",
					FixUtil.createBlockState("minecraft:" + entity + "_wall_" + type, Map.of("facing", "east"))
			);
			map.put(
					id + "south",
					FixUtil.createBlockState("minecraft:" + entity + "_wall_" + type, Map.of("facing", "south"))
			);
			map.put(
					id + "west",
					FixUtil.createBlockState("minecraft:" + entity + "_wall_" + type, Map.of("facing", "west"))
			);

			for (int i = 0; i < SIDE_FLAG_SOUTH; i++) {
				map.put(
						"" + id + i,
						FixUtil.createBlockState(
								"minecraft:" + entity + "_" + type,
								Map.of("rotation", String.valueOf(i))
						)
				);
			}
		}

		private static void door(Map<String, Dynamic<?>> map, String id) {
			String string = "minecraft:" + id;
			map.put(
					"minecraft:" + id + "eastlowerleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastlowerrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "eastupperrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"east",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northlowerrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "northupperrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"north",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southlowerrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "southupperrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"south",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westlowerrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"lower",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperleftfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperleftfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperlefttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperlefttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"left",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperrightfalsefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperrightfalsetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"false",
									"powered",
									"true"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperrighttruefalse",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"false"
							)
					)
			);
			map.put(
					"minecraft:" + id + "westupperrighttruetrue",
					FixUtil.createBlockState(
							string,
							Map.of(
									"facing",
									"west",
									"half",
									"upper",
									"hinge",
									"right",
									"open",
									"true",
									"powered",
									"true"
							)
					)
			);
		}

		private static void bed(Map<String, Dynamic<?>> map, int id, String color) {
			map.put(
					"southfalsefoot" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "south", "occupied", "false", "part", "foot")
					)
			);
			map.put(
					"westfalsefoot" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "west", "occupied", "false", "part", "foot")
					)
			);
			map.put(
					"northfalsefoot" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "north", "occupied", "false", "part", "foot")
					)
			);
			map.put(
					"eastfalsefoot" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "east", "occupied", "false", "part", "foot")
					)
			);
			map.put(
					"southfalsehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "south", "occupied", "false", "part", "head")
					)
			);
			map.put(
					"westfalsehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "west", "occupied", "false", "part", "head")
					)
			);
			map.put(
					"northfalsehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "north", "occupied", "false", "part", "head")
					)
			);
			map.put(
					"eastfalsehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "east", "occupied", "false", "part", "head")
					)
			);
			map.put(
					"southtruehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "south", "occupied", "true", "part", "head")
					)
			);
			map.put(
					"westtruehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "west", "occupied", "true", "part", "head")
					)
			);
			map.put(
					"northtruehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "north", "occupied", "true", "part", "head")
					)
			);
			map.put(
					"easttruehead" + id,
					FixUtil.createBlockState(
							"minecraft:" + color + "_bed",
							Map.of("facing", "east", "occupied", "true", "part", "head")
					)
			);
		}

		private static void banner(Map<String, Dynamic<?>> map, int id, String color) {
			for (int i = 0; i < SIDE_FLAG_SOUTH; i++) {
				map.put(
						i + "_" + id,
						FixUtil.createBlockState(
								"minecraft:" + color + "_banner",
								Map.of("rotation", String.valueOf(i))
						)
				);
			}

			map.put(
					"north_" + id,
					FixUtil.createBlockState("minecraft:" + color + "_wall_banner", Map.of("facing", "north"))
			);
			map.put(
					"south_" + id,
					FixUtil.createBlockState("minecraft:" + color + "_wall_banner", Map.of("facing", "south"))
			);
			map.put(
					"west_" + id,
					FixUtil.createBlockState("minecraft:" + color + "_wall_banner", Map.of("facing", "west"))
			);
			map.put(
					"east_" + id,
					FixUtil.createBlockState("minecraft:" + color + "_wall_banner", Map.of("facing", "east"))
			);
		}

		static {
			DOOR_BLOCKS.set(2);
			DOOR_BLOCKS.set(3);
			DOOR_BLOCKS.set(110);
			DOOR_BLOCKS.set(140);
			DOOR_BLOCKS.set(144);
			DOOR_BLOCKS.set(25);
			DOOR_BLOCKS.set(86);
			DOOR_BLOCKS.set(26);
			DOOR_BLOCKS.set(176);
			DOOR_BLOCKS.set(177);
			DOOR_BLOCKS.set(175);
			DOOR_BLOCKS.set(SIDE_FLAG_WEST);
			DOOR_BLOCKS.set(71);
			DOOR_BLOCKS.set(193);
			DOOR_BLOCKS.set(194);
			DOOR_BLOCKS.set(195);
			DOOR_BLOCKS.set(196);
			DOOR_BLOCKS.set(197);
			STAIR_BLOCKS.set(54);
			STAIR_BLOCKS.set(146);
			STAIR_BLOCKS.set(25);
			STAIR_BLOCKS.set(26);
			STAIR_BLOCKS.set(51);
			STAIR_BLOCKS.set(53);
			STAIR_BLOCKS.set(67);
			STAIR_BLOCKS.set(108);
			STAIR_BLOCKS.set(109);
			STAIR_BLOCKS.set(114);
			STAIR_BLOCKS.set(SIDE_FLAG_NORTH_WEST);
			STAIR_BLOCKS.set(134);
			STAIR_BLOCKS.set(135);
			STAIR_BLOCKS.set(136);
			STAIR_BLOCKS.set(156);
			STAIR_BLOCKS.set(163);
			STAIR_BLOCKS.set(164);
			STAIR_BLOCKS.set(180);
			STAIR_BLOCKS.set(203);
			STAIR_BLOCKS.set(55);
			STAIR_BLOCKS.set(85);
			STAIR_BLOCKS.set(113);
			STAIR_BLOCKS.set(188);
			STAIR_BLOCKS.set(189);
			STAIR_BLOCKS.set(190);
			STAIR_BLOCKS.set(191);
			STAIR_BLOCKS.set(192);
			STAIR_BLOCKS.set(93);
			STAIR_BLOCKS.set(94);
			STAIR_BLOCKS.set(101);
			STAIR_BLOCKS.set(102);
			STAIR_BLOCKS.set(160);
			STAIR_BLOCKS.set(106);
			STAIR_BLOCKS.set(107);
			STAIR_BLOCKS.set(183);
			STAIR_BLOCKS.set(184);
			STAIR_BLOCKS.set(185);
			STAIR_BLOCKS.set(186);
			STAIR_BLOCKS.set(187);
			STAIR_BLOCKS.set(132);
			STAIR_BLOCKS.set(139);
			STAIR_BLOCKS.set(199);
		}
	}

	/**
	 * Представляет одну 16×16×16 секцию чанка в старом формате (до паллетизации).
	 * Хранит блоки в виде числовых ID и накапливает список позиций,
	 * требующих дополнительной обработки (двери, кровати, баннеры и т.д.).
	 */
	static class Section {

		private static final int MAX_BLOCK_INDEX = BLOCKS_PER_SECTION - 1;

		private final Int2ObjectBiMap<Dynamic<?>> paletteMap = Int2ObjectBiMap.create(SIDE_FLAG_SOUTH_WEST);
		private final List<Dynamic<?>> paletteData;
		private final Dynamic<?> section;
		private final boolean hasBlocks;
		final Int2ObjectMap<IntList> inPlaceUpdates = new Int2ObjectLinkedOpenHashMap<>();
		final IntList innerPositions = new IntArrayList();
		public final int y;
		private final Set<Dynamic<?>> seenStates = Sets.newIdentityHashSet();
		private final int[] states = new int[BLOCKS_PER_SECTION];

		public Section(Dynamic<?> section) {
			this.paletteData = Lists.newArrayList();
			this.section = section;
			this.y = section.get("Y").asInt(0);
			this.hasBlocks = section.get("Blocks").result().isPresent();
		}

		public Dynamic<?> getBlock(int index) {
			if (index < 0 || index > MAX_BLOCK_INDEX) {
				return ChunkPalettedStorageFix.Mapping.AIR_STATE;
			}

			Dynamic<?> block = paletteMap.get(states[index]);
			return block == null ? ChunkPalettedStorageFix.Mapping.AIR_STATE : block;
		}

		public void setBlock(int pos, Dynamic<?> dynamic) {
			if (this.seenStates.add(dynamic)) {
				this.paletteData.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(dynamic))
				                     ? ChunkPalettedStorageFix.Mapping.AIR_STATE : dynamic);
			}

			this.states[pos] = ChunkPalettedStorageFix.addTo(this.paletteMap, dynamic);
		}

		public int visit(int sidesToUpgrade) {
			if (!hasBlocks) {
				return sidesToUpgrade;
			}

			ByteBuffer byteBuffer = section.get("Blocks").asByteBufferOpt().result().get();
			ChunkPalettedStorageFix.ChunkNibbleArray dataNibbles = section
				.get("Data")
				.asByteBufferOpt()
				.map(buf -> new ChunkPalettedStorageFix.ChunkNibbleArray(DataFixUtils.toArray(buf)))
				.result()
				.orElseGet(ChunkPalettedStorageFix.ChunkNibbleArray::new);
			ChunkPalettedStorageFix.ChunkNibbleArray addNibbles = section
				.get("Add")
				.asByteBufferOpt()
				.map(buf -> new ChunkPalettedStorageFix.ChunkNibbleArray(DataFixUtils.toArray(buf)))
				.result()
				.orElseGet(ChunkPalettedStorageFix.ChunkNibbleArray::new);
			seenStates.add(ChunkPalettedStorageFix.Mapping.AIR_STATE);
			ChunkPalettedStorageFix.addTo(paletteMap, ChunkPalettedStorageFix.Mapping.AIR_STATE);
			paletteData.add(ChunkPalettedStorageFix.Mapping.AIR_STATE);

			for (int i = 0; i < BLOCKS_PER_SECTION; i++) {
				int j = i & 15;
				int k = i >> 8 & 15;
				int l = i >> 4 & 15;
				int numericId = addNibbles.get(j, k, l) << 12 | (byteBuffer.get(i) & 255) << 4 | dataNibbles.get(j, k, l);
				int blockId = numericId >> 4;
	
				if (ChunkPalettedStorageFix.Mapping.DOOR_BLOCKS.get(blockId)) {
					addInPlaceUpdate(blockId, i);
				}
	
				if (ChunkPalettedStorageFix.Mapping.STAIR_BLOCKS.get(blockId)) {
					int sideFlag = ChunkPalettedStorageFix.getSideToUpgradeFlag(j == 0, j == 15, l == 0, l == 15);
	
					if (sideFlag == 0) {
						innerPositions.add(i);
					}
					else {
						sidesToUpgrade |= sideFlag;
					}
				}
	
				setBlock(i, BlockStateFlattening.lookupState(numericId));
			}
	
			return sidesToUpgrade;
		}

		private void addInPlaceUpdate(int blockId, int index) {
			inPlaceUpdates.computeIfAbsent(blockId, k -> new IntArrayList()).add(index);
		}

		public Dynamic<?> transform() {
			if (!hasBlocks) {
				return section;
			}

			Dynamic<?> result = section.set("Palette", section.createList(paletteData.stream()));
			int bitsPerEntry = Math.max(4, DataFixUtils.ceillog2(seenStates.size()));
			WordPackedArray packedArray = new WordPackedArray(bitsPerEntry, BLOCKS_PER_SECTION);

			for (int i = 0; i < states.length; i++) {
				packedArray.set(i, states[i]);
			}

			result = result.set("BlockStates", result.createLongList(Arrays.stream(packedArray.getAlignedArray())));
			result = result.remove("Blocks");
			result = result.remove("Data");
			return result.remove("Add");
		}
	}
}
