package net.minecraft.block.dispenser;

import com.mojang.logging.LogUtils;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;

/**
 * Поведение диспенсера для блоков-предметов ({@link BlockItem}): размещает блок перед диспенсером.
 * Используется, в частности, для шалкеровых ящиков.
 */
public class BlockPlacementDispenserBehavior extends FallibleItemDispenserBehavior {

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		setSuccess(false);
		Item item = stack.getItem();

		if (item instanceof BlockItem blockItem) {
			Direction direction = pointer.state().get(DispenserBlock.FACING);
			BlockPos targetPos = pointer.pos().offset(direction);
			Direction placementFace = pointer.world().isAir(targetPos.down()) ? direction : Direction.UP;

			try {
				setSuccess(
						blockItem.place(new AutomaticItemPlacementContext(
								pointer.world(),
								targetPos,
								direction,
								stack,
								placementFace
						)).isAccepted()
				);
			} catch (Exception exception) {
				LOGGER.error("Error trying to place block from dispenser at {}", targetPos, exception);
			}
		}

		return stack;
	}
}
