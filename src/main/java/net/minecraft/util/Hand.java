package net.minecraft.util;

import net.minecraft.entity.EquipmentSlot;

/**
 * Рука игрока для выполнения действий — основная или вспомогательная.
 * Используется для определения слота экипировки при взаимодействии с предметами.
 */
public enum Hand {
	MAIN_HAND,
	OFF_HAND;

	/** @return соответствующий слот экипировки для данной руки */
	public EquipmentSlot getEquipmentSlot() {
		return this == MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}
}
