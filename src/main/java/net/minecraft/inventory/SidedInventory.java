package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Инвентарь с поддержкой направленного доступа к слотам.
 * Используется хопперами и другими механизмами автоматической передачи предметов,
 * которым важно знать, с какой стороны блока происходит взаимодействие.
 */
public interface SidedInventory extends Inventory {

	/**
	 * Возвращает массив индексов слотов, доступных со стороны {@code side}.
	 *
	 * @param side сторона блока, с которой происходит обращение
	 * @return массив доступных индексов слотов
	 */
	int[] getAvailableSlots(Direction side);

	/**
	 * Проверяет, можно ли вставить {@code stack} в слот {@code slot} со стороны {@code dir}.
	 *
	 * @param slot  индекс целевого слота
	 * @param stack стак для вставки
	 * @param dir   направление вставки; {@code null} означает отсутствие конкретной стороны
	 * @return {@code true}, если вставка разрешена
	 */
	boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir);

	/**
	 * Проверяет, можно ли извлечь {@code stack} из слота {@code slot} со стороны {@code dir}.
	 *
	 * @param slot  индекс исходного слота
	 * @param stack стак для извлечения
	 * @param dir   направление извлечения
	 * @return {@code true}, если извлечение разрешено
	 */
	boolean canExtract(int slot, ItemStack stack, Direction dir);
}
