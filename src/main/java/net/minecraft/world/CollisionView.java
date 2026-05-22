package net.minecraft.world;

import com.google.common.collect.Iterables;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.border.WorldBorder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Интерфейс для проверки коллизий в мире: блоков, жидкостей, сущностей и границы мира.
 * Предоставляет методы для определения свободного пространства, поиска опорных блоков
 * и нахождения ближайшей точки столкновения.
 */
public interface CollisionView extends BlockView {

	WorldBorder getWorldBorder();

	@Nullable BlockView getChunkAsView(int chunkX, int chunkZ);

	default boolean doesNotIntersectEntities(@Nullable Entity except, VoxelShape shape) {
		return true;
	}

	default boolean canPlace(BlockState state, BlockPos pos, ShapeContext context) {
		VoxelShape shape = state.getCollisionShape(this, pos, context);
		return shape.isEmpty() || doesNotIntersectEntities(null, shape.offset(pos));
	}

	default boolean doesNotIntersectEntities(Entity entity) {
		return doesNotIntersectEntities(entity, VoxelShapes.cuboid(entity.getBoundingBox()));
	}

	default boolean isSpaceEmpty(Box box) {
		return isSpaceEmpty(null, box);
	}

	default boolean isSpaceEmpty(Entity entity) {
		return isSpaceEmpty(entity, entity.getBoundingBox());
	}

	default boolean isSpaceEmpty(@Nullable Entity entity, Box box) {
		return isSpaceEmpty(entity, box, false);
	}

	default boolean isSpaceEmpty(@Nullable Entity entity, Box box, boolean checkFluid) {
		return hasNoBlockOrFluidCollisions(entity, box, checkFluid)
			&& hasNoEntityCollisions(entity, box)
			&& isWithinWorldBorder(entity, box);
	}

	default boolean isBlockSpaceEmpty(@Nullable Entity entity, Box box) {
		return hasNoBlockOrFluidCollisions(entity, box, false);
	}

	default boolean hasNoBlockOrFluidCollisions(@Nullable Entity entity, Box box, boolean includeFluid) {
		Iterable<VoxelShape> shapes = includeFluid
			? getBlockOrFluidCollisions(entity, box)
			: getBlockCollisions(entity, box);

		for (VoxelShape shape : shapes) {
			if (!shape.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	default boolean hasNoEntityCollisions(@Nullable Entity entity, Box box) {
		return getEntityCollisions(entity, box).isEmpty();
	}

	default boolean isWithinWorldBorder(@Nullable Entity entity, Box box) {
		if (entity == null) {
			return true;
		}

		VoxelShape borderShape = getWorldBorderCollisions(entity, box);
		return borderShape == null
			|| !VoxelShapes.matchesAnywhere(borderShape, VoxelShapes.cuboid(box), BooleanBiFunction.AND);
	}

	List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box);

	default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, Box box) {
		List<VoxelShape> entityShapes = getEntityCollisions(entity, box);
		Iterable<VoxelShape> blockShapes = getBlockCollisions(entity, box);
		return entityShapes.isEmpty() ? blockShapes : Iterables.concat(entityShapes, blockShapes);
	}

	default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, Box box, Vec3d pos) {
		List<VoxelShape> entityShapes = getEntityCollisions(entity, box);
		Iterable<VoxelShape> blockShapes = getBlockOrFluidCollisions(ShapeContext.ofCollision(entity, pos.y), box);
		return entityShapes.isEmpty() ? blockShapes : Iterables.concat(entityShapes, blockShapes);
	}

	default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, Box box) {
		return getBlockOrFluidCollisions(
			entity == null ? ShapeContext.absent() : ShapeContext.of(entity),
			box
		);
	}

	default Iterable<VoxelShape> getBlockOrFluidCollisions(@Nullable Entity entity, Box box) {
		return getBlockOrFluidCollisions(
			entity == null ? ShapeContext.absentTreatingFluidAsCube() : ShapeContext.of(entity, true),
			box
		);
	}

	private Iterable<VoxelShape> getBlockOrFluidCollisions(ShapeContext shapeContext, Box box) {
		return () -> new BlockCollisionSpliterator<>(this, shapeContext, box, false, (pos, shape) -> shape);
	}

	private @Nullable VoxelShape getWorldBorderCollisions(Entity entity, Box box) {
		WorldBorder worldBorder = getWorldBorder();
		return worldBorder.canCollide(entity, box) ? worldBorder.asVoxelShape() : null;
	}

	/**
	 * Выполняет рейкаст с учётом границы мира: если луч выходит за границу,
	 * возвращает точку пересечения с границей вместо реального попадания.
	 */
	default BlockHitResult getCollisionsIncludingWorldBorder(RaycastContext context) {
		BlockHitResult hit = raycast(context);
		WorldBorder worldBorder = getWorldBorder();
		if (worldBorder.contains(context.getStart()) && !worldBorder.contains(hit.getPos())) {
			Vec3d direction = hit.getPos().subtract(context.getStart());
			Direction facing = Direction.getFacing(direction.x, direction.y, direction.z);
			Vec3d clampedPos = worldBorder.clamp(hit.getPos());
			return new BlockHitResult(clampedPos, facing, BlockPos.ofFloored(clampedPos), false, true);
		}

		return hit;
	}

	/**
	 * Проверяет, есть ли непустые коллизии блоков в заданном боксе.
	 * Использует BlockCollisionSpliterator с флагом includeFluid=true.
	 */
	default boolean canCollide(@Nullable Entity entity, Box box) {
		BlockCollisionSpliterator<VoxelShape> spliterator = new BlockCollisionSpliterator<>(
			this, entity, box, true, (pos, shape) -> shape
		);

		while (spliterator.hasNext()) {
			if (!spliterator.next().isEmpty()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Находит ближайший к сущности блок, на котором она стоит (опорный блок).
	 * Среди равноудалённых блоков выбирает наименьший по компаратору BlockPos.
	 */
	default Optional<BlockPos> findSupportingBlockPos(Entity entity, Box box) {
		BlockPos closestPos = null;
		double minDistSq = Double.MAX_VALUE;
		BlockCollisionSpliterator<BlockPos> spliterator = new BlockCollisionSpliterator<>(
			this, entity, box, false, (pos, shape) -> pos
		);

		while (spliterator.hasNext()) {
			BlockPos candidate = spliterator.next();
			double distSq = candidate.getSquaredDistance(entity.getEntityPos());
			if (distSq < minDistSq || (distSq == minDistSq && (closestPos == null || closestPos.compareTo(candidate) < 0))) {
				closestPos = candidate.toImmutable();
				minDistSq = distSq;
			}
		}

		return Optional.ofNullable(closestPos);
	}

	/**
	 * Находит ближайшую к target точку внутри shape, не перекрытую блоками мира.
	 * Расширяет бокс на (x, y, z) для учёта зазора вокруг формы.
	 */
	default Optional<Vec3d> findClosestCollision(
		@Nullable Entity entity,
		VoxelShape shape,
		Vec3d target,
		double x,
		double y,
		double z
	) {
		if (shape.isEmpty()) {
			return Optional.empty();
		}

		Box expandedBox = shape.getBoundingBox().expand(x, y, z);
		VoxelShape worldCollisions = StreamSupport
			.stream(getBlockCollisions(entity, expandedBox).spliterator(), false)
			.filter(collision -> getWorldBorder() == null || getWorldBorder().contains(collision.getBoundingBox()))
			.flatMap(collision -> collision.getBoundingBoxes().stream())
			.map(boxx -> boxx.expand(x / 2.0, y / 2.0, z / 2.0))
			.map(VoxelShapes::cuboid)
			.reduce(VoxelShapes.empty(), VoxelShapes::union);

		VoxelShape freeSpace = VoxelShapes.combineAndSimplify(shape, worldCollisions, BooleanBiFunction.ONLY_FIRST);
		return freeSpace.getClosestPointTo(target);
	}
}
