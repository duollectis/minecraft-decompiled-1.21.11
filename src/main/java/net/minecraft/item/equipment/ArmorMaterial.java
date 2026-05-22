package net.minecraft.item.equipment;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Материал брони: определяет прочность, защиту по слотам, зачарование,
 * звук надевания, стойкость, сопротивление отбрасыванию и ресурс для ремонта.
 */
public record ArmorMaterial(
		int durability,
		Map<EquipmentType, Integer> defense,
		int enchantmentValue,
		RegistryEntry<SoundEvent> equipSound,
		float toughness,
		float knockbackResistance,
		TagKey<Item> repairIngredient,
		RegistryKey<EquipmentAsset> assetId
) {

	/**
	 * Создаёт компонент модификаторов атрибутов для указанного слота снаряжения.
	 * Всегда добавляет ARMOR и ARMOR_TOUGHNESS; KNOCKBACK_RESISTANCE — только если значение > 0.
	 *
	 * @param equipmentType тип слота снаряжения (шлем, нагрудник и т.д.)
	 * @return компонент с модификаторами атрибутов для данного материала и слота
	 */
	public AttributeModifiersComponent createAttributeModifiers(EquipmentType equipmentType) {
		int defenseValue = defense.getOrDefault(equipmentType, 0);
		AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
		AttributeModifierSlot slot = AttributeModifierSlot.forEquipmentSlot(equipmentType.getEquipmentSlot());
		Identifier modifierId = Identifier.ofVanilla("armor." + equipmentType.getName());

		builder.add(
				EntityAttributes.ARMOR,
				new EntityAttributeModifier(modifierId, defenseValue, EntityAttributeModifier.Operation.ADD_VALUE),
				slot
		);
		builder.add(
				EntityAttributes.ARMOR_TOUGHNESS,
				new EntityAttributeModifier(modifierId, toughness, EntityAttributeModifier.Operation.ADD_VALUE),
				slot
		);

		if (knockbackResistance > 0.0F) {
			builder.add(
					EntityAttributes.KNOCKBACK_RESISTANCE,
					new EntityAttributeModifier(modifierId, knockbackResistance, EntityAttributeModifier.Operation.ADD_VALUE),
					slot
			);
		}

		return builder.build();
	}
}
