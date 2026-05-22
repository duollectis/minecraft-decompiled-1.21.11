package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Предмет «Пустая карта». При использовании создаёт заполненную карту
 * масштаба 0 с центром в текущей позиции игрока.
 */
public class EmptyMapItem extends Item {

	public EmptyMapItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			stack.decrementUnlessCreative(1, user);
			user.incrementStat(Stats.USED.getOrCreateStat(this));
			serverWorld.playSoundFromEntity(
					null,
					user,
					SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
					user.getSoundCategory(),
					1.0F,
					1.0F
			);

			ItemStack filledMap = FilledMapItem.createMap(serverWorld, user.getBlockX(), user.getBlockZ(), (byte) 0, true, false);

			if (stack.isEmpty()) {
				return ActionResult.SUCCESS.withNewHandStack(filledMap);
			}

			if (!user.getInventory().insertStack(filledMap.copy())) {
				user.dropItem(filledMap, false);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.SUCCESS;
	}
}
