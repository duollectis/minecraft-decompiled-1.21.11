package net.minecraft.screen;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Обработчик экрана лошади (и ламы).
 * <p>
 * Добавляет слоты седла и брони (для ламы — попона, для лошади — броня),
 * а также сетку инвентаря лошади (если есть) и инвентарь игрока.
 */
public class HorseScreenHandler extends MountScreenHandler {

	private static final Identifier EMPTY_SADDLE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/saddle");
	private static final Identifier EMPTY_LLAMA_ARMOR_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/llama_armor");
	private static final Identifier EMPTY_HORSE_ARMOR_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/horse_armor");
	private static final int SLOT_STEP = 18;
	private static final int INVENTORY_ROWS = 3;
	private static final int INVENTORY_START_X = 80;
	private static final int INVENTORY_START_Y = 18;

	public HorseScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			AbstractHorseEntity entity,
			int slotColumnCount
	) {
		super(syncId, playerInventory, inventory, entity);

		Inventory saddleInventory = entity.createEquipmentInventory(EquipmentSlot.SADDLE);
		addSlot(new ArmorSlot(saddleInventory, entity, EquipmentSlot.SADDLE, 0, 8, 18, EMPTY_SADDLE_SLOT_TEXTURE) {
			@Override
			public boolean isEnabled() {
				return entity.canUseSlot(EquipmentSlot.SADDLE)
						&& entity.getType().isIn(EntityTypeTags.CAN_EQUIP_SADDLE);
			}
		});

		boolean isLlama = entity instanceof LlamaEntity;
		Identifier armorTexture = isLlama ? EMPTY_LLAMA_ARMOR_SLOT_TEXTURE : EMPTY_HORSE_ARMOR_SLOT_TEXTURE;
		Inventory bodyInventory = entity.createEquipmentInventory(EquipmentSlot.BODY);
		addSlot(new ArmorSlot(bodyInventory, entity, EquipmentSlot.BODY, 0, 8, 36, armorTexture) {
			@Override
			public boolean isEnabled() {
				return entity.canUseSlot(EquipmentSlot.BODY)
						&& (entity.getType().isIn(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || isLlama);
			}
		});

		if (slotColumnCount > 0) {
			for (int row = 0; row < INVENTORY_ROWS; row++) {
				for (int col = 0; col < slotColumnCount; col++) {
					addSlot(new Slot(
							inventory,
							col + row * slotColumnCount,
							INVENTORY_START_X + col * SLOT_STEP,
							INVENTORY_START_Y + row * SLOT_STEP
					));
				}
			}
		}

		addPlayerSlots(playerInventory, 8, 84);
	}

	@Override
	protected boolean areInventoriesDifferent(Inventory inventory) {
		return ((AbstractHorseEntity) mount).areInventoriesDifferent(inventory);
	}
}
