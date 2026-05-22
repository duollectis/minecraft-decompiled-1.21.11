package net.minecraft.screen;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.util.Identifier;

/**
 * Обработчик экрана наутилуса.
 * <p>
 * Добавляет слоты седла и брони наутилуса, а также инвентарь игрока.
 * Инвентарь самого наутилуса не добавляется (передаётся пустой инвентарь).
 */
public class NautilusScreenHandler extends MountScreenHandler {

	private static final Identifier EMPTY_SADDLE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/saddle");
	private static final Identifier EMPTY_NAUTILUS_ARMOR_SLOT_TEXTURE =
			Identifier.ofVanilla("container/slot/nautilus_armor_inventory");

	public NautilusScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			AbstractNautilusEntity nautilus,
			int slotColumnCount
	) {
		super(syncId, playerInventory, inventory, nautilus);

		Inventory saddleInventory = nautilus.createEquipmentInventory(EquipmentSlot.SADDLE);
		addSlot(new ArmorSlot(saddleInventory, nautilus, EquipmentSlot.SADDLE, 0, 8, 18, EMPTY_SADDLE_SLOT_TEXTURE) {
			@Override
			public boolean isEnabled() {
				return nautilus.canUseSlot(EquipmentSlot.SADDLE);
			}
		});

		Inventory bodyInventory = nautilus.createEquipmentInventory(EquipmentSlot.BODY);
		addSlot(new ArmorSlot(
				bodyInventory,
				nautilus,
				EquipmentSlot.BODY,
				0,
				8,
				36,
				EMPTY_NAUTILUS_ARMOR_SLOT_TEXTURE
		) {
			@Override
			public boolean isEnabled() {
				return nautilus.canUseSlot(EquipmentSlot.BODY);
			}
		});

		addPlayerSlots(playerInventory, 8, 84);
	}

	@Override
	protected boolean areInventoriesDifferent(Inventory inventory) {
		return ((AbstractNautilusEntity) mount).areInventoriesDifferent(inventory);
	}
}
