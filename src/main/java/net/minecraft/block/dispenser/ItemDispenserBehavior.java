package net.minecraft.block.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * Базовая реализация {@link DispenserBehavior}: выбрасывает предмет в мир как {@link ItemEntity}.
 * Подклассы переопределяют {@link #dispenseSilently} для специфической логики,
 * а звук и частицы воспроизводятся автоматически через {@link #dispense}.
 */
public class ItemDispenserBehavior implements DispenserBehavior {

	private static final int ITEM_SPAWN_SPEED = 6;

	/** Код мирового события «диспенсер сработал» (звук). */
	private static final int DISPENSE_SOUND_EVENT = 1000;

	/** Код мирового события «диспенсер активирован» (частицы). */
	private static final int DISPENSE_PARTICLES_EVENT = 2000;

	/** Смещение Y для предметов, выброшенных по вертикальной оси. */
	private static final double VERTICAL_Y_OFFSET = 0.125;

	/** Смещение Y для предметов, выброшенных по горизонтальной оси. */
	private static final double HORIZONTAL_Y_OFFSET = 0.15625;

	/** Базовый разброс скорости выброса предмета. */
	private static final double BASE_VELOCITY_SPREAD = 0.1;

	/** Базовая скорость по оси Y при выбросе. */
	private static final double BASE_Y_VELOCITY = 0.2;

	/** Коэффициент случайного разброса скорости. */
	private static final double VELOCITY_RANDOMNESS = 0.0172275;

	@Override
	public final ItemStack dispense(BlockPointer pointer, ItemStack stack) {
		ItemStack result = dispenseSilently(pointer, stack);
		playSound(pointer);
		spawnParticles(pointer, pointer.state().get(DispenserBlock.FACING));
		return result;
	}

	/**
	 * Выполняет выброс предмета без звука и частиц.
	 * Переопределяется подклассами для специфической логики диспенсера.
	 */
	protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		Direction direction = pointer.state().get(DispenserBlock.FACING);
		Position outputPos = DispenserBlock.getOutputLocation(pointer);
		ItemStack single = stack.split(1);
		spawnItem(pointer.world(), single, ITEM_SPAWN_SPEED, direction, outputPos);
		return stack;
	}

	/**
	 * Спавнит предмет в мире с физическим разбросом скорости.
	 * Смещение по Y зависит от оси выброса: вертикальный выброс даёт меньшее смещение.
	 */
	public static void spawnItem(World world, ItemStack stack, int speed, Direction side, Position pos) {
		double x = pos.getX();
		double y = pos.getY() - (side.getAxis() == Direction.Axis.Y ? VERTICAL_Y_OFFSET : HORIZONTAL_Y_OFFSET);
		double z = pos.getZ();
		ItemEntity itemEntity = new ItemEntity(world, x, y, z, stack);
		double spread = world.random.nextDouble() * BASE_VELOCITY_SPREAD + BASE_VELOCITY_SPREAD;
		itemEntity.setVelocity(
				world.random.nextTriangular(side.getOffsetX() * spread, VELOCITY_RANDOMNESS * speed),
				world.random.nextTriangular(BASE_Y_VELOCITY, VELOCITY_RANDOMNESS * speed),
				world.random.nextTriangular(side.getOffsetZ() * spread, VELOCITY_RANDOMNESS * speed)
		);
		world.spawnEntity(itemEntity);
	}

	protected void playSound(BlockPointer pointer) {
		syncDispensesEvent(pointer);
	}

	protected void spawnParticles(BlockPointer pointer, Direction side) {
		syncActivatesEvent(pointer, side);
	}

	private static void syncDispensesEvent(BlockPointer pointer) {
		pointer.world().syncWorldEvent(DISPENSE_SOUND_EVENT, pointer.pos(), 0);
	}

	private static void syncActivatesEvent(BlockPointer pointer, Direction side) {
		pointer.world().syncWorldEvent(DISPENSE_PARTICLES_EVENT, pointer.pos(), side.getIndex());
	}

	protected ItemStack decrementStackWithRemainder(BlockPointer pointer, ItemStack stack, ItemStack remainder) {
		stack.decrement(1);
		if (stack.isEmpty()) {
			return remainder;
		}

		addStackOrSpawn(pointer, remainder);
		return stack;
	}

	private void addStackOrSpawn(BlockPointer pointer, ItemStack stack) {
		ItemStack leftover = pointer.blockEntity().addToFirstFreeSlot(stack);
		if (leftover.isEmpty()) {
			return;
		}

		Direction direction = pointer.state().get(DispenserBlock.FACING);
		spawnItem(pointer.world(), leftover, ITEM_SPAWN_SPEED, direction, DispenserBlock.getOutputLocation(pointer));
		syncDispensesEvent(pointer);
		syncActivatesEvent(pointer, direction);
	}
}
