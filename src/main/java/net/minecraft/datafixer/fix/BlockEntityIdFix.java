package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;

/**
 * Переименовывает устаревшие строковые ID блок-сущностей в новый формат с namespace
 * (например, {@code "Chest"} → {@code "minecraft:chest"}).
 * Применяется как к самим блок-сущностям, так и к ItemStack-тегам предметов.
 */
public class BlockEntityIdFix extends DataFix {

	public static final Map<String, String> RENAMED_BLOCK_ENTITIES = Map.ofEntries(
			Map.entry("Airportal", "minecraft:end_portal"),
			Map.entry("Banner", "minecraft:banner"),
			Map.entry("Beacon", "minecraft:beacon"),
			Map.entry("Cauldron", "minecraft:brewing_stand"),
			Map.entry("Chest", "minecraft:chest"),
			Map.entry("Comparator", "minecraft:comparator"),
			Map.entry("Control", "minecraft:command_block"),
			Map.entry("DLDetector", "minecraft:daylight_detector"),
			Map.entry("Dropper", "minecraft:dropper"),
			Map.entry("EnchantTable", "minecraft:enchanting_table"),
			Map.entry("EndGateway", "minecraft:end_gateway"),
			Map.entry("EnderChest", "minecraft:ender_chest"),
			Map.entry("FlowerPot", "minecraft:flower_pot"),
			Map.entry("Furnace", "minecraft:furnace"),
			Map.entry("Hopper", "minecraft:hopper"),
			Map.entry("MobSpawner", "minecraft:mob_spawner"),
			Map.entry("Music", "minecraft:noteblock"),
			Map.entry("Piston", "minecraft:piston"),
			Map.entry("RecordPlayer", "minecraft:jukebox"),
			Map.entry("Sign", "minecraft:sign"),
			Map.entry("Skull", "minecraft:skull"),
			Map.entry("Structure", "minecraft:structure_block"),
			Map.entry("Trap", "minecraft:dispenser")
	);

	public BlockEntityIdFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@SuppressWarnings("unchecked")
	public TypeRewriteRule makeRule() {
		Type<?> inputItemType = getInputSchema().getType(TypeReferences.ITEM_STACK);
		Type<?> outputItemType = getOutputSchema().getType(TypeReferences.ITEM_STACK);
		TaggedChoiceType<String> inputChoiceType =
				(TaggedChoiceType<String>) getInputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY);
		TaggedChoiceType<String> outputChoiceType =
				(TaggedChoiceType<String>) getOutputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY);

		return TypeRewriteRule.seq(
				convertUnchecked("item stack block entity name hook converter", inputItemType, outputItemType),
				fixTypeEverywhere(
						"BlockEntityIdFix",
						inputChoiceType,
						outputChoiceType,
						dynamicOps -> pair -> pair.mapFirst(
								oldName -> RENAMED_BLOCK_ENTITIES.getOrDefault(oldName, oldName)
						)
				)
		);
	}
}
