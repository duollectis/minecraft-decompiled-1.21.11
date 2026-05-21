package net.minecraft.item.equipment;

import com.google.common.collect.Maps;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * {@code ArmorMaterials}.
 */
public interface ArmorMaterials {

	ArmorMaterial LEATHER = new ArmorMaterial(
			5,
			createDefenseMap(1, 2, 3, 1, 3),
			15,
			SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_LEATHER_ARMOR,
			EquipmentAssetKeys.LEATHER
	);

	ArmorMaterial COPPER = new ArmorMaterial(
			11,
			createDefenseMap(1, 3, 4, 2, 4),
			8,
			SoundEvents.ITEM_ARMOR_EQUIP_COPPER,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_COPPER_ARMOR,
			EquipmentAssetKeys.COPPER
	);

	ArmorMaterial CHAIN = new ArmorMaterial(
			15,
			createDefenseMap(1, 4, 5, 2, 4),
			12,
			SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_CHAIN_ARMOR,
			EquipmentAssetKeys.CHAINMAIL
	);

	ArmorMaterial IRON = new ArmorMaterial(
			15,
			createDefenseMap(2, 5, 6, 2, 5),
			9,
			SoundEvents.ITEM_ARMOR_EQUIP_IRON,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_IRON_ARMOR,
			EquipmentAssetKeys.IRON
	);

	ArmorMaterial GOLD = new ArmorMaterial(
			7,
			createDefenseMap(1, 3, 5, 2, 7),
			25,
			SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_GOLD_ARMOR,
			EquipmentAssetKeys.GOLD
	);

	ArmorMaterial DIAMOND = new ArmorMaterial(
			33,
			createDefenseMap(3, 6, 8, 3, 11),
			10,
			SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
			2.0F,
			0.0F,
			ItemTags.REPAIRS_DIAMOND_ARMOR,
			EquipmentAssetKeys.DIAMOND
	);

	ArmorMaterial TURTLE_SCUTE = new ArmorMaterial(
			25,
			createDefenseMap(2, 5, 6, 2, 5),
			9,
			SoundEvents.ITEM_ARMOR_EQUIP_TURTLE,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_TURTLE_HELMET,
			EquipmentAssetKeys.TURTLE_SCUTE
	);

	ArmorMaterial NETHERITE = new ArmorMaterial(
			37,
			createDefenseMap(3, 6, 8, 3, 19),
			15,
			SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
			3.0F,
			0.1F,
			ItemTags.REPAIRS_NETHERITE_ARMOR,
			EquipmentAssetKeys.NETHERITE
	);

	ArmorMaterial ARMADILLO_SCUTE = new ArmorMaterial(
			4,
			createDefenseMap(3, 6, 8, 3, 11),
			10,
			SoundEvents.ITEM_ARMOR_EQUIP_WOLF,
			0.0F,
			0.0F,
			ItemTags.REPAIRS_WOLF_ARMOR,
			EquipmentAssetKeys.ARMADILLO_SCUTE
	);

	static Map<EquipmentType, Integer> createDefenseMap(
			int bootsDefense,
			int leggingsDefense,
			int chestplateDefense,
			int helmetDefense,
			int bodyDefense
	) {
		return Maps.newEnumMap(
				Map.of(
						EquipmentType.BOOTS,
						bootsDefense,
						EquipmentType.LEGGINGS,
						leggingsDefense,
						EquipmentType.CHESTPLATE,
						chestplateDefense,
						EquipmentType.HELMET,
						helmetDefense,
						EquipmentType.BODY,
						bodyDefense
				)
		);
	}
}
