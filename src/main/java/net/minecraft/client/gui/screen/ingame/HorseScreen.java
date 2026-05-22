package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Экран инвентаря лошади. Отображает слоты седла, брони и сундука (если есть).
 */
@Environment(EnvType.CLIENT)
public class HorseScreen extends MountScreen<HorseScreenHandler> {

	private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
	private static final Identifier CHEST_SLOTS_TEXTURE = Identifier.ofVanilla("container/horse/chest_slots");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/horse.png");

	public HorseScreen(
		HorseScreenHandler handler,
		PlayerInventory inventory,
		AbstractHorseEntity entity,
		int slotColumnCount
	) {
		super(handler, inventory, entity.getDisplayName(), slotColumnCount, entity);
	}

	@Override
	protected Identifier getTexture() {
		return TEXTURE;
	}

	@Override
	protected Identifier getSlotTexture() {
		return SLOT_TEXTURE;
	}

	@Override
	protected @Nullable Identifier getChestSlotsTexture() {
		return CHEST_SLOTS_TEXTURE;
	}

	@Override
	protected boolean canEquipSaddle() {
		return mount.canUseSlot(EquipmentSlot.SADDLE)
			&& mount.getType().isIn(EntityTypeTags.CAN_EQUIP_SADDLE);
	}

	@Override
	protected boolean canEquipArmor() {
		return mount.canUseSlot(EquipmentSlot.BODY)
			&& (mount.getType().isIn(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || mount instanceof LlamaEntity);
	}
}
