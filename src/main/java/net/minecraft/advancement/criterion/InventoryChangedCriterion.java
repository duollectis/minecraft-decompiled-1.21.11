package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Критерий: изменился инвентарь игрока.
 * Проверяет состояние слотов инвентаря и наличие конкретных предметов.
 */
public class InventoryChangedCriterion extends AbstractCriterion<InventoryChangedCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	/**
	 * Подсчитывает статистику слотов инвентаря и передаёт в условия.
	 * Разделяет слоты на: полные (стак максимального размера), пустые и занятые.
	 */
	public void trigger(ServerPlayerEntity player, PlayerInventory inventory, ItemStack stack) {
		int fullSlots = 0;
		int emptySlots = 0;
		int occupiedSlots = 0;

		for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++) {
			ItemStack slotStack = inventory.getStack(slotIndex);

			if (slotStack.isEmpty()) {
				emptySlots++;
			} else {
				occupiedSlots++;

				if (slotStack.getCount() >= slotStack.getMaxCount()) {
					fullSlots++;
				}
			}
		}

		trigger(player, inventory, stack, fullSlots, emptySlots, occupiedSlots);
	}

	private void trigger(
			ServerPlayerEntity player,
			PlayerInventory inventory,
			ItemStack stack,
			int full,
			int empty,
			int occupied
	) {
		trigger(player, conditions -> conditions.matches(inventory, stack, full, empty, occupied));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Slots slots,
			List<ItemPredicate> items
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Slots.CODEC
								.optionalFieldOf("slots", Slots.ANY)
								.forGetter(Conditions::slots),
						ItemPredicate.CODEC
								.listOf()
								.optionalFieldOf("items", List.of())
								.forGetter(Conditions::items)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> items(ItemPredicate.Builder... items) {
			return items(Stream.of(items).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
		}

		public static AdvancementCriterion<Conditions> items(ItemPredicate... items) {
			return Criteria.INVENTORY_CHANGED.create(new Conditions(Optional.empty(), Slots.ANY, List.of(items)));
		}

		public static AdvancementCriterion<Conditions> items(ItemConvertible... items) {
			ItemPredicate[] predicates = new ItemPredicate[items.length];

			for (int index = 0; index < items.length; index++) {
				predicates[index] = new ItemPredicate(
						Optional.of(RegistryEntryList.of(items[index].asItem().getRegistryEntry())),
						NumberRange.IntRange.ANY,
						ComponentsPredicate.EMPTY
				);
			}

			return items(predicates);
		}

		/**
		 * Проверяет инвентарь на соответствие условиям.
		 * При одном предикате предмета — быстрая проверка только изменённого стака.
		 * При нескольких — полный перебор инвентаря с удалением совпавших предикатов.
		 */
		public boolean matches(PlayerInventory inventory, ItemStack stack, int full, int empty, int occupied) {
			if (!slots.test(full, empty, occupied)) {
				return false;
			}

			if (items.isEmpty()) {
				return true;
			}

			if (items.size() == 1) {
				return !stack.isEmpty() && items.get(0).test(stack);
			}

			List<ItemPredicate> remaining = new ObjectArrayList<>(items);

			for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++) {
				if (remaining.isEmpty()) {
					return true;
				}

				ItemStack slotStack = inventory.getStack(slotIndex);

				if (!slotStack.isEmpty()) {
					remaining.removeIf(predicate -> predicate.test(slotStack));
				}
			}

			return remaining.isEmpty();
		}

		public record Slots(NumberRange.IntRange occupied, NumberRange.IntRange full, NumberRange.IntRange empty) {

			public static final Codec<Slots> CODEC = RecordCodecBuilder.create(
					instance -> instance.group(
							NumberRange.IntRange.CODEC
									.optionalFieldOf("occupied", NumberRange.IntRange.ANY)
									.forGetter(Slots::occupied),
							NumberRange.IntRange.CODEC
									.optionalFieldOf("full", NumberRange.IntRange.ANY)
									.forGetter(Slots::full),
							NumberRange.IntRange.CODEC
									.optionalFieldOf("empty", NumberRange.IntRange.ANY)
									.forGetter(Slots::empty)
					).apply(instance, Slots::new)
			);

			public static final Slots ANY = new Slots(
					NumberRange.IntRange.ANY,
					NumberRange.IntRange.ANY,
					NumberRange.IntRange.ANY
			);

			public boolean test(int full, int empty, int occupied) {
				if (!this.full.test(full)) {
					return false;
				}

				if (!this.empty.test(empty)) {
					return false;
				}

				return this.occupied.test(occupied);
			}
		}
	}
}
