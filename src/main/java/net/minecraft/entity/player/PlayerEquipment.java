package net.minecraft.entity.player;

import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Реализация экипировки игрока, перенаправляющая операции с основной рукой
 * в инвентарь игрока (выбранный слот хотбара).
 */
public class PlayerEquipment extends EntityEquipment {

	private final PlayerEntity player;

	public PlayerEquipment(PlayerEntity player) {
		this.player = player;
	}

	@Override
	public ItemStack put(EquipmentSlot slot, ItemStack itemStack) {
		return slot == EquipmentSlot.MAINHAND
			? player.getInventory().setSelectedStack(itemStack)
			: super.put(slot, itemStack);
	}

	@Override
	public ItemStack get(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND
			? player.getInventory().getSelectedStack()
			: super.get(slot);
	}

	@Override
	public boolean isEmpty() {
		return player.getInventory().getSelectedStack().isEmpty() && super.isEmpty();
	}
}
