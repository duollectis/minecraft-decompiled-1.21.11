package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Цель торговца: смотреть на текущего покупателя, пока идёт торговля.
 */
public class LookAtCustomerGoal extends LookAtEntityGoal {

	private final MerchantEntity merchant;

	public LookAtCustomerGoal(MerchantEntity merchant) {
		super(merchant, PlayerEntity.class, 8.0F);
		this.merchant = merchant;
	}

	@Override
	public boolean canStart() {
		if (!merchant.hasCustomer()) {
			return false;
		}

		target = merchant.getCustomer();
		return true;
	}
}
