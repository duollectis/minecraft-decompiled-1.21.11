package net.minecraft.entity;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Маркерный интерфейс для верховых сущностей, имеющих инвентарь (например, лошади, ламы).
 * Позволяет игроку открыть экран инвентаря верхового животного.
 */
public interface RideableInventory {

	void openInventory(PlayerEntity player);
}
