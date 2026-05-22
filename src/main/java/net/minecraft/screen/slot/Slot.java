package net.minecraft.screen.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Слот инвентаря в экране обработчика.
 * <p>
 * Представляет одну ячейку инвентаря с позицией на экране и логикой ограничений
 * (что можно вставить, кто может взять). Базовый класс для всех специализированных
 * слотов (броня, результат крафта, топливо и т.д.).
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

	public void onQuickTransfer(ItemStack newItem, ItemStack original) {
		int transferred = original.getCount() - newItem.getCount();

		if (transferred > 0) {
			onCrafted(original, transferred);
		}
	}

	protected void onCrafted(ItemStack stack, int amount) {
	}

	public void onTake(int amount) {
	}

	protected void onCrafted(ItemStack stack) {
	}

	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		markDirty();
	}

	public boolean canInsert(ItemStack stack) {
		return true;
	}

	public ItemStack getStack() {
		return inventory.getStack(index);
	}

	public boolean hasStack() {
		return !getStack().isEmpty();
	}

	public void setStack(ItemStack stack) {
		setStack(stack, getStack());
	}

	public void setStack(ItemStack stack, ItemStack previousStack) {
		setStackNoCallbacks(stack);
	}

	public void setStackNoCallbacks(ItemStack stack) {
		inventory.setStack(index, stack);
		markDirty();
	}

	public void markDirty() {
		inventory.markDirty();
	}

	public int getMaxItemCount() {
		return inventory.getMaxCountPerStack();
	}

	public int getMaxItemCount(ItemStack stack) {
		return Math.min(getMaxItemCount(), stack.getMaxCount());
	}

	public @Nullable Identifier getBackgroundSprite() {
		return null;
	}

	public ItemStack takeStack(int amount) {
		return inventory.removeStack(index, amount);
	}

	public boolean canTakeItems(PlayerEntity player) {
		return true;
	}

	public boolean isEnabled() {
		return true;
	}

	/**
	 * Пытается взять из слота от {@code min} до {@code max} предметов.
	 * Учитывает ограничения {@link #canTakeItems} и {@link #canTakePartial}.
	 *
	 * @param min    минимальное желаемое количество
	 * @param max    максимальное желаемое количество
	 * @param player игрок, выполняющий действие
	 * @return взятый стек или {@link Optional#empty()} если взять невозможно
	 */
	public Optional<ItemStack> tryTakeStackRange(int min, int max, PlayerEntity player) {
		if (!canTakeItems(player)) {
			return Optional.empty();
		}

		if (!canTakePartial(player) && max < getStack().getCount()) {
			return Optional.empty();
		}

		int actualAmount = Math.min(min, max);
		ItemStack taken = takeStack(actualAmount);

		if (taken.isEmpty()) {
			return Optional.empty();
		}

		if (getStack().isEmpty()) {
			setStack(ItemStack.EMPTY, taken);
		}

		return Optional.of(taken);
	}

	public ItemStack takeStackRange(int min, int max, PlayerEntity player) {
		Optional<ItemStack> taken = tryTakeStackRange(min, max, player);
		taken.ifPresent(stack -> onTakeItem(player, stack));
		return taken.orElse(ItemStack.EMPTY);
	}

	/**
	 * Вставляет указанное количество предметов из стека в слот.
	 * Возвращает остаток стека после вставки.
	 *
	 * @param stack стек для вставки
	 * @param count максимальное количество для вставки
	 * @return остаток стека (может быть тем же объектом если вставка невозможна)
	 */
	public ItemStack insertStack(ItemStack stack, int count) {
		if (stack.isEmpty() || !canInsert(stack)) {
			return stack;
		}

		ItemStack current = getStack();
		int insertable = Math.min(Math.min(count, stack.getCount()), getMaxItemCount(stack) - current.getCount());

		if (insertable <= 0) {
			return stack;
		}

		if (current.isEmpty()) {
			setStack(stack.split(insertable));
		} else if (ItemStack.areItemsAndComponentsEqual(current, stack)) {
			stack.decrement(insertable);
			current.increment(insertable);
			setStack(current);
		}

		return stack;
	}

	public ItemStack insertStack(ItemStack stack) {
		return insertStack(stack, stack.getCount());
	}

	public boolean canTakePartial(PlayerEntity player) {
		return canTakeItems(player) && canInsert(getStack());
	}

	public int getIndex() {
		return index;
	}

	public boolean canBeHighlighted() {
		return true;
	}

	public boolean disablesDynamicDisplay() {
		return false;
	}
}
