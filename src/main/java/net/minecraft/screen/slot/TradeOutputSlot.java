package net.minecraft.screen.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.village.Merchant;
import net.minecraft.village.MerchantInventory;
import net.minecraft.village.TradeOffer;

/**
 * Слот результата торговли с торговцем (жителем, странствующим торговцем и т.д.).
 * <p>
 * При взятии предмета автоматически расходует входные предметы из инвентаря торговца,
 * начисляет опыт торговцу и увеличивает счётчик статистики игрока.
 */
public class TradeOutputSlot extends Slot {

	private final MerchantInventory merchantInventory;
	private final PlayerEntity player;
	private final Merchant merchant;
	private int amount;

	public TradeOutputSlot(
			PlayerEntity player,
			Merchant merchant,
			MerchantInventory merchantInventory,
			int index,
			int x,
			int y
	) {
		super(merchantInventory, index, x, y);
		this.player = player;
		this.merchant = merchant;
		this.merchantInventory = merchantInventory;
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
	protected void onCrafted(ItemStack stack, int amount) {
		this.amount += amount;
		onCrafted(stack);
	}

	@Override
	protected void onCrafted(ItemStack stack) {
		stack.onCraftByPlayer(player, amount);
		amount = 0;
	}

	/**
	 * Вызывается при взятии предмета из слота результата.
	 * Расходует входные предметы торговли и начисляет опыт торговцу.
	 */
	@Override
	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		onCrafted(stack);

		TradeOffer offer = merchantInventory.getTradeOffer();
		if (offer == null) {
			return;
		}

		ItemStack firstInput = merchantInventory.getStack(0);
		ItemStack secondInput = merchantInventory.getStack(1);

		if (offer.depleteBuyItems(firstInput, secondInput)
				|| offer.depleteBuyItems(secondInput, firstInput)) {
			merchant.trade(offer);
			player.incrementStat(Stats.TRADED_WITH_VILLAGER);
			merchantInventory.setStack(0, firstInput);
			merchantInventory.setStack(1, secondInput);
		}

		merchant.setExperienceFromServer(merchant.getExperience() + offer.getMerchantExperience());
	}
}
