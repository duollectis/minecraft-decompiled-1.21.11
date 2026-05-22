package net.minecraft.entity;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Контракт для сущностей, способных носить снаряжение в слотах.
 * Предоставляет логику экипировки предметов из лут-таблиц с учётом шансов выпадения.
 */
public interface EquipmentHolder {

	void equipStack(EquipmentSlot slot, ItemStack stack);

	ItemStack getEquippedStack(EquipmentSlot slot);

	void setEquipmentDropChance(EquipmentSlot slot, float dropChance);

	default void setEquipmentFromTable(EquipmentTable equipmentTable, LootWorldContext parameters) {
		setEquipmentFromTable(equipmentTable.lootTable(), parameters, equipmentTable.slotDropChances());
	}

	default void setEquipmentFromTable(
		RegistryKey<LootTable> lootTable,
		LootWorldContext parameters,
		Map<EquipmentSlot, Float> slotDropChances
	) {
		setEquipmentFromTable(lootTable, parameters, 0L, slotDropChances);
	}

	/**
	 * Экипирует сущность предметами из лут-таблицы с заданным сидом и шансами выпадения.
	 * Каждый слот может быть занят только одним предметом — повторные попытки занять уже
	 * занятый слот игнорируются через список {@code occupiedSlots}.
	 *
	 * @param lootTable      ключ лут-таблицы для генерации предметов
	 * @param parameters     контекст мира для генерации лута
	 * @param seed           сид для воспроизводимой генерации (0 = случайный)
	 * @param slotDropChances шансы выпадения по слотам, переопределяющие дефолтные
	 */
	default void setEquipmentFromTable(
		RegistryKey<LootTable> lootTable,
		LootWorldContext parameters,
		long seed,
		Map<EquipmentSlot, Float> slotDropChances
	) {
		LootTable resolvedTable = parameters.getWorld().getServer().getReloadableRegistries().getLootTable(lootTable);
		if (resolvedTable == LootTable.EMPTY) {
			return;
		}

		List<ItemStack> generatedItems = resolvedTable.generateLoot(parameters, seed);
		List<EquipmentSlot> occupiedSlots = new ArrayList<>();

		for (ItemStack itemStack : generatedItems) {
			EquipmentSlot targetSlot = getSlotForStack(itemStack, occupiedSlots);
			if (targetSlot == null) {
				continue;
			}

			ItemStack slicedStack = targetSlot.split(itemStack);
			equipStack(targetSlot, slicedStack);

			Float dropChance = slotDropChances.get(targetSlot);
			if (dropChance != null) {
				setEquipmentDropChance(targetSlot, dropChance);
			}

			occupiedSlots.add(targetSlot);
		}
	}

	/**
	 * Определяет подходящий слот для предмета, исключая уже занятые слоты.
	 * Если предмет имеет компонент {@link EquippableComponent}, используется его слот.
	 * Иначе предмет помещается в {@link EquipmentSlot#MAINHAND}, если он свободен.
	 *
	 * @param stack         предмет для размещения
	 * @param slotBlacklist список уже занятых слотов
	 * @return подходящий слот или {@code null}, если подходящего нет
	 */
	default @Nullable EquipmentSlot getSlotForStack(ItemStack stack, List<EquipmentSlot> slotBlacklist) {
		if (stack.isEmpty()) {
			return null;
		}

		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		if (equippable != null) {
			EquipmentSlot slot = equippable.slot();
			return slotBlacklist.contains(slot) ? null : slot;
		}

		return slotBlacklist.contains(EquipmentSlot.MAINHAND) ? null : EquipmentSlot.MAINHAND;
	}
}
