package net.minecraft.util;

import net.minecraft.entity.EquipmentSlot;

/**
 * {@code Hand}.
 */
public enum Hand {
	MAIN_HAND,
	OFF_HAND;

	public EquipmentSlot getEquipmentSlot() {
		return this == MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}
}
