package net.minecraft.datafixer.schema;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.fix.BlockEntityIdFix;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 704, выполняющая нормализацию идентификаторов блок-сущностей:
 * переименовывает устаревшие строковые идентификаторы (например, {@code "Furnace"})
 * в современные namespaced-идентификаторы (например, {@code "minecraft:furnace"}).
 * Также применяет {@code ITEM_STACK_HOOK} для обновления тегов {@code BlockEntityTag}
 * и {@code EntityTag} внутри стеков предметов.
 */
public class Schema704 extends Schema {

	protected static final Map<String, String> BLOCK_RENAMES = Map.ofEntries(
		Map.entry("minecraft:furnace", "minecraft:furnace"),
		Map.entry("minecraft:lit_furnace", "minecraft:furnace"),
		Map.entry("minecraft:chest", "minecraft:chest"),
		Map.entry("minecraft:trapped_chest", "minecraft:chest"),
		Map.entry("minecraft:ender_chest", "minecraft:ender_chest"),
		Map.entry("minecraft:jukebox", "minecraft:jukebox"),
		Map.entry("minecraft:dispenser", "minecraft:dispenser"),
		Map.entry("minecraft:dropper", "minecraft:dropper"),
		Map.entry("minecraft:sign", "minecraft:sign"),
		Map.entry("minecraft:mob_spawner", "minecraft:mob_spawner"),
		Map.entry("minecraft:spawner", "minecraft:mob_spawner"),
		Map.entry("minecraft:noteblock", "minecraft:noteblock"),
		Map.entry("minecraft:brewing_stand", "minecraft:brewing_stand"),
		Map.entry("minecraft:enhanting_table", "minecraft:enchanting_table"),
		Map.entry("minecraft:command_block", "minecraft:command_block"),
		Map.entry("minecraft:beacon", "minecraft:beacon"),
		Map.entry("minecraft:skull", "minecraft:skull"),
		Map.entry("minecraft:daylight_detector", "minecraft:daylight_detector"),
		Map.entry("minecraft:hopper", "minecraft:hopper"),
		Map.entry("minecraft:banner", "minecraft:banner"),
		Map.entry("minecraft:flower_pot", "minecraft:flower_pot"),
		Map.entry("minecraft:repeating_command_block", "minecraft:command_block"),
		Map.entry("minecraft:chain_command_block", "minecraft:command_block"),
		Map.entry("minecraft:shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:white_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:orange_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:magenta_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:light_blue_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:yellow_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:lime_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:pink_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:gray_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:silver_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:cyan_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:purple_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:blue_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:brown_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:green_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:red_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:black_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:light_gray_shulker_box", "minecraft:shulker_box"),
		Map.entry("minecraft:bed", "minecraft:bed"),
		Map.entry("minecraft:white_banner", "minecraft:banner"),
		Map.entry("minecraft:orange_banner", "minecraft:banner"),
		Map.entry("minecraft:magenta_banner", "minecraft:banner"),
		Map.entry("minecraft:light_blue_banner", "minecraft:banner"),
		Map.entry("minecraft:yellow_banner", "minecraft:banner"),
		Map.entry("minecraft:lime_banner", "minecraft:banner"),
		Map.entry("minecraft:pink_banner", "minecraft:banner"),
		Map.entry("minecraft:gray_banner", "minecraft:banner"),
		Map.entry("minecraft:silver_banner", "minecraft:banner"),
		Map.entry("minecraft:light_gray_banner", "minecraft:banner"),
		Map.entry("minecraft:cyan_banner", "minecraft:banner"),
		Map.entry("minecraft:purple_banner", "minecraft:banner"),
		Map.entry("minecraft:blue_banner", "minecraft:banner"),
		Map.entry("minecraft:brown_banner", "minecraft:banner"),
		Map.entry("minecraft:green_banner", "minecraft:banner"),
		Map.entry("minecraft:red_banner", "minecraft:banner"),
		Map.entry("minecraft:black_banner", "minecraft:banner"),
		Map.entry("minecraft:standing_sign", "minecraft:sign"),
		Map.entry("minecraft:wall_sign", "minecraft:sign"),
		Map.entry("minecraft:piston_head", "minecraft:piston"),
		Map.entry("minecraft:daylight_detector_inverted", "minecraft:daylight_detector"),
		Map.entry("minecraft:unpowered_comparator", "minecraft:comparator"),
		Map.entry("minecraft:powered_comparator", "minecraft:comparator"),
		Map.entry("minecraft:wall_banner", "minecraft:banner"),
		Map.entry("minecraft:standing_banner", "minecraft:banner"),
		Map.entry("minecraft:structure_block", "minecraft:structure_block"),
		Map.entry("minecraft:end_portal", "minecraft:end_portal"),
		Map.entry("minecraft:end_gateway", "minecraft:end_gateway"),
		Map.entry("minecraft:shield", "minecraft:banner"),
		Map.entry("minecraft:white_bed", "minecraft:bed"),
		Map.entry("minecraft:orange_bed", "minecraft:bed"),
		Map.entry("minecraft:magenta_bed", "minecraft:bed"),
		Map.entry("minecraft:light_blue_bed", "minecraft:bed"),
		Map.entry("minecraft:yellow_bed", "minecraft:bed"),
		Map.entry("minecraft:lime_bed", "minecraft:bed"),
		Map.entry("minecraft:pink_bed", "minecraft:bed"),
		Map.entry("minecraft:gray_bed", "minecraft:bed"),
		Map.entry("minecraft:silver_bed", "minecraft:bed"),
		Map.entry("minecraft:light_gray_bed", "minecraft:bed"),
		Map.entry("minecraft:cyan_bed", "minecraft:bed"),
		Map.entry("minecraft:purple_bed", "minecraft:bed"),
		Map.entry("minecraft:blue_bed", "minecraft:bed"),
		Map.entry("minecraft:brown_bed", "minecraft:bed"),
		Map.entry("minecraft:green_bed", "minecraft:bed"),
		Map.entry("minecraft:red_bed", "minecraft:bed"),
		Map.entry("minecraft:black_bed", "minecraft:bed"),
		Map.entry("minecraft:oak_sign", "minecraft:sign"),
		Map.entry("minecraft:spruce_sign", "minecraft:sign"),
		Map.entry("minecraft:birch_sign", "minecraft:sign"),
		Map.entry("minecraft:jungle_sign", "minecraft:sign"),
		Map.entry("minecraft:acacia_sign", "minecraft:sign"),
		Map.entry("minecraft:dark_oak_sign", "minecraft:sign"),
		Map.entry("minecraft:crimson_sign", "minecraft:sign"),
		Map.entry("minecraft:warped_sign", "minecraft:sign"),
		Map.entry("minecraft:skeleton_skull", "minecraft:skull"),
		Map.entry("minecraft:wither_skeleton_skull", "minecraft:skull"),
		Map.entry("minecraft:zombie_head", "minecraft:skull"),
		Map.entry("minecraft:player_head", "minecraft:skull"),
		Map.entry("minecraft:creeper_head", "minecraft:skull"),
		Map.entry("minecraft:dragon_head", "minecraft:skull"),
		Map.entry("minecraft:barrel", "minecraft:barrel"),
		Map.entry("minecraft:conduit", "minecraft:conduit"),
		Map.entry("minecraft:smoker", "minecraft:smoker"),
		Map.entry("minecraft:blast_furnace", "minecraft:blast_furnace"),
		Map.entry("minecraft:lectern", "minecraft:lectern"),
		Map.entry("minecraft:bell", "minecraft:bell"),
		Map.entry("minecraft:jigsaw", "minecraft:jigsaw"),
		Map.entry("minecraft:campfire", "minecraft:campfire"),
		Map.entry("minecraft:bee_nest", "minecraft:beehive"),
		Map.entry("minecraft:beehive", "minecraft:beehive"),
		Map.entry("minecraft:sculk_sensor", "minecraft:sculk_sensor"),
		Map.entry("minecraft:decorated_pot", "minecraft:decorated_pot"),
		Map.entry("minecraft:crafter", "minecraft:crafter")
	);
	protected static final HookFunction ITEM_STACK_HOOK = new HookFunction() {
		public <T> T apply(DynamicOps<T> ops, T value) {
			return Schema99.updateBlockEntityTags(
					new Dynamic<>(ops, value),
					Schema704.BLOCK_RENAMES,
					Schema99.ENTITY_TO_BLOCK_ENTITY_RENAMES
			);
		}
	};

	public Schema704(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Type<?> getChoiceType(TypeReference type, String choiceName) {
		return Objects.equals(type.typeName(), TypeReferences.BLOCK_ENTITY.typeName())
		       ? super.getChoiceType(type, IdentifierNormalizingSchema.normalize(choiceName))
		       : super.getChoiceType(type, choiceName);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		BlockEntityIdFix.RENAMED_BLOCK_ENTITIES
				.forEach((string, string2) -> map.put(
						string2,
						Objects.requireNonNull(map.remove(string), () -> "Didn't find " + string + " in schema")
				));
		return map;
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				true,
				TypeReferences.BLOCK_ENTITY,
				() -> DSL.optionalFields(
						"components",
						TypeReferences.DATA_COMPONENTS.in(schema),
						DSL.taggedChoiceLazy("id", IdentifierNormalizingSchema.getIdentifierType(), blockEntityTypes)
				)
		);
		schema.registerType(
				true,
				TypeReferences.ITEM_STACK,
				() -> DSL.hook(
						DSL.optionalFields(
								"id",
								TypeReferences.ITEM_NAME.in(schema),
								"tag",
								Schema99.createItemTagTypeTemplate(schema)
						), ITEM_STACK_HOOK, HookFunction.IDENTITY
				)
		);
	}
}
