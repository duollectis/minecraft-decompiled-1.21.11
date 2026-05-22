package net.minecraft.screen.slot;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Выходной слот печи.
 * <p>
 * Запрещает вставку предметов вручную. При взятии предмета начисляет опыт
 * за использованные рецепты и вызывает колбэк крафта для статистики.
 */
public class FurnaceOutputSlot extends Slot {

	private final PlayerEntity player;
	private int amount;

	public FurnaceOutputSlot(PlayerEntity player, Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
		this.player = player;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack takeStack(int amount) {
		if (hasStack()) {
			this.amount += Math.min(amount, getStack().getCount());
		}

		return super.takeStack(amount);
	}

	@Override
	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		onCrafted(stack);
		super.onTakeItem(player, stack);
	}

	@Override
	protected void onCrafted(ItemStack stack, int amount) {
		this.amount += amount;
		onCrafted(stack);
	}

	@Override
	protected void onCrafted(ItemStack stack) {
		stack.onCraftByPlayer(player, amount);

		if (player instanceof ServerPlayerEntity serverPlayer
				&& inventory instanceof AbstractFurnaceBlockEntity furnace) {
			furnace.dropExperienceForRecipesUsed(serverPlayer);
		}

		amount = 0;
	}
}
