package net.minecraft.village;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

/**
 * Инвентарь торговца — три слота: два входных (покупка) и один выходной (продажа).
 * <p>
 * При изменении входных слотов автоматически пересчитывает подходящее предложение
 * и обновляет выходной слот. Слот 0 и 1 — товары для обмена, слот 2 — результат.
 */
public class MerchantInventory implements Inventory {

	private static final int SLOT_FIRST_BUY = 0;
	private static final int SLOT_SECOND_BUY = 1;
	private static final int SLOT_SELL = 2;
	private static final int INVENTORY_SIZE = 3;

	private final Merchant merchant;
	private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
	private @Nullable TradeOffer tradeOffer;
	private int offerIndex;
	private int merchantRewardedExperience;

	public MerchantInventory(Merchant merchant) {
		this.merchant = merchant;
	}

	@Override
	public int size() {
		return inventory.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return inventory.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack stack = inventory.get(slot);

		if (slot == SLOT_SELL && !stack.isEmpty()) {
			return Inventories.splitStack(inventory, slot, stack.getCount());
		}

		ItemStack removed = Inventories.splitStack(inventory, slot, amount);

		if (!removed.isEmpty() && isInputSlot(slot)) {
			updateOffers();
		}

		return removed;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(inventory, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		inventory.set(slot, stack);
		stack.capCount(getMaxCount(stack));

		if (isInputSlot(slot)) {
			updateOffers();
		}
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return merchant.getCustomer() == player;
	}

	@Override
	public void markDirty() {
		updateOffers();
	}

	@Override
	public void clear() {
		inventory.clear();
	}

	/**
	 * Пересчитывает активное торговое предложение на основе текущего содержимого
	 * входных слотов. Если подходящее предложение найдено — заполняет выходной слот.
	 * <p>
	 * Логика: сначала пробует (слот0, слот1), затем (слот1, слот0) — для поддержки
	 * обратного порядка ввода товаров.
	 */
	public void updateOffers() {
		tradeOffer = null;

		ItemStack firstBuy;
		ItemStack secondBuy;

		if (inventory.get(SLOT_FIRST_BUY).isEmpty()) {
			firstBuy = inventory.get(SLOT_SECOND_BUY);
			secondBuy = ItemStack.EMPTY;
		} else {
			firstBuy = inventory.get(SLOT_FIRST_BUY);
			secondBuy = inventory.get(SLOT_SECOND_BUY);
		}

		if (firstBuy.isEmpty()) {
			setStack(SLOT_SELL, ItemStack.EMPTY);
			merchantRewardedExperience = 0;
			return;
		}

		TradeOfferList offers = merchant.getOffers();

		if (offers.isEmpty()) {
			merchant.onSellingItem(getStack(SLOT_SELL));
			return;
		}

		TradeOffer found = offers.getValidOffer(firstBuy, secondBuy, offerIndex);

		if (found == null || found.isDisabled()) {
			tradeOffer = found;
			found = offers.getValidOffer(secondBuy, firstBuy, offerIndex);
		}

		if (found != null && !found.isDisabled()) {
			tradeOffer = found;
			setStack(SLOT_SELL, found.copySellItem());
			merchantRewardedExperience = found.getMerchantExperience();
		} else {
			setStack(SLOT_SELL, ItemStack.EMPTY);
			merchantRewardedExperience = 0;
		}

		merchant.onSellingItem(getStack(SLOT_SELL));
	}

	public @Nullable TradeOffer getTradeOffer() {
		return tradeOffer;
	}

	/**
	 * Устанавливает индекс активного предложения и пересчитывает выходной слот.
	 *
	 * @param index индекс предложения в списке торговца
	 */
	public void setOfferIndex(int index) {
		offerIndex = index;
		updateOffers();
	}

	public int getMerchantRewardedExperience() {
		return merchantRewardedExperience;
	}

	private boolean isInputSlot(int slot) {
		return slot == SLOT_FIRST_BUY || slot == SLOT_SECOND_BUY;
	}
}
