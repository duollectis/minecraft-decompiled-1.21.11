package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.inventory.StackReference;
import net.minecraft.inventory.StackReferenceGetter;
import net.minecraft.predicate.item.ItemPredicate;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Предикат для проверки содержимого слотов инвентаря сущности.
 * Для каждого диапазона слотов проверяет, что хотя бы один слот содержит подходящий предмет.
 */
public record SlotsPredicate(Map<SlotRange, ItemPredicate> slots) {

	public static final Codec<SlotsPredicate> CODEC = Codec
			.unboundedMap(SlotRanges.CODEC, ItemPredicate.CODEC)
			.xmap(SlotsPredicate::new, SlotsPredicate::slots);

	public boolean matches(StackReferenceGetter stackReferenceGetter) {
		for (Entry<SlotRange, ItemPredicate> entry : slots.entrySet()) {
			if (!matchesAnySlot(stackReferenceGetter, entry.getValue(), entry.getKey().getSlotIds())) {
				return false;
			}
		}

		return true;
	}

	private static boolean matchesAnySlot(
			StackReferenceGetter stackReferenceGetter,
			ItemPredicate itemPredicate,
			IntList slotIds
	) {
		for (int slotIndex = 0; slotIndex < slotIds.size(); slotIndex++) {
			int slotId = slotIds.getInt(slotIndex);
			StackReference stackReference = stackReferenceGetter.getStackReference(slotId);

			if (stackReference != null && itemPredicate.test(stackReference.get())) {
				return true;
			}
		}

		return false;
	}
}
