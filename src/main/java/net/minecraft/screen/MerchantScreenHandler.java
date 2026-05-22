package net.minecraft.screen;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.TradeOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.village.*;

/**
 * Обработчик экрана торговца (жителя/странствующего торговца).
 * <p>
 * Управляет двумя входными слотами и слотом результата сделки.
 * Поддерживает автозаполнение входных слотов при переключении между сделками
 * и воспроизведение звука согласия торговца при быстром перемещении результата.
 */
public class MerchantScreenHandler extends ScreenHandler {

	protected static final int INPUT_1_ID = 0;
	protected static final int INPUT_2_ID = 1;
	protected static final int OUTPUT_ID = 2;
	private static final int INVENTORY_START = 3;
	private static final int INVENTORY_END = 30;
	private static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;
	private static final int INPUT_1_X = 136;
	private static final int INPUT_2_X = 162;
	private static final int OUTPUT_X = 220;
	private static final int SLOT_Y = 37;

	private final Merchant merchant;
	private final MerchantInventory merchantInventory;
	private int levelProgress;
	private boolean leveled;
	private boolean canRefreshTrades;

	public MerchantScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleMerchant(playerInventory.player));
	}

	public MerchantScreenHandler(int syncId, PlayerInventory playerInventory, Merchant merchant) {
		super(ScreenHandlerType.MERCHANT, syncId);
		this.merchant = merchant;
		merchantInventory = new MerchantInventory(merchant);
		addSlot(new Slot(merchantInventory, INPUT_1_ID, INPUT_1_X, SLOT_Y));
		addSlot(new Slot(merchantInventory, INPUT_2_ID, INPUT_2_X, SLOT_Y));
		addSlot(new TradeOutputSlot(playerInventory.player, merchant, merchantInventory, OUTPUT_ID, OUTPUT_X, SLOT_Y));
		addPlayerSlots(playerInventory, 108, 84);
	}

	public void setLeveled(boolean leveled) {
		this.leveled = leveled;
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		merchantInventory.updateOffers();
		super.onContentChanged(inventory);
	}

	public void setRecipeIndex(int index) {
		merchantInventory.setOfferIndex(index);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return merchant.canInteract(player);
	}

	public int getExperience() {
		return merchant.getExperience();
	}

	public int getMerchantRewardedExperience() {
		return merchantInventory.getMerchantRewardedExperience();
	}

	public void setExperienceFromServer(int experience) {
		merchant.setExperienceFromServer(experience);
	}

	public int getLevelProgress() {
		return levelProgress;
	}

	public void setLevelProgress(int levelProgress) {
		this.levelProgress = levelProgress;
	}

	public void setCanRefreshTrades(boolean canRefreshTrades) {
		this.canRefreshTrades = canRefreshTrades;
	}

	public boolean canRefreshTrades() {
		return canRefreshTrades;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return false;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();

		if (slot == OUTPUT_ID) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
			playYesSound();
		}
		else if (slot == INPUT_1_ID || slot == INPUT_2_ID) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
			if (!insertItem(slotStack, HOTBAR_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (slot >= HOTBAR_START && slot < HOTBAR_END) {
			if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}
		else {
			sourceSlot.markDirty();
		}

		if (slotStack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, slotStack);

		return original;
	}

	private void playYesSound() {
		if (merchant.isClient()) {
			return;
		}

		Entity entity = (Entity) merchant;
		entity.getEntityWorld().playSoundClient(
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				merchant.getYesSound(),
				SoundCategory.NEUTRAL,
				1.0F,
				1.0F,
				false
		);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		merchant.setCustomer(null);

		if (merchant.isClient()) {
			return;
		}

		boolean playerDead = !player.isAlive()
				|| player instanceof ServerPlayerEntity serverPlayer && serverPlayer.isDisconnected();

		if (playerDead) {
			ItemStack firstInput = merchantInventory.removeStack(INPUT_1_ID);

			if (!firstInput.isEmpty()) {
				player.dropItem(firstInput, false);
			}

			ItemStack secondInput = merchantInventory.removeStack(INPUT_2_ID);

			if (!secondInput.isEmpty()) {
				player.dropItem(secondInput, false);
			}
		}
		else if (player instanceof ServerPlayerEntity) {
			player.getInventory().offerOrDrop(merchantInventory.removeStack(INPUT_1_ID));
			player.getInventory().offerOrDrop(merchantInventory.removeStack(INPUT_2_ID));
		}
	}

	/**
	 * Переключается на указанную сделку, возвращая текущие ингредиенты в инвентарь
	 * и автозаполняя входные слоты ингредиентами новой сделки из инвентаря игрока.
	 *
	 * @param recipeIndex индекс сделки в списке предложений торговца
	 */
	public void switchTo(int recipeIndex) {
		if (recipeIndex < 0 || getRecipes().size() <= recipeIndex) {
			return;
		}

		ItemStack firstInput = merchantInventory.getStack(INPUT_1_ID);

		if (!firstInput.isEmpty()) {
			if (!insertItem(firstInput, INVENTORY_START, HOTBAR_END, true)) {
				return;
			}

			merchantInventory.setStack(INPUT_1_ID, firstInput);
		}

		ItemStack secondInput = merchantInventory.getStack(INPUT_2_ID);

		if (!secondInput.isEmpty()) {
			if (!insertItem(secondInput, INVENTORY_START, HOTBAR_END, true)) {
				return;
			}

			merchantInventory.setStack(INPUT_2_ID, secondInput);
		}

		if (merchantInventory.getStack(INPUT_1_ID).isEmpty() && merchantInventory.getStack(INPUT_2_ID).isEmpty()) {
			TradeOffer tradeOffer = getRecipes().get(recipeIndex);
			autofill(INPUT_1_ID, tradeOffer.getFirstBuyItem());
			tradeOffer.getSecondBuyItem().ifPresent(item -> autofill(INPUT_2_ID, item));
		}
	}

	private void autofill(int slot, TradedItem tradedItem) {
		for (int playerSlot = INVENTORY_START; playerSlot < HOTBAR_END; playerSlot++) {
			ItemStack playerStack = slots.get(playerSlot).getStack();

			if (playerStack.isEmpty() || !tradedItem.matches(playerStack)) {
				continue;
			}

			ItemStack currentInput = merchantInventory.getStack(slot);

			if (!currentInput.isEmpty() && !ItemStack.areItemsAndComponentsEqual(playerStack, currentInput)) {
				continue;
			}

			int maxCount = playerStack.getMaxCount();
			int transferAmount = Math.min(maxCount - currentInput.getCount(), playerStack.getCount());
			ItemStack merged = playerStack.copyWithCount(currentInput.getCount() + transferAmount);
			playerStack.decrement(transferAmount);
			merchantInventory.setStack(slot, merged);

			if (merged.getCount() >= maxCount) {
				break;
			}
		}
	}

	public void setOffers(TradeOfferList offers) {
		merchant.setOffersFromServer(offers);
	}

	public TradeOfferList getRecipes() {
		return merchant.getOffers();
	}

	public boolean isLeveled() {
		return leveled;
	}
}
