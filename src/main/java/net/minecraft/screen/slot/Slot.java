package net.minecraft.screen.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code Slot}.
 */
public class Slot {

	private final int index;
	public final Inventory inventory;
	public int id;
	public final int x;
	public final int y;

	public Slot(Inventory inventory, int index, int x, int y) {
		this.inventory = inventory;
		this.index = index;
		this.x = x;
		this.y = y;
	}

	/**
	 * Обрабатывает событие quick transfer.
	 *
	 * @param newItem new item
	 * @param original original
	 */
	public void onQuickTransfer(ItemStack newItem, ItemStack original) {
		int i = original.getCount() - newItem.getCount();
		if (i > 0) {
			this.onCrafted(original, i);
		}
	}

	/**
	 * Обрабатывает событие crafted.
	 *
	 * @param stack stack
	 * @param amount amount
	 */
	protected void onCrafted(ItemStack stack, int amount) {
	}

	/**
	 * Обрабатывает событие take.
	 *
	 * @param amount amount
	 */
	public void onTake(int amount) {
	}

	/**
	 * Обрабатывает событие crafted.
	 *
	 * @param stack stack
	 */
	protected void onCrafted(ItemStack stack) {
	}

	/**
	 * Обрабатывает событие take item.
	 *
	 * @param player player
	 * @param stack stack
	 */
	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		this.markDirty();
	}

	/**
	 * Проверяет возможность insert.
	 *
	 * @param stack stack
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canInsert(ItemStack stack) {
		return true;
	}

	public ItemStack getStack() {
		return this.inventory.getStack(this.index);
	}

	public boolean hasStack() {
		return !this.getStack().isEmpty();
	}

	public void setStack(ItemStack stack) {
		this.setStack(stack, this.getStack());
	}

	public void setStack(ItemStack stack, ItemStack previousStack) {
		this.setStackNoCallbacks(stack);
	}

	public void setStackNoCallbacks(ItemStack stack) {
		this.inventory.setStack(this.index, stack);
		this.markDirty();
	}

	/**
	 * Mark dirty.
	 */
	public void markDirty() {
		this.inventory.markDirty();
	}

	public int getMaxItemCount() {
		return this.inventory.getMaxCountPerStack();
	}

	public int getMaxItemCount(ItemStack stack) {
		return Math.min(this.getMaxItemCount(), stack.getMaxCount());
	}

	public @Nullable Identifier getBackgroundSprite() {
		return null;
	}

	/**
	 * Take stack.
	 *
	 * @param amount amount
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack takeStack(int amount) {
		return this.inventory.removeStack(this.index, amount);
	}

	/**
	 * Проверяет возможность take items.
	 *
	 * @param playerEntity player entity
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canTakeItems(PlayerEntity playerEntity) {
		return true;
	}

	public boolean isEnabled() {
		return true;
	}

	/**
	 * Try take stack range.
	 *
	 * @param min min
	 * @param max max
	 * @param player player
	 *
	 * @return Optional — результат операции
	 */
	public Optional<ItemStack> tryTakeStackRange(int min, int max, PlayerEntity player) {
		if (!this.canTakeItems(player)) {
			return Optional.empty();
		}
		else if (!this.canTakePartial(player) && max < this.getStack().getCount()) {
			return Optional.empty();
		}
		else {
			min = Math.min(min, max);
			ItemStack itemStack = this.takeStack(min);
			if (itemStack.isEmpty()) {
				return Optional.empty();
			}
			else {
				if (this.getStack().isEmpty()) {
					this.setStack(ItemStack.EMPTY, itemStack);
				}

				return Optional.of(itemStack);
			}
		}
	}

	/**
	 * Take stack range.
	 *
	 * @param min min
	 * @param max max
	 * @param player player
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack takeStackRange(int min, int max, PlayerEntity player) {
		Optional<ItemStack> optional = this.tryTakeStackRange(min, max, player);
		optional.ifPresent(stack -> this.onTakeItem(player, stack));
		return optional.orElse(ItemStack.EMPTY);
	}

	/**
	 * Insert stack.
	 *
	 * @param stack stack
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack insertStack(ItemStack stack) {
		return this.insertStack(stack, stack.getCount());
	}

	/**
	 * Insert stack.
	 *
	 * @param stack stack
	 * @param count count
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack insertStack(ItemStack stack, int count) {
		if (!stack.isEmpty() && this.canInsert(stack)) {
			ItemStack itemStack = this.getStack();
			int i = Math.min(Math.min(count, stack.getCount()), this.getMaxItemCount(stack) - itemStack.getCount());
			if (i <= 0) {
				return stack;
			}
			else {
				if (itemStack.isEmpty()) {
					this.setStack(stack.split(i));
				}
				else if (ItemStack.areItemsAndComponentsEqual(itemStack, stack)) {
					stack.decrement(i);
					itemStack.increment(i);
					this.setStack(itemStack);
				}

				return stack;
			}
		}
		else {
			return stack;
		}
	}

	/**
	 * Проверяет возможность take partial.
	 *
	 * @param player player
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canTakePartial(PlayerEntity player) {
		return this.canTakeItems(player) && this.canInsert(this.getStack());
	}

	public int getIndex() {
		return this.index;
	}

	/**
	 * Проверяет возможность be highlighted.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canBeHighlighted() {
		return true;
	}

	/**
	 * Отключает s dynamic display.
	 *
	 * @return boolean — результат операции
	 */
	public boolean disablesDynamicDisplay() {
		return false;
	}
}
