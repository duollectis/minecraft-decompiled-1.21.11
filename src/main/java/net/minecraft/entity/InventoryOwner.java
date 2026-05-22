package net.minecraft.entity;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

/**
 * Интерфейс для сущностей, владеющих инвентарём (например, лошади, лисы).
 * Предоставляет логику подбора предметов с земли и сериализации инвентаря.
 */
public interface InventoryOwner {

	String INVENTORY_KEY = "Inventory";

	SimpleInventory getInventory();

	/**
	 * Обрабатывает подбор предмета с земли в инвентарь сущности.
	 * Предмет подбирается только если сущность может его собрать и в инвентаре есть место.
	 * Остаток предмета, не вошедший в инвентарь, остаётся на земле.
	 *
	 * @param world          серверный мир
	 * @param entity         сущность-подборщик
	 * @param inventoryOwner владелец инвентаря
	 * @param item           сущность предмета на земле
	 */
	static void pickUpItem(ServerWorld world, MobEntity entity, InventoryOwner inventoryOwner, ItemEntity item) {
		ItemStack itemStack = item.getStack();
		if (!entity.canGather(world, itemStack)) {
			return;
		}

		SimpleInventory inventory = inventoryOwner.getInventory();
		if (!inventory.canInsert(itemStack)) {
			return;
		}

		entity.triggerItemPickedUpByEntityCriteria(item);
		int originalCount = itemStack.getCount();
		ItemStack remainder = inventory.addStack(itemStack);
		entity.sendPickup(item, originalCount - remainder.getCount());

		if (remainder.isEmpty()) {
			item.discard();
		} else {
			itemStack.setCount(remainder.getCount());
		}
	}

	default void readInventory(ReadView view) {
		view.getOptionalTypedListView("Inventory", ItemStack.CODEC)
			.ifPresent(list -> getInventory().readDataList((ReadView.TypedListReadView<ItemStack>) list));
	}

	default void writeInventory(WriteView view) {
		getInventory().toDataList(view.getListAppender("Inventory", ItemStack.CODEC));
	}
}
