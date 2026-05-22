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
 * Базовый интерфейс инвентаря Minecraft. Определяет контракт для любого
 * хранилища предметов: доступ по слоту, изменение содержимого, уведомление
 * об изменениях и проверку прав доступа игрока.
 *
 * <p>Реализует {@link Iterable} для удобного перебора всех стаков,
 * а также {@link StackReferenceGetter} для унифицированного доступа
 * через {@link StackReference}.
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
		return Math.min(getMaxCountPerStack(), stack.getMaxCount());
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
		int total = 0;

		for (ItemStack stack : this) {
			if (stack.getItem().equals(item)) {
				total += stack.getCount();
			}
		}

		return total;
	}

	default boolean containsAny(Set<Item> items) {
		return containsAny(stack -> !stack.isEmpty() && items.contains(stack.getItem()));
	}

	default boolean containsAny(Predicate<ItemStack> predicate) {
		for (ItemStack stack : this) {
			if (predicate.test(stack)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Проверяет, может ли игрок взаимодействовать с блок-сущностью
	 * в пределах дистанции {@link #DEFAULT_MAX_INTERACTION_RANGE}.
	 *
	 * @param blockEntity блок-сущность контейнера
	 * @param player      игрок, пытающийся открыть контейнер
	 * @return {@code true}, если взаимодействие разрешено
	 */
	static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player) {
		return canPlayerUse(blockEntity, player, DEFAULT_MAX_INTERACTION_RANGE);
	}

	/**
	 * Проверяет, может ли игрок взаимодействовать с блок-сущностью
	 * в пределах заданной дистанции. Возвращает {@code false}, если мир
	 * не загружен или блок-сущность была заменена другим блоком.
	 *
	 * @param blockEntity блок-сущность контейнера
	 * @param player      игрок, пытающийся открыть контейнер
	 * @param range       максимальная дистанция взаимодействия в блоках
	 * @return {@code true}, если взаимодействие разрешено
	 */
	static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player, float range) {
		World world = blockEntity.getWorld();
		BlockPos pos = blockEntity.getPos();

		if (world == null) {
			return false;
		}

		if (world.getBlockEntity(pos) != blockEntity) {
			return false;
		}

		return player.canInteractWithBlockAt(pos, range);
	}

	@Override
	default @Nullable StackReference getStackReference(int slot) {
		if (slot < 0 || slot >= size()) {
			return null;
		}

		return new StackReference() {
			@Override
			public ItemStack get() {
				return Inventory.this.getStack(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				Inventory.this.setStack(slot, stack);
				return true;
			}
		};
	}

	@Override
	default java.util.Iterator<ItemStack> iterator() {
		return new Inventory.Iterator(this);
	}

	/**
	 * Итератор по слотам инвентаря. Фиксирует размер инвентаря в момент создания,
	 * чтобы избежать проблем при изменении инвентаря во время итерации.
	 */
	class Iterator implements java.util.Iterator<ItemStack> {

		private final Inventory inventory;
		private int index;
		private final int size;

		public Iterator(Inventory inventory) {
			this.inventory = inventory;
			this.size = inventory.size();
		}

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public ItemStack next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			return inventory.getStack(index++);
		}
	}
}
