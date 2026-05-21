package net.minecraft.entity;

import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.jspecify.annotations.Nullable;

/**
 * {@code CrossbowUser}.
 */
public interface CrossbowUser extends RangedAttackMob {

	void setCharging(boolean charging);

	@Nullable LivingEntity getTarget();

	void postShoot();

	default void shoot(LivingEntity entity, float speed) {
		Hand hand = ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW);
		ItemStack itemStack = entity.getStackInHand(hand);
		if (itemStack.getItem() instanceof CrossbowItem crossbowItem) {
			crossbowItem.shootAll(
					entity.getEntityWorld(),
					entity,
					hand,
					itemStack,
					speed,
					14 - entity.getEntityWorld().getDifficulty().getId() * 4,
					this.getTarget()
			);
		}

		this.postShoot();
	}
}
