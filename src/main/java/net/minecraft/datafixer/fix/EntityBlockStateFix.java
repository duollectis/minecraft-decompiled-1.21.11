package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Конвертирует числовые идентификаторы блоков в блок-стейты для сущностей,
 * которые хранят блок внутри себя: падающие блоки, эндермены, миникарты, снаряды.
 */
public class EntityBlockStateFix extends DataFix {

	private static final Map<String, Integer> BLOCK_NAME_TO_ID = Map.ofEntries(
		Map.entry("minecraft:air", 0),
		Map.entry("minecraft:stone", 1),
		Map.entry("minecraft:grass", 2),
		Map.entry("minecraft:dirt", 3),
		Map.entry("minecraft:cobblestone", 4),
		Map.entry("minecraft:planks", 5),
		Map.entry("minecraft:sapling", 6),
		Map.entry("minecraft:bedrock", 7),
		Map.entry("minecraft:flowing_water", 8),
		Map.entry("minecraft:water", 9),
		Map.entry("minecraft:flowing_lava", 10),
		Map.entry("minecraft:lava", 11),
		Map.entry("minecraft:sand", 12),
		Map.entry("minecraft:gravel", 13),
		Map.entry("minecraft:gold_ore", 14),
		Map.entry("minecraft:iron_ore", 15),
		Map.entry("minecraft:coal_ore", 16),
		Map.entry("minecraft:log", 17),
		Map.entry("minecraft:leaves", 18),
		Map.entry("minecraft:sponge", 19),
		Map.entry("minecraft:glass", 20),
		Map.entry("minecraft:lapis_ore", 21),
		Map.entry("minecraft:lapis_block", 22),
		Map.entry("minecraft:dispenser", 23),
		Map.entry("minecraft:sandstone", 24),
		Map.entry("minecraft:noteblock", 25),
		Map.entry("minecraft:bed", 26),
		Map.entry("minecraft:golden_rail", 27),
		Map.entry("minecraft:detector_rail", 28),
		Map.entry("minecraft:sticky_piston", 29),
		Map.entry("minecraft:web", 30),
		Map.entry("minecraft:tallgrass", 31),
		Map.entry("minecraft:deadbush", 32),
		Map.entry("minecraft:piston", 33),
		Map.entry("minecraft:piston_head", 34),
		Map.entry("minecraft:wool", 35),
		Map.entry("minecraft:piston_extension", 36),
		Map.entry("minecraft:yellow_flower", 37),
		Map.entry("minecraft:red_flower", 38),
		Map.entry("minecraft:brown_mushroom", 39),
		Map.entry("minecraft:red_mushroom", 40),
		Map.entry("minecraft:gold_block", 41),
		Map.entry("minecraft:iron_block", 42),
		Map.entry("minecraft:double_stone_slab", 43),
		Map.entry("minecraft:stone_slab", 44),
		Map.entry("minecraft:brick_block", 45),
		Map.entry("minecraft:tnt", 46),
		Map.entry("minecraft:bookshelf", 47),
		Map.entry("minecraft:mossy_cobblestone", 48),
		Map.entry("minecraft:obsidian", 49),
		Map.entry("minecraft:torch", 50),
		Map.entry("minecraft:fire", 51),
		Map.entry("minecraft:mob_spawner", 52),
		Map.entry("minecraft:oak_stairs", 53),
		Map.entry("minecraft:chest", 54),
		Map.entry("minecraft:redstone_wire", 55),
		Map.entry("minecraft:diamond_ore", 56),
		Map.entry("minecraft:diamond_block", 57),
		Map.entry("minecraft:crafting_table", 58),
		Map.entry("minecraft:wheat", 59),
		Map.entry("minecraft:farmland", 60),
		Map.entry("minecraft:furnace", 61),
		Map.entry("minecraft:lit_furnace", 62),
		Map.entry("minecraft:standing_sign", 63),
		Map.entry("minecraft:wooden_door", 64),
		Map.entry("minecraft:ladder", 65),
		Map.entry("minecraft:rail", 66),
		Map.entry("minecraft:stone_stairs", 67),
		Map.entry("minecraft:wall_sign", 68),
		Map.entry("minecraft:lever", 69),
		Map.entry("minecraft:stone_pressure_plate", 70),
		Map.entry("minecraft:iron_door", 71),
		Map.entry("minecraft:wooden_pressure_plate", 72),
		Map.entry("minecraft:redstone_ore", 73),
		Map.entry("minecraft:lit_redstone_ore", 74),
		Map.entry("minecraft:unlit_redstone_torch", 75),
		Map.entry("minecraft:redstone_torch", 76),
		Map.entry("minecraft:stone_button", 77),
		Map.entry("minecraft:snow_layer", 78),
		Map.entry("minecraft:ice", 79),
		Map.entry("minecraft:snow", 80),
		Map.entry("minecraft:cactus", 81),
		Map.entry("minecraft:clay", 82),
		Map.entry("minecraft:reeds", 83),
		Map.entry("minecraft:jukebox", 84),
		Map.entry("minecraft:fence", 85),
		Map.entry("minecraft:pumpkin", 86),
		Map.entry("minecraft:netherrack", 87),
		Map.entry("minecraft:soul_sand", 88),
		Map.entry("minecraft:glowstone", 89),
		Map.entry("minecraft:portal", 90),
		Map.entry("minecraft:lit_pumpkin", 91),
		Map.entry("minecraft:cake", 92),
		Map.entry("minecraft:unpowered_repeater", 93),
		Map.entry("minecraft:powered_repeater", 94),
		Map.entry("minecraft:stained_glass", 95),
		Map.entry("minecraft:trapdoor", 96),
		Map.entry("minecraft:monster_egg", 97),
		Map.entry("minecraft:stonebrick", 98),
		Map.entry("minecraft:brown_mushroom_block", 99),
		Map.entry("minecraft:red_mushroom_block", 100),
		Map.entry("minecraft:iron_bars", 101),
		Map.entry("minecraft:glass_pane", 102),
		Map.entry("minecraft:melon_block", 103),
		Map.entry("minecraft:pumpkin_stem", 104),
		Map.entry("minecraft:melon_stem", 105),
		Map.entry("minecraft:vine", 106),
		Map.entry("minecraft:fence_gate", 107),
		Map.entry("minecraft:brick_stairs", 108),
		Map.entry("minecraft:stone_brick_stairs", 109),
		Map.entry("minecraft:mycelium", 110),
		Map.entry("minecraft:waterlily", 111),
		Map.entry("minecraft:nether_brick", 112),
		Map.entry("minecraft:nether_brick_fence", 113),
		Map.entry("minecraft:nether_brick_stairs", 114),
		Map.entry("minecraft:nether_wart", 115),
		Map.entry("minecraft:enchanting_table", 116),
		Map.entry("minecraft:brewing_stand", 117),
		Map.entry("minecraft:cauldron", 118),
		Map.entry("minecraft:end_portal", 119),
		Map.entry("minecraft:end_portal_frame", 120),
		Map.entry("minecraft:end_stone", 121),
		Map.entry("minecraft:dragon_egg", 122),
		Map.entry("minecraft:redstone_lamp", 123),
		Map.entry("minecraft:lit_redstone_lamp", 124),
		Map.entry("minecraft:double_wooden_slab", 125),
		Map.entry("minecraft:wooden_slab", 126),
		Map.entry("minecraft:cocoa", 127),
		Map.entry("minecraft:sandstone_stairs", 128),
		Map.entry("minecraft:emerald_ore", 129),
		Map.entry("minecraft:ender_chest", 130),
		Map.entry("minecraft:tripwire_hook", 131),
		Map.entry("minecraft:tripwire", 132),
		Map.entry("minecraft:emerald_block", 133),
		Map.entry("minecraft:spruce_stairs", 134),
		Map.entry("minecraft:birch_stairs", 135),
		Map.entry("minecraft:jungle_stairs", 136),
		Map.entry("minecraft:command_block", 137),
		Map.entry("minecraft:beacon", 138),
		Map.entry("minecraft:cobblestone_wall", 139),
		Map.entry("minecraft:flower_pot", 140),
		Map.entry("minecraft:carrots", 141),
		Map.entry("minecraft:potatoes", 142),
		Map.entry("minecraft:wooden_button", 143),
		Map.entry("minecraft:skull", 144),
		Map.entry("minecraft:anvil", 145),
		Map.entry("minecraft:trapped_chest", 146),
		Map.entry("minecraft:light_weighted_pressure_plate", 147),
		Map.entry("minecraft:heavy_weighted_pressure_plate", 148),
		Map.entry("minecraft:unpowered_comparator", 149),
		Map.entry("minecraft:powered_comparator", 150),
		Map.entry("minecraft:daylight_detector", 151),
		Map.entry("minecraft:redstone_block", 152),
		Map.entry("minecraft:quartz_ore", 153),
		Map.entry("minecraft:hopper", 154),
		Map.entry("minecraft:quartz_block", 155),
		Map.entry("minecraft:quartz_stairs", 156),
		Map.entry("minecraft:activator_rail", 157),
		Map.entry("minecraft:dropper", 158),
		Map.entry("minecraft:stained_hardened_clay", 159),
		Map.entry("minecraft:stained_glass_pane", 160),
		Map.entry("minecraft:leaves2", 161),
		Map.entry("minecraft:log2", 162),
		Map.entry("minecraft:acacia_stairs", 163),
		Map.entry("minecraft:dark_oak_stairs", 164),
		Map.entry("minecraft:slime", 165),
		Map.entry("minecraft:barrier", 166),
		Map.entry("minecraft:iron_trapdoor", 167),
		Map.entry("minecraft:prismarine", 168),
		Map.entry("minecraft:sea_lantern", 169),
		Map.entry("minecraft:hay_block", 170),
		Map.entry("minecraft:carpet", 171),
		Map.entry("minecraft:hardened_clay", 172),
		Map.entry("minecraft:coal_block", 173),
		Map.entry("minecraft:packed_ice", 174),
		Map.entry("minecraft:double_plant", 175),
		Map.entry("minecraft:standing_banner", 176),
		Map.entry("minecraft:wall_banner", 177),
		Map.entry("minecraft:daylight_detector_inverted", 178),
		Map.entry("minecraft:red_sandstone", 179),
		Map.entry("minecraft:red_sandstone_stairs", 180),
		Map.entry("minecraft:double_stone_slab2", 181),
		Map.entry("minecraft:stone_slab2", 182),
		Map.entry("minecraft:spruce_fence_gate", 183),
		Map.entry("minecraft:birch_fence_gate", 184),
		Map.entry("minecraft:jungle_fence_gate", 185),
		Map.entry("minecraft:dark_oak_fence_gate", 186),
		Map.entry("minecraft:acacia_fence_gate", 187),
		Map.entry("minecraft:spruce_fence", 188),
		Map.entry("minecraft:birch_fence", 189),
		Map.entry("minecraft:jungle_fence", 190),
		Map.entry("minecraft:dark_oak_fence", 191),
		Map.entry("minecraft:acacia_fence", 192),
		Map.entry("minecraft:spruce_door", 193),
		Map.entry("minecraft:birch_door", 194),
		Map.entry("minecraft:jungle_door", 195),
		Map.entry("minecraft:acacia_door", 196),
		Map.entry("minecraft:dark_oak_door", 197),
		Map.entry("minecraft:end_rod", 198),
		Map.entry("minecraft:chorus_plant", 199),
		Map.entry("minecraft:chorus_flower", 200),
		Map.entry("minecraft:purpur_block", 201),
		Map.entry("minecraft:purpur_pillar", 202),
		Map.entry("minecraft:purpur_stairs", 203),
		Map.entry("minecraft:purpur_double_slab", 204),
		Map.entry("minecraft:purpur_slab", 205),
		Map.entry("minecraft:end_bricks", 206),
		Map.entry("minecraft:beetroots", 207),
		Map.entry("minecraft:grass_path", 208),
		Map.entry("minecraft:end_gateway", 209),
		Map.entry("minecraft:repeating_command_block", 210),
		Map.entry("minecraft:chain_command_block", 211),
		Map.entry("minecraft:frosted_ice", 212),
		Map.entry("minecraft:magma", 213),
		Map.entry("minecraft:nether_wart_block", 214),
		Map.entry("minecraft:red_nether_brick", 215),
		Map.entry("minecraft:bone_block", 216),
		Map.entry("minecraft:structure_void", 217),
		Map.entry("minecraft:observer", 218),
		Map.entry("minecraft:white_shulker_box", 219),
		Map.entry("minecraft:orange_shulker_box", 220),
		Map.entry("minecraft:magenta_shulker_box", 221),
		Map.entry("minecraft:light_blue_shulker_box", 222),
		Map.entry("minecraft:yellow_shulker_box", 223),
		Map.entry("minecraft:lime_shulker_box", 224),
		Map.entry("minecraft:pink_shulker_box", 225),
		Map.entry("minecraft:gray_shulker_box", 226),
		Map.entry("minecraft:silver_shulker_box", 227),
		Map.entry("minecraft:cyan_shulker_box", 228),
		Map.entry("minecraft:purple_shulker_box", 229),
		Map.entry("minecraft:blue_shulker_box", 230),
		Map.entry("minecraft:brown_shulker_box", 231),
		Map.entry("minecraft:green_shulker_box", 232),
		Map.entry("minecraft:red_shulker_box", 233),
		Map.entry("minecraft:black_shulker_box", 234),
		Map.entry("minecraft:white_glazed_terracotta", 235),
		Map.entry("minecraft:orange_glazed_terracotta", 236),
		Map.entry("minecraft:magenta_glazed_terracotta", 237),
		Map.entry("minecraft:light_blue_glazed_terracotta", 238),
		Map.entry("minecraft:yellow_glazed_terracotta", 239),
		Map.entry("minecraft:lime_glazed_terracotta", 240),
		Map.entry("minecraft:pink_glazed_terracotta", 241),
		Map.entry("minecraft:gray_glazed_terracotta", 242),
		Map.entry("minecraft:silver_glazed_terracotta", 243),
		Map.entry("minecraft:cyan_glazed_terracotta", 244),
		Map.entry("minecraft:purple_glazed_terracotta", 245),
		Map.entry("minecraft:blue_glazed_terracotta", 246),
		Map.entry("minecraft:brown_glazed_terracotta", 247),
		Map.entry("minecraft:green_glazed_terracotta", 248),
		Map.entry("minecraft:red_glazed_terracotta", 249),
		Map.entry("minecraft:black_glazed_terracotta", 250),
		Map.entry("minecraft:concrete", 251),
		Map.entry("minecraft:concrete_powder", 252),
		Map.entry("minecraft:structure_block", 255)
	);

	public EntityBlockStateFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	public static int getNumericalBlockId(String blockId) {
		Integer numericId = BLOCK_NAME_TO_ID.get(blockId);
		return numericId == null ? 0 : numericId;
	}

	public TypeRewriteRule makeRule() {
		Function<Typed<?>, Typed<?>> minecartFixer =
				minecart -> mergeIdAndData(minecart, "DisplayTile", "DisplayData", "DisplayState");
		Function<Typed<?>, Typed<?>> arrowFixer =
				arrow -> mergeIdAndData(arrow, "inTile", "inData", "inBlockState");
		Type<Pair<Either<Pair<String, Either<Integer, String>>, Unit>, Dynamic<?>>> projectileType = DSL.and(
				DSL.optional(
						DSL.field(
								"inTile",
								DSL.named(
										TypeReferences.BLOCK_NAME.typeName(),
										DSL.or(DSL.intType(), IdentifierNormalizingSchema.getIdentifierType())
								)
						)
				),
				DSL.remainderType()
		);
		Function<Typed<?>, Typed<?>> projectileFixer =
				projectile -> projectile.update(projectileType.finder(), DSL.remainderType(), Pair::getSecond);

		return fixTypeEverywhereTyped(
				"EntityBlockStateFix",
				getInputSchema().getType(TypeReferences.ENTITY),
				getOutputSchema().getType(TypeReferences.ENTITY),
				entity -> {
					entity = useFunction(entity, "minecraft:falling_block", this::fixFallingBlock);
					entity = useFunction(
							entity,
							"minecraft:enderman",
							enderman -> mergeIdAndData(enderman, "carried", "carriedData", "carriedBlockState")
					);
					entity = useFunction(entity, "minecraft:arrow", arrowFixer);
					entity = useFunction(entity, "minecraft:spectral_arrow", arrowFixer);
					entity = useFunction(entity, "minecraft:egg", projectileFixer);
					entity = useFunction(entity, "minecraft:ender_pearl", projectileFixer);
					entity = useFunction(entity, "minecraft:fireball", projectileFixer);
					entity = useFunction(entity, "minecraft:potion", projectileFixer);
					entity = useFunction(entity, "minecraft:small_fireball", projectileFixer);
					entity = useFunction(entity, "minecraft:snowball", projectileFixer);
					entity = useFunction(entity, "minecraft:wither_skull", projectileFixer);
					entity = useFunction(entity, "minecraft:xp_bottle", projectileFixer);
					entity = useFunction(entity, "minecraft:commandblock_minecart", minecartFixer);
					entity = useFunction(entity, "minecraft:minecart", minecartFixer);
					entity = useFunction(entity, "minecraft:chest_minecart", minecartFixer);
					entity = useFunction(entity, "minecraft:furnace_minecart", minecartFixer);
					entity = useFunction(entity, "minecraft:tnt_minecart", minecartFixer);
					entity = useFunction(entity, "minecraft:hopper_minecart", minecartFixer);
					return useFunction(entity, "minecraft:spawner_minecart", minecartFixer);
				}
		);
	}

	private Typed<?> fixFallingBlock(Typed<?> fallingBlock) {
		Type<Either<Pair<String, Either<Integer, String>>, Unit>> blockIdType = DSL.optional(
				DSL.field(
						"Block",
						DSL.named(
								TypeReferences.BLOCK_NAME.typeName(),
								DSL.or(DSL.intType(), IdentifierNormalizingSchema.getIdentifierType())
						)
				)
		);
		Type<Either<Pair<String, Dynamic<?>>, Unit>> blockStateType = DSL.optional(
				DSL.field("BlockState", DSL.named(TypeReferences.BLOCK_STATE.typeName(), DSL.remainderType()))
		);
		Dynamic<?> remainder = (Dynamic<?>) fallingBlock.get(DSL.remainderFinder());

		return fallingBlock.update(
				blockIdType.finder(),
				blockStateType,
				state -> {
					int numericId = (Integer) state.map(
							pair -> (Integer) ((Either<?, ?>) pair.getSecond()).map(
									id -> id,
									id -> EntityBlockStateFix.getNumericalBlockId((String) id)
							),
							unit -> {
								Optional<Number> tileId = remainder.get("TileID").asNumber().result();
								return tileId
										.map(Number::intValue)
										.orElseGet(() -> remainder.get("Tile").asByte((byte) 0) & 0xFF);
							}
					);
					int data = remainder.get("Data").asInt(0) & 15;
					return Either.left(Pair.of(
							TypeReferences.BLOCK_STATE.typeName(),
							BlockStateFlattening.lookupState(numericId << 4 | data)
					));
				}
		).set(DSL.remainderFinder(), remainder.remove("Data").remove("TileID").remove("Tile"));
	}

	private Typed<?> mergeIdAndData(Typed<?> entity, String oldIdKey, String oldDataKey, String newStateKey) {
		Type<Pair<String, Either<Integer, String>>> blockIdType = DSL.field(
				oldIdKey,
				DSL.named(
						TypeReferences.BLOCK_NAME.typeName(),
						DSL.or(DSL.intType(), IdentifierNormalizingSchema.getIdentifierType())
				)
		);
		Type<Pair<String, Dynamic<?>>> blockStateType =
				DSL.field(newStateKey, DSL.named(TypeReferences.BLOCK_STATE.typeName(), DSL.remainderType()));
		Dynamic<?> remainder = (Dynamic<?>) entity.getOrCreate(DSL.remainderFinder());

		return entity.update(
				blockIdType.finder(),
				blockStateType,
				state -> {
					int numericId = (Integer) ((Either<?, ?>) state.getSecond()).map(
							id -> id,
							id -> EntityBlockStateFix.getNumericalBlockId((String) id)
					);
					int data = remainder.get(oldDataKey).asInt(0) & 15;
					return Pair.of(
							TypeReferences.BLOCK_STATE.typeName(),
							BlockStateFlattening.lookupState(numericId << 4 | data)
					);
				}
		).set(DSL.remainderFinder(), remainder.remove(oldDataKey));
	}

	private Typed<?> useFunction(Typed<?> entity, String entityId, Function<Typed<?>, Typed<?>> fixer) {
		Type<?> inputType = getInputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		Type<?> outputType = getOutputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		return entity.updateTyped(DSL.namedChoice(entityId, inputType), outputType, fixer);
	}
}
