package net.minecraft.screen.slot;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Слот экипировки для брони и снаряжения живых существ.
 * <p>
 * Ограничивает вставку предметами, подходящими для конкретного {@link EquipmentSlot},
 * и запрещает снятие предметов с зачарованием {@code PREVENT_ARMOR_CHANGE} у не-творческих игроков.
 */
public class ArmorSlot extends Slot {

	private final LivingEntity entity;
	private final EquipmentSlot equipmentSlot;
	private final @Nullable Identifier backgroundSprite;

	public ArmorSlot(
			Inventory inventory,
			LivingEntity entity,
			EquipmentSlot equipmentSlot,
			int index,
			int x,
			int y,
			@Nullable Identifier backgroundSprite
	) {
		super(inventory, index, x, y);
		this.entity = entity;
		this.equipmentSlot = equipmentSlot;
		this.backgroundSprite = backgroundSprite;
	}

	@Override
	public void setStack(ItemStack stack, ItemStack previousStack) {
		entity.onEquipStack(equipmentSlot, previousStack, stack);
		super.setStack(stack, previousStack);
	}

	@Override
	public int getMaxItemCount() {
		return 1;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return entity.canEquip(stack, equipmentSlot);
	}

	@Override
	public boolean isEnabled() {
		return entity.canUseSlot(equipmentSlot);
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack equipped = getStack();
		boolean cursed = !equipped.isEmpty()
				&& !player.isCreative()
				&& EnchantmentHelper.hasAnyEnchantmentsWith(equipped, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE);

		return cursed ? false : super.canTakeItems(player);
	}

	@Override
	public @Nullable Identifier getBackgroundSprite() {
		return backgroundSprite;
	}
}
