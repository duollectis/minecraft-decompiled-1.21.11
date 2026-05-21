package net.minecraft.block.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * {@code ItemDispenserBehavior}.
 */
public class ItemDispenserBehavior implements DispenserBehavior {

	private static final int ITEM_SPAWN_SPEED = 6;

	@Override
	public final ItemStack dispense(BlockPointer blockPointer, ItemStack itemStack) {
		ItemStack itemStack2 = this.dispenseSilently(blockPointer, itemStack);
		this.playSound(blockPointer);
		this.spawnParticles(blockPointer, blockPointer.state().get(DispenserBlock.FACING));
		return itemStack2;
	}

	/**
	 * Dispense silently.
	 *
	 * @param pointer pointer
	 * @param stack stack
	 *
	 * @return ItemStack — результат операции
	 */
	protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		Direction direction = pointer.state().get(DispenserBlock.FACING);
		Position position = DispenserBlock.getOutputLocation(pointer);
		ItemStack itemStack = stack.split(1);
		spawnItem(pointer.world(), itemStack, 6, direction, position);
		return stack;
	}

	/**
	 * Создаёт (спавнит) item.
	 *
	 * @param world world
	 * @param stack stack
	 * @param speed speed
	 * @param side side
	 * @param pos pos
	 */
	public static void spawnItem(World world, ItemStack stack, int speed, Direction side, Position pos) {
		double d = pos.getX();
		double e = pos.getY();
		double f = pos.getZ();
		if (side.getAxis() == Direction.Axis.Y) {
			e -= 0.125;
		}
		else {
			e -= 0.15625;
		}

		ItemEntity itemEntity = new ItemEntity(world, d, e, f, stack);
		double g = world.random.nextDouble() * 0.1 + 0.2;
		itemEntity.setVelocity(
				world.random.nextTriangular(side.getOffsetX() * g, 0.0172275 * speed),
				world.random.nextTriangular(0.2, 0.0172275 * speed),
				world.random.nextTriangular(side.getOffsetZ() * g, 0.0172275 * speed)
		);
		world.spawnEntity(itemEntity);
	}

	/**
	 * Play sound.
	 *
	 * @param pointer pointer
	 */
	protected void playSound(BlockPointer pointer) {
		syncDispensesEvent(pointer);
	}

	/**
	 * Создаёт (спавнит) particles.
	 *
	 * @param pointer pointer
	 * @param side side
	 */
	protected void spawnParticles(BlockPointer pointer, Direction side) {
		syncActivatesEvent(pointer, side);
	}

	private static void syncDispensesEvent(BlockPointer pointer) {
		pointer.world().syncWorldEvent(1000, pointer.pos(), 0);
	}

	private static void syncActivatesEvent(BlockPointer pointer, Direction side) {
		pointer.world().syncWorldEvent(2000, pointer.pos(), side.getIndex());
	}

	/**
	 * Decrement stack with remainder.
	 *
	 * @param pointer pointer
	 * @param stack stack
	 * @param remainder remainder
	 *
	 * @return ItemStack — результат операции
	 */
	protected ItemStack decrementStackWithRemainder(BlockPointer pointer, ItemStack stack, ItemStack remainder) {
		stack.decrement(1);
		if (stack.isEmpty()) {
			return remainder;
		}
		else {
			this.addStackOrSpawn(pointer, remainder);
			return stack;
		}
	}

	private void addStackOrSpawn(BlockPointer pointer, ItemStack stack) {
		ItemStack itemStack = pointer.blockEntity().addToFirstFreeSlot(stack);
		if (!itemStack.isEmpty()) {
			Direction direction = pointer.state().get(DispenserBlock.FACING);
			spawnItem(pointer.world(), itemStack, 6, direction, DispenserBlock.getOutputLocation(pointer));
			syncDispensesEvent(pointer);
			syncActivatesEvent(pointer, direction);
		}
	}
}
