package net.minecraft.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Предмет «Мотыга». Вспахивает землю, превращая блоки в грядки или другие блоки.
 * Таблица преобразований хранится в {@link #TILLING_ACTIONS}.
 */
public class HoeItem extends Item {

	/** Флаги обновления блока при вспашке. */
	private static final int TILL_UPDATE_FLAGS = 11;

	/**
	 * Таблица действий вспашки: блок → (условие применения, действие преобразования).
	 * Используется в {@link #useOnBlock} для определения результата взаимодействия.
	 */
	protected static final Map<Block, Pair<Predicate<ItemUsageContext>, Consumer<ItemUsageContext>>> TILLING_ACTIONS =
			Maps.newHashMap(
					ImmutableMap.of(
							Blocks.GRASS_BLOCK,
							Pair.of(HoeItem::canTillFarmland, createTillAction(Blocks.FARMLAND.getDefaultState())),
							Blocks.DIRT_PATH,
							Pair.of(HoeItem::canTillFarmland, createTillAction(Blocks.FARMLAND.getDefaultState())),
							Blocks.DIRT,
							Pair.of(HoeItem::canTillFarmland, createTillAction(Blocks.FARMLAND.getDefaultState())),
							Blocks.COARSE_DIRT,
							Pair.of(HoeItem::canTillFarmland, createTillAction(Blocks.DIRT.getDefaultState())),
							Blocks.ROOTED_DIRT,
							Pair.of(
									(Predicate<ItemUsageContext>) context -> true,
									createTillAndDropAction(Blocks.DIRT.getDefaultState(), Items.HANGING_ROOTS)
							)
					)
			);

	public HoeItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
		super(settings.hoe(material, attackDamage, attackSpeed));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		Pair<Predicate<ItemUsageContext>, Consumer<ItemUsageContext>> action =
				TILLING_ACTIONS.get(world.getBlockState(pos).getBlock());

		if (action == null) {
			return ActionResult.PASS;
		}

		Predicate<ItemUsageContext> condition = action.getFirst();
		Consumer<ItemUsageContext> tillAction = action.getSecond();

		if (!condition.test(context)) {
			return ActionResult.PASS;
		}

		PlayerEntity player = context.getPlayer();
		world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);

		if (!world.isClient()) {
			tillAction.accept(context);

			if (player != null) {
				context.getStack().damage(1, player, context.getHand().getEquipmentSlot());
			}
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Создаёт действие вспашки, которое заменяет блок на указанное состояние.
	 *
	 * @param result целевое состояние блока после вспашки
	 * @return потребитель контекста использования
	 */
	public static Consumer<ItemUsageContext> createTillAction(BlockState result) {
		return context -> {
			context.getWorld().setBlockState(context.getBlockPos(), result, TILL_UPDATE_FLAGS);
			context.getWorld().emitGameEvent(
					GameEvent.BLOCK_CHANGE,
					context.getBlockPos(),
					GameEvent.Emitter.of(context.getPlayer(), result)
			);
		};
	}

	/**
	 * Создаёт действие вспашки с выпадением предмета.
	 * Используется для корневой земли: заменяет блок и выбрасывает подвесные корни.
	 *
	 * @param result      целевое состояние блока
	 * @param droppedItem предмет, который выпадает при вспашке
	 * @return потребитель контекста использования
	 */
	public static Consumer<ItemUsageContext> createTillAndDropAction(BlockState result, ItemConvertible droppedItem) {
		return context -> {
			context.getWorld().setBlockState(context.getBlockPos(), result, TILL_UPDATE_FLAGS);
			context.getWorld().emitGameEvent(
					GameEvent.BLOCK_CHANGE,
					context.getBlockPos(),
					GameEvent.Emitter.of(context.getPlayer(), result)
			);
			Block.dropStack(context.getWorld(), context.getBlockPos(), context.getSide(), new ItemStack(droppedItem));
		};
	}

	/**
	 * Проверяет, можно ли вспахать блок в грядку.
	 * Требует: клик не снизу и блок сверху — воздух.
	 *
	 * @param context контекст использования предмета
	 * @return {@code true} если вспашка возможна
	 */
	public static boolean canTillFarmland(ItemUsageContext context) {
		return context.getSide() != Direction.DOWN
				&& context.getWorld().getBlockState(context.getBlockPos().up()).isAir();
	}
}
