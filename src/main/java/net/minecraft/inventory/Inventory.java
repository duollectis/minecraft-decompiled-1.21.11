package net.minecraft.inventory;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code Inventory}.
 */
public interface Inventory extends Clearable, StackReferenceGetter, Iterable<ItemStack> {

	float DEFAULT_MAX_INTERACTION_RANGE = 4.0F;

	int size();

	boolean isEmpty();

	ItemStack getStack(int slot);

	ItemStack removeStack(int slot, int amount);

	ItemStack removeStack(int slot);

	void setStack(int slot, ItemStack stack);

	default int getMaxCountPerStack() {
		return 99;
	}

	default int getMaxCount(ItemStack stack) {
		return Math.min(this.getMaxCountPerStack(), stack.getMaxCount());
	}

	void markDirty();

	boolean canPlayerUse(PlayerEntity player);

	default void onOpen(ContainerUser user) {
	}

	default void onClose(ContainerUser user) {
	}

	default List<ContainerUser> getViewingUsers() {
		return List.of();
	}

	default boolean isValid(int slot, ItemStack stack) {
		return true;
	}

	default boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
		return true;
	}

	default int count(Item item) {
		int i = 0;

		for (ItemStack itemStack : this) {
			if (itemStack.getItem().equals(item)) {
				i += itemStack.getCount();
			}
		}

		return i;
	}

	default boolean containsAny(Set<Item> items) {
		return this.containsAny(stack -> !stack.isEmpty() && items.contains(stack.getItem()));
	}

	default boolean containsAny(Predicate<ItemStack> predicate) {
		for (ItemStack itemStack : this) {
			if (predicate.test(itemStack)) {
				return true;
			}
		}

		return false;
	}

	static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player) {
		return canPlayerUse(blockEntity, player, 4.0F);
	}

	static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player, float range) {
		World world = blockEntity.getWorld();
		BlockPos blockPos = blockEntity.getPos();
		if (world == null) {
			return false;
		}
		else {
			return world.getBlockEntity(blockPos) != blockEntity ? false
			                                                     : player.canInteractWithBlockAt(blockPos, range);
		}
	}

	@Override
	default @Nullable StackReference getStackReference(int slot) {
		return slot >= 0 && slot < this.size() ? new StackReference() {
			@Override
			public ItemStack get() {
				return Inventory.this.getStack(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				Inventory.this.setStack(slot, stack);
				return true;
			}
		} : null;
	}

	@Override
	default java.util.Iterator<ItemStack> iterator() {
		return new Inventory.Iterator(this);
	}

	/**
	 * {@code Iterator}.
	 */
	public static class Iterator implements java.util.Iterator<ItemStack> {

		private final Inventory inventory;
		private int index;
		private final int size;

		public Iterator(Inventory inventory) {
			this.inventory = inventory;
			this.size = inventory.size();
		}

		@Override
		public boolean hasNext() {
			return this.index < this.size;
		}

		public ItemStack next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}
			else {
				return this.inventory.getStack(this.index++);
			}
		}
	}
}
