package net.minecraft.block;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

/**
 * Контракт для блоков, предоставляющих доступ к {@link SidedInventory} с учётом стороны.
 * Реализуется блоками-контейнерами (компостер, воронка и т.д.), которые должны
 * экспортировать свой инвентарь для автоматизации (хопперы, трубы).
 */
public interface InventoryProvider {

	/**
	 * Возвращает {@link SidedInventory} для данного блока в указанной позиции мира.
	 * Используется хопперами и другими механизмами автоматической передачи предметов.
	 *
	 * @param state текущее состояние блока
	 * @param world доступ к миру (может быть клиентским или серверным)
	 * @param pos позиция блока в мире
	 * @return инвентарь блока с поддержкой направленного доступа к слотам
	 */
	SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos);
}
