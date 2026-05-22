package net.minecraft.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Утилита для разбрасывания предметов в мире при разрушении блоков или смерти сущностей.
 * Предметы спавнятся с небольшим случайным смещением и скоростью для реалистичного рассеивания.
 */
public class ItemScatterer {

	/** Девиация треугольного распределения скорости разлёта предметов (≈ 0.1149). */
	private static final double VELOCITY_DEVIATION = 0.11485000171139836;

	/** Средняя скорость разлёта предметов по вертикали. */
	private static final double VERTICAL_VELOCITY_MEAN = 0.2;

	/** Минимальное количество предметов в одном стаке при разделении. */
	private static final int SPLIT_MIN = 10;

	/** Диапазон случайного добавления к минимальному количеству при разделении. */
	private static final int SPLIT_RANGE = 21;

	/**
	 * Выбрасывает все предметы из инвентаря в позиции блока.
	 *
	 * @param world мир, в котором спавнятся предметы
	 * @param pos позиция блока, из которого выбрасываются предметы
	 * @param inventory инвентарь, содержимое которого нужно рассеять
	 */
	public static void spawn(World world, BlockPos pos, Inventory inventory) {
		spawn(world, pos.getX(), pos.getY(), pos.getZ(), inventory);
	}

	/**
	 * Выбрасывает все предметы из инвентаря в позиции сущности.
	 *
	 * @param world мир, в котором спавнятся предметы
	 * @param entity сущность, в позиции которой выбрасываются предметы
	 * @param inventory инвентарь, содержимое которого нужно рассеять
	 */
	public static void spawn(World world, Entity entity, Inventory inventory) {
		spawn(world, entity.getX(), entity.getY(), entity.getZ(), inventory);
	}

	private static void spawn(World world, double x, double y, double z, Inventory inventory) {
		for (int slot = 0; slot < inventory.size(); slot++) {
			spawn(world, x, y, z, inventory.getStack(slot));
		}
	}

	/**
	 * Выбрасывает все предметы из списка в позиции блока.
	 *
	 * @param world мир, в котором спавнятся предметы
	 * @param pos позиция блока
	 * @param stacks список стаков предметов для рассеивания
	 */
	public static void spawn(World world, BlockPos pos, DefaultedList<ItemStack> stacks) {
		stacks.forEach(stack -> spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack));
	}

	/**
	 * Выбрасывает стак предметов в указанных координатах, разбивая его на случайные части.
	 * Каждая часть получает случайную скорость по треугольному распределению для реалистичного рассеивания.
	 *
	 * @param world мир, в котором спавнятся предметы
	 * @param x координата X
	 * @param y координата Y
	 * @param z координата Z
	 * @param stack стак предметов для выброса
	 */
	public static void spawn(World world, double x, double y, double z, ItemStack stack) {
		double itemWidth = EntityType.ITEM.getWidth();
		double freeSpace = 1.0 - itemWidth;
		double halfWidth = itemWidth / 2.0;

		double spawnX = Math.floor(x) + world.random.nextDouble() * freeSpace + halfWidth;
		double spawnY = Math.floor(y) + world.random.nextDouble() * freeSpace;
		double spawnZ = Math.floor(z) + world.random.nextDouble() * freeSpace + halfWidth;

		while (!stack.isEmpty()) {
			ItemEntity itemEntity = new ItemEntity(
				world,
				spawnX,
				spawnY,
				spawnZ,
				stack.split(world.random.nextInt(SPLIT_RANGE) + SPLIT_MIN)
			);

			itemEntity.setVelocity(
				world.random.nextTriangular(0.0, VELOCITY_DEVIATION),
				world.random.nextTriangular(VERTICAL_VELOCITY_MEAN, VELOCITY_DEVIATION),
				world.random.nextTriangular(0.0, VELOCITY_DEVIATION)
			);

			world.spawnEntity(itemEntity);
		}
	}

	/**
	 * Обновляет компараторы при замене состояния блока.
	 * Вызывается при разрушении блока, содержащего инвентарь.
	 *
	 * @param state предыдущее состояние блока
	 * @param world мир
	 * @param pos позиция блока
	 */
	public static void onStateReplaced(BlockState state, World world, BlockPos pos) {
		world.updateComparators(pos, state.getBlock());
	}
}
