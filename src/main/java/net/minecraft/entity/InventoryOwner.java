package net.minecraft.entity;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

/**
 * {@code InventoryOwner}.
 */
public interface InventoryOwner {

	String INVENTORY_KEY = "Inventory";

	SimpleInventory getInventory();

	static void pickUpItem(ServerWorld world, MobEntity entity, InventoryOwner inventoryOwner, ItemEntity item) {
		ItemStack itemStack = item.getStack();
		if (entity.canGather(world, itemStack)) {
			SimpleInventory simpleInventory = inventoryOwner.getInventory();
			boolean bl = simpleInventory.canInsert(itemStack);
			if (!bl) {
				return;
			}

			entity.triggerItemPickedUpByEntityCriteria(item);
			int i = itemStack.getCount();
			ItemStack itemStack2 = simpleInventory.addStack(itemStack);
			entity.sendPickup(item, i - itemStack2.getCount());
			if (itemStack2.isEmpty()) {
				item.discard();
			}
			else {
				itemStack.setCount(itemStack2.getCount());
			}
		}
	}

	default void readInventory(ReadView view) {
		view.getOptionalTypedListView("Inventory", ItemStack.CODEC)
		    .ifPresent(list -> this.getInventory().readDataList((ReadView.TypedListReadView<ItemStack>) list));
	}

	default void writeInventory(WriteView view) {
		this.getInventory().toDataList(view.getListAppender("Inventory", ItemStack.CODEC));
	}
}
