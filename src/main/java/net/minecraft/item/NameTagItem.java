package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Предмет «Бирка». При использовании на существе присваивает ему пользовательское
 * имя из компонента {@link DataComponentTypes#CUSTOM_NAME}. Если существо является
 * {@link MobEntity}, дополнительно делает его постоянным (не деспавнится).
 */
public class NameTagItem extends Item {

	public NameTagItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);

		if (customName == null || !entity.getType().isSaveable()) {
			return ActionResult.PASS;
		}

		if (!user.getEntityWorld().isClient() && entity.isAlive()) {
			entity.setCustomName(customName);

			if (entity instanceof MobEntity mob) {
				mob.setPersistent();
			}

			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}
}
