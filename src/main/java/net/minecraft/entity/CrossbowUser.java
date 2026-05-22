package net.minecraft.entity;

import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для мобов, использующих арбалет в качестве оружия.
 * Расширяет {@link RangedAttackMob}, добавляя логику зарядки и выстрела из арбалета.
 */
public interface CrossbowUser extends RangedAttackMob {

	void setCharging(boolean charging);

	@Nullable LivingEntity getTarget();

	void postShoot();

	/**
	 * Производит выстрел из арбалета, если сущность держит его в руке.
	 * Разброс снарядов масштабируется от сложности мира: чем выше сложность,
	 * тем точнее стрельба (разброс уменьшается на 4 единицы за уровень сложности).
	 *
	 * @param entity стреляющая сущность
	 * @param speed  скорость снаряда
	 */
	default void shoot(LivingEntity entity, float speed) {
		Hand hand = ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW);
		ItemStack stack = entity.getStackInHand(hand);

		if (stack.getItem() instanceof CrossbowItem crossbowItem) {
			crossbowItem.shootAll(
				entity.getEntityWorld(),
				entity,
				hand,
				stack,
				speed,
				14 - entity.getEntityWorld().getDifficulty().getId() * 4,
				getTarget()
			);
		}

		postShoot();
	}
}
