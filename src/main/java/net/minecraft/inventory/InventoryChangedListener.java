package net.minecraft.inventory;

/**
 * Слушатель изменений инвентаря. Реализуется объектами, которым необходимо
 * реагировать на любое изменение содержимого {@link SimpleInventory}.
 */
public interface InventoryChangedListener {

	void onInventoryChanged(Inventory sender);
}
