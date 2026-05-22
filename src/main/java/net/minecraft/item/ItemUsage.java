package net.minecraft.item;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Утилитарный класс с вспомогательными методами для обработки использования предметов.
 */
public class ItemUsage {

	/**
	 * Начинает длительное использование предмета: устанавливает текущую руку игрока
	 * и возвращает {@link ActionResult#CONSUME}.
	 *
	 * @param world  мир
	 * @param player игрок
	 * @param hand   рука с предметом
	 * @return {@link ActionResult#CONSUME}
	 */
	public static ActionResult consumeHeldItem(World world, PlayerEntity player, Hand hand) {
		player.setCurrentHand(hand);
		return ActionResult.CONSUME;
	}

	/**
	 * Заменяет входной стек на выходной, учитывая режим творчества.
	 * <p>В режиме творчества с {@code creativeOverride=true} входной стек не уменьшается,
	 * а выходной добавляется в инвентарь только если его там ещё нет.</p>
	 *
	 * @param inputStack      стек, который расходуется
	 * @param player          игрок
	 * @param outputStack     стек, который появляется после использования
	 * @param creativeOverride если {@code true}, в режиме творчества входной стек не расходуется
	 * @return итоговый стек в руке игрока
	 */
	public static ItemStack exchangeStack(
			ItemStack inputStack,
			PlayerEntity player,
			ItemStack outputStack,
			boolean creativeOverride
	) {
		boolean isCreative = player.isInCreativeMode();
		if (creativeOverride && isCreative) {
			if (!player.getInventory().contains(outputStack)) {
				player.getInventory().insertStack(outputStack);
			}

			return inputStack;
		}

		inputStack.decrementUnlessCreative(1, player);
		if (inputStack.isEmpty()) {
			return outputStack;
		}

		if (!player.getInventory().insertStack(outputStack)) {
			player.dropItem(outputStack, false);
		}

		return inputStack;
	}

	/**
	 * Заменяет входной стек на выходной с учётом режима творчества (creativeOverride=true).
	 *
	 * @param inputStack  стек, который расходуется
	 * @param player      игрок
	 * @param outputStack стек, который появляется после использования
	 * @return итоговый стек в руке игрока
	 */
	public static ItemStack exchangeStack(ItemStack inputStack, PlayerEntity player, ItemStack outputStack) {
		return exchangeStack(inputStack, player, outputStack, true);
	}

	/**
	 * Спавнит содержимое предмета-контейнера в мире при его уничтожении.
	 * <p>Вызывается только на сервере. Каждый стек из {@code contents} спавнится
	 * на позиции уничтоженного предмета.</p>
	 *
	 * @param itemEntity уничтоженный предмет-сущность
	 * @param contents   содержимое для спавна
	 */
	public static void spawnItemContents(ItemEntity itemEntity, Iterable<ItemStack> contents) {
		World world = itemEntity.getEntityWorld();
		if (world.isClient()) {
			return;
		}

		contents.forEach(stack -> world.spawnEntity(new ItemEntity(
				world,
				itemEntity.getX(),
				itemEntity.getY(),
				itemEntity.getZ(),
				stack
		)));
	}
}
