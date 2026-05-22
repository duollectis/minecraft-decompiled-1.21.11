package net.minecraft.datafixer.fix;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Мигрирует броню лошади из устаревшего поля (например, {@code ArmorItem})
 * в новое поле {@code body_armor_item}. Если {@code removeOldArmor} установлен,
 * также очищает слот брони (индекс 2) в {@code ArmorItems} и сбрасывает
 * соответствующий шанс выпадения в {@code ArmorDropChances}.
 */
public class HorseArmorFix extends ChoiceWriteReadFix {

	private static final int CHEST_ARMOR_SLOT = 2;
	private static final float DEFAULT_BODY_ARMOR_DROP_CHANCE = 2.0F;
	private static final float DEFAULT_ARMOR_DROP_CHANCE = 0.085F;

	private final String oldNbtKey;
	private final boolean removeOldArmor;

	public HorseArmorFix(Schema outputSchema, String entityId, String oldNbtKey, boolean removeOldArmor) {
		super(outputSchema, true, "Horse armor fix for " + entityId, TypeReferences.ENTITY, entityId);
		this.oldNbtKey = oldNbtKey;
		this.removeOldArmor = removeOldArmor;
	}

	@Override
	protected <T> Dynamic<T> transform(Dynamic<T> data) {
		Optional<? extends Dynamic<?>> armorItem = data.get(oldNbtKey).result();

		if (armorItem.isEmpty()) {
			return data;
		}

		Dynamic<?> armor = armorItem.get();
		Dynamic<T> result = data.remove(oldNbtKey);

		if (removeOldArmor) {
			result = result.update(
				"ArmorItems",
				armorItems -> armorItems.createList(
					Streams.mapWithIndex(
						armorItems.asStream(),
						(item, slot) -> slot == CHEST_ARMOR_SLOT ? item.emptyMap() : item
					)
				)
			);

			result = result.update(
				"ArmorDropChances",
				dropChances -> dropChances.createList(
					Streams.mapWithIndex(
						dropChances.asStream(),
						(chance, slot) -> slot == CHEST_ARMOR_SLOT
							? chance.createFloat(DEFAULT_ARMOR_DROP_CHANCE)
							: chance
					)
				)
			);
		}

		return result
			.set("body_armor_item", armor)
			.set("body_armor_drop_chance", data.createFloat(DEFAULT_BODY_ARMOR_DROP_CHANCE));
	}
}
