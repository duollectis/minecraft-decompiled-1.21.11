package net.minecraft.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;

/**
 * Блок дроппера. В отличие от диспенсера, всегда использует стандартное поведение
 * выброса предмета, но при наличии инвентаря по направлению — перекладывает предмет
 * в него через механику хоппера.
 */
public class DropperBlock extends DispenserBlock {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<DropperBlock> CODEC = createCodec(DropperBlock::new);
	private static final DispenserBehavior BEHAVIOR = new ItemDispenserBehavior();

	@Override
	public MapCodec<DropperBlock> getCodec() {
		return CODEC;
	}

	public DropperBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected DispenserBehavior getBehaviorForItem(World world, ItemStack stack) {
		return BEHAVIOR;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DropperBlockEntity(pos, state);
	}

	@Override
	protected void dispense(ServerWorld world, BlockState state, BlockPos pos) {
		DispenserBlockEntity dropper = world.getBlockEntity(pos, BlockEntityType.DROPPER).orElse(null);

		if (dropper == null) {
			LOGGER.warn("Ignoring dispensing attempt for Dropper without matching block entity at {}", pos);
			return;
		}

		BlockPointer pointer = new BlockPointer(world, pos, state, dropper);
		int slot = dropper.chooseNonEmptySlot(world.random);

		if (slot < 0) {
			world.syncWorldEvent(1001, pos, 0);
			return;
		}

		ItemStack stack = dropper.getStack(slot);

		if (stack.isEmpty()) {
			return;
		}

		Direction facing = world.getBlockState(pos).get(FACING);
		Inventory targetInventory = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));
		ItemStack result;

		if (targetInventory == null) {
			result = BEHAVIOR.dispense(pointer, stack);
		} else {
			result = HopperBlockEntity.transfer(dropper, targetInventory, stack.copyWithCount(1), facing.getOpposite());

			if (result.isEmpty()) {
				result = stack.copy();
				result.decrement(1);
			} else {
				result = stack.copy();
			}
		}

		dropper.setStack(slot, result);
	}
}
