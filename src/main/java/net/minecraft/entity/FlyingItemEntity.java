package net.minecraft.entity;

import net.minecraft.item.ItemStack;

/**
 * Интерфейс для летящих снарядов, визуально представленных предметом (яйцо, снежок, эндер-жемчуг и т.д.).
 */
public interface FlyingItemEntity {

	ItemStack getStack();
}
