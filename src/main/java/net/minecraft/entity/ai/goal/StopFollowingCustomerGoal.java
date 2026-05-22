package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

/**
 * Цель торговца: останавливать навигацию, пока покупатель находится рядом
 * (в радиусе 4 блоков). По завершении сбрасывает ссылку на покупателя.
 */
public class StopFollowingCustomerGoal extends Goal {

	private static final double MAX_CUSTOMER_DISTANCE_SQ = 16.0;

	private final MerchantEntity merchant;

	public StopFollowingCustomerGoal(MerchantEntity merchant) {
		this.merchant = merchant;
		this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (!merchant.isAlive()) {
			return false;
		}

		if (merchant.isTouchingWater()) {
			return false;
		}

		if (!merchant.isOnGround()) {
			return false;
		}

		if (merchant.knockedBack) {
			return false;
		}

		PlayerEntity customer = merchant.getCustomer();
		return customer != null && merchant.squaredDistanceTo(customer) <= MAX_CUSTOMER_DISTANCE_SQ;
	}

	@Override
	public void start() {
		merchant.getNavigation().stop();
	}

	@Override
	public void stop() {
		merchant.setCustomer(null);
	}
}
