package net.minecraft.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Утилитарный класс для расчёта позиций высадки сущностей с транспортных средств.
 * Содержит алгоритмы поиска безопасных позиций для спавна и высадки,
 * а также проверки коллизий при размещении сущности в мире.
 */
public class Dismounting {

	/**
	 * Возвращает массив смещений для поиска позиций высадки вокруг транспортного средства.
	 * Смещения расположены по спирали: сначала по бокам, затем по диагоналям,
	 * затем сзади и спереди — для нахождения ближайшей свободной позиции.
	 *
	 * @param movementDirection направление движения транспортного средства
	 * @return массив пар [offsetX, offsetZ] для проверки позиций высадки
	 */
	public static int[][] getDismountOffsets(Direction movementDirection) {
		Direction right = movementDirection.rotateYClockwise();
		Direction left = right.getOpposite();
		Direction back = movementDirection.getOpposite();

		return new int[][]{
			{right.getOffsetX(), right.getOffsetZ()},
			{left.getOffsetX(), left.getOffsetZ()},
			{back.getOffsetX() + right.getOffsetX(), back.getOffsetZ() + right.getOffsetZ()},
			{back.getOffsetX() + left.getOffsetX(), back.getOffsetZ() + left.getOffsetZ()},
			{movementDirection.getOffsetX() + right.getOffsetX(), movementDirection.getOffsetZ() + right.getOffsetZ()},
			{movementDirection.getOffsetX() + left.getOffsetX(), movementDirection.getOffsetZ() + left.getOffsetZ()},
			{back.getOffsetX(), back.getOffsetZ()},
			{movementDirection.getOffsetX(), movementDirection.getOffsetZ()}
		};
	}

	/**
	 * Проверяет, можно ли высадить сущность на блоке с данной высотой поверхности.
	 * Бесконечная высота означает отсутствие твёрдой поверхности (пропасть),
	 * высота ≥ 1.0 означает, что блок слишком высокий для высадки.
	 *
	 * @param height высота поверхности блока
	 * @return {@code true} если высадка возможна
	 */
	public static boolean canDismountInBlock(double height) {
		return !Double.isInfinite(height) && height < 1.0;
	}

	/**
	 * Проверяет, можно ли разместить сущность в указанном ограничивающем прямоугольнике
	 * без коллизий с блоками и за пределами границы мира.
	 *
	 * @param world     мир для проверки коллизий
	 * @param entity    размещаемая сущность
	 * @param targetBox целевой ограничивающий прямоугольник
	 * @return {@code true} если размещение возможно
	 */
	public static boolean canPlaceEntityAt(CollisionView world, LivingEntity entity, Box targetBox) {
		for (VoxelShape shape : world.getBlockCollisions(entity, targetBox)) {
			if (!shape.isEmpty()) {
				return false;
			}
		}

		return world.getWorldBorder().contains(targetBox);
	}

	public static boolean canPlaceEntityAt(CollisionView world, Vec3d offset, LivingEntity entity, EntityPose pose) {
		return canPlaceEntityAt(world, entity, entity.getBoundingBox(pose).offset(offset));
	}

	/**
	 * Возвращает форму коллизии блока для расчёта высадки.
	 * Лестницы и открытые люки считаются пустыми — сущность может через них пройти.
	 *
	 * @param world мир
	 * @param pos   позиция блока
	 * @return форма коллизии или пустая форма для проходимых блоков
	 */
	public static VoxelShape getCollisionShape(BlockView world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		boolean isClimbable = state.isIn(BlockTags.CLIMBABLE);
		boolean isOpenTrapdoor = state.getBlock() instanceof TrapdoorBlock && state.get(TrapdoorBlock.OPEN);

		return isClimbable || isOpenTrapdoor
			? VoxelShapes.empty()
			: state.getCollisionShape(world, pos);
	}

	/**
	 * Находит высоту потолка над заданной позицией, двигаясь вверх до {@code maxDistance} блоков.
	 *
	 * @param pos                  начальная позиция
	 * @param maxDistance          максимальное расстояние поиска вверх
	 * @param collisionShapeGetter функция получения формы коллизии по позиции
	 * @return Y-координата нижней грани первого непустого блока, или {@link Double#POSITIVE_INFINITY}
	 */
	public static double getCeilingHeight(
		BlockPos pos,
		int maxDistance,
		Function<BlockPos, VoxelShape> collisionShapeGetter
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int offset = 0; offset < maxDistance; offset++) {
			VoxelShape shape = collisionShapeGetter.apply(mutable);
			if (!shape.isEmpty()) {
				return pos.getY() + offset + shape.getMin(Direction.Axis.Y);
			}

			mutable.move(Direction.UP);
		}

		return Double.POSITIVE_INFINITY;
	}

	/**
	 * Ищет безопасную позицию для спавна/телепортации сущности на заданном блоке.
	 * Учитывает коллизии, границу мира и запрещённые блоки для спавна игрока.
	 *
	 * @param entityType        тип сущности для проверки размеров и ограничений спавна
	 * @param world             мир
	 * @param pos               целевая позиция блока
	 * @param ignoreInvalidPos  если {@code true}, проверяет запрещённые блоки спавна
	 * @return позиция центра блока с учётом высоты поверхности, или {@code null} если позиция недоступна
	 */
	public static @Nullable Vec3d findRespawnPos(
		EntityType<?> entityType,
		CollisionView world,
		BlockPos pos,
		boolean ignoreInvalidPos
	) {
		if (ignoreInvalidPos && entityType.isInvalidSpawn(world.getBlockState(pos))) {
			return null;
		}

		double height = world.getDismountHeight(
			getCollisionShape(world, pos),
			() -> getCollisionShape(world, pos.down())
		);

		if (!canDismountInBlock(height)) {
			return null;
		}

		if (ignoreInvalidPos && height <= 0.0 && entityType.isInvalidSpawn(world.getBlockState(pos.down()))) {
			return null;
		}

		Vec3d center = Vec3d.ofCenter(pos, height);
		Box box = entityType.getDimensions().getBoxAt(center);

		for (VoxelShape shape : world.getBlockCollisions(null, box)) {
			if (!shape.isEmpty()) {
				return null;
			}
		}

		if (entityType == EntityType.PLAYER) {
			boolean currentInvalid = world.getBlockState(pos).isIn(BlockTags.INVALID_SPAWN_INSIDE);
			boolean aboveInvalid = world.getBlockState(pos.up()).isIn(BlockTags.INVALID_SPAWN_INSIDE);

			if (currentInvalid || aboveInvalid) {
				return null;
			}
		}

		return world.getWorldBorder().contains(box) ? center : null;
	}
}
