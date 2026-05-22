package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;
import java.util.function.Predicate;

/**
 * Утилитарный класс со статическими методами для работы с инвентарями:
 * сериализация/десериализация в NBT, извлечение стаков и массовое удаление предметов.
 */
public class Inventories {

	public static final String ITEMS_NBT_KEY = "Items";

	/**
	 * Извлекает часть стака из указанного слота списка, не превышая {@code amount}.
	 * Возвращает {@link ItemStack#EMPTY}, если слот невалиден или пуст.
	 *
	 * @param stacks список стаков
	 * @param slot   индекс слота
	 * @param amount количество предметов для извлечения
	 * @return извлечённый стак или {@link ItemStack#EMPTY}
	 */
	public static ItemStack splitStack(List<ItemStack> stacks, int slot, int amount) {
		boolean validSlot = slot >= 0 && slot < stacks.size() && !stacks.get(slot).isEmpty() && amount > 0;

		return validSlot ? stacks.get(slot).split(amount) : ItemStack.EMPTY;
	}

	/**
	 * Полностью извлекает стак из указанного слота, заменяя его на {@link ItemStack#EMPTY}.
	 * Возвращает {@link ItemStack#EMPTY}, если слот невалиден.
	 *
	 * @param stacks список стаков
	 * @param slot   индекс слота
	 * @return извлечённый стак или {@link ItemStack#EMPTY}
	 */
	public static ItemStack removeStack(List<ItemStack> stacks, int slot) {
		return slot >= 0 && slot < stacks.size() ? stacks.set(slot, ItemStack.EMPTY) : ItemStack.EMPTY;
	}

	/**
	 * Сериализует инвентарь в NBT, всегда записывая ключ {@link #ITEMS_NBT_KEY}
	 * даже если инвентарь пуст.
	 *
	 * @param view   целевое представление для записи
	 * @param stacks список стаков инвентаря
	 */
	public static void writeData(WriteView view, DefaultedList<ItemStack> stacks) {
		writeData(view, stacks, true);
	}

	/**
	 * Сериализует инвентарь в NBT. При {@code setIfEmpty = false} удаляет ключ
	 * {@link #ITEMS_NBT_KEY} из представления, если инвентарь пуст, что позволяет
	 * избежать записи пустых списков в NBT.
	 *
	 * @param view       целевое представление для записи
	 * @param stacks     список стаков инвентаря
	 * @param setIfEmpty если {@code false}, пустой инвентарь не записывается
	 */
	public static void writeData(WriteView view, DefaultedList<ItemStack> stacks, boolean setIfEmpty) {
		WriteView.ListAppender<StackWithSlot> listAppender = view.getListAppender(ITEMS_NBT_KEY, StackWithSlot.CODEC);

		for (int slot = 0; slot < stacks.size(); slot++) {
			ItemStack stack = stacks.get(slot);

			if (!stack.isEmpty()) {
				listAppender.add(new StackWithSlot(slot, stack));
			}
		}

		if (listAppender.isEmpty() && !setIfEmpty) {
			view.remove(ITEMS_NBT_KEY);
		}
	}

	/**
	 * Десериализует инвентарь из NBT. Слоты с невалидными индексами пропускаются,
	 * что обеспечивает безопасность при изменении размера инвентаря между версиями.
	 *
	 * @param view   источник данных для чтения
	 * @param stacks список стаков, в который будут записаны данные
	 */
	public static void readData(ReadView view, DefaultedList<ItemStack> stacks) {
		for (StackWithSlot stackWithSlot : view.getTypedListView(ITEMS_NBT_KEY, StackWithSlot.CODEC)) {
			if (stackWithSlot.isValidSlot(stacks.size())) {
				stacks.set(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	/**
	 * Удаляет предметы из инвентаря, соответствующие предикату, до указанного максимального количества.
	 * При {@code dryRun = true} только подсчитывает количество без фактического удаления.
	 *
	 * @param inventory   инвентарь, из которого удаляются предметы
	 * @param shouldRemove предикат отбора стаков для удаления
	 * @param maxCount    максимальное суммарное количество предметов для удаления; отрицательное — без ограничений
	 * @param dryRun      если {@code true}, удаление не производится — только подсчёт
	 * @return суммарное количество удалённых (или подлежащих удалению) предметов
	 */
	public static int remove(Inventory inventory, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
		int totalRemoved = 0;

		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			int removed = remove(stack, shouldRemove, maxCount - totalRemoved, dryRun);

			if (removed > 0 && !dryRun && stack.isEmpty()) {
				inventory.setStack(slot, ItemStack.EMPTY);
			}

			totalRemoved += removed;
		}

		return totalRemoved;
	}

	/**
	 * Удаляет предметы из одного стака, соответствующего предикату.
	 * При {@code dryRun = true} возвращает текущее количество предметов в стаке без изменений.
	 *
	 * @param stack        стак, из которого удаляются предметы
	 * @param shouldRemove предикат проверки стака
	 * @param maxCount     максимальное количество для удаления; отрицательное — без ограничений
	 * @param dryRun       если {@code true}, удаление не производится
	 * @return количество удалённых (или подлежащих удалению) предметов
	 */
	public static int remove(ItemStack stack, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
		if (stack.isEmpty() || !shouldRemove.test(stack)) {
			return 0;
		}

		if (dryRun) {
			return stack.getCount();
		}

		int toRemove = maxCount < 0 ? stack.getCount() : Math.min(maxCount, stack.getCount());
		stack.decrement(toRemove);

		return toRemove;
	}
}
