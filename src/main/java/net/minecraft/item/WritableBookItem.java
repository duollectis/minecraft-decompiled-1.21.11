package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Книга и перо — открывает интерфейс редактирования книги при использовании.
 */
public class WritableBookItem extends Item {

	public WritableBookItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		user.useBook(itemStack, hand);
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		return ActionResult.SUCCESS;
	}
}
