package net.minecraft.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Базовый интерфейс для чтения состояний блоков, жидкостей и блок-сущностей.
 * Предоставляет алгоритмы рейкаста и обхода коллизий в пространстве блоков.
 */
public interface BlockView extends HeightLimitView, FabricBlockView {

	/** Малый эпсилон для смещения границ при обходе коллизий. */
	float COLLISION_EPSILON = 1.0E-5F;

	@Nullable BlockEntity getBlockEntity(BlockPos pos);

	default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
		BlockEntity blockEntity = getBlockEntity(pos);
		return blockEntity != null && blockEntity.getType() == type
			? Optional.of((T) blockEntity)
			: Optional.empty();
	}

	BlockState getBlockState(BlockPos pos);

	FluidState getFluidState(BlockPos pos);

	default int getLuminance(BlockPos pos) {
		return getBlockState(pos).getLuminance();
	}

	default Stream<BlockState> getStatesInBox(Box box) {
		return BlockPos.stream(box).map(this::getBlockState);
	}

	default BlockHitResult raycast(BlockStateRaycastContext context) {
		return raycast(
			context.getStart(),
			context.getEnd(),
			context,
			(innerContext, pos) -> {
				BlockState blockState = getBlockState(pos);
				Vec3d direction = innerContext.getStart().subtract(innerContext.getEnd());
				return innerContext.getStatePredicate().test(blockState)
					? new BlockHitResult(
						innerContext.getEnd(),
						Direction.getFacing(direction.x, direction.y, direction.z),
						BlockPos.ofFloored(innerContext.getEnd()),
						false
					)
					: null;
			},
			innerContext -> {
				Vec3d direction = innerContext.getStart().subtract(innerContext.getEnd());
				return BlockHitResult.createMissed(
					innerContext.getEnd(),
					Direction.getFacing(direction.x, direction.y, direction.z),
					BlockPos.ofFloored(innerContext.getEnd())
				);
			}
		);
	}

	/**
	 * Выполняет рейкаст через блоки с учётом форм коллизий блоков и жидкостей.
	 * Возвращает ближайшее пересечение из двух возможных (блок или жидкость).
	 */
	default BlockHitResult raycast(RaycastContext context) {
		return raycast(
			context.getStart(),
			context.getEnd(),
			context,
			(innerContext, pos) -> {
				BlockState blockState = getBlockState(pos);
				FluidState fluidState = getFluidState(pos);
				Vec3d start = innerContext.getStart();
				Vec3d end = innerContext.getEnd();

				VoxelShape blockShape = innerContext.getBlockShape(blockState, this, pos);
				BlockHitResult blockHit = raycastBlock(start, end, pos, blockShape, blockState);

				VoxelShape fluidShape = innerContext.getFluidShape(fluidState, this, pos);
				BlockHitResult fluidHit = fluidShape.raycast(start, end, pos);

				double blockDist = blockHit == null
					? Double.MAX_VALUE
					: innerContext.getStart().squaredDistanceTo(blockHit.getPos());
				double fluidDist = fluidHit == null
					? Double.MAX_VALUE
					: innerContext.getStart().squaredDistanceTo(fluidHit.getPos());

				return blockDist <= fluidDist ? blockHit : fluidHit;
			},
			innerContext -> {
				Vec3d direction = innerContext.getStart().subtract(innerContext.getEnd());
				return BlockHitResult.createMissed(
					innerContext.getEnd(),
					Direction.getFacing(direction.x, direction.y, direction.z),
					BlockPos.ofFloored(innerContext.getEnd())
				);
			}
		);
	}

	/**
	 * Выполняет рейкаст через конкретный блок, уточняя сторону попадания
	 * через форму рейкаста блока (может отличаться от формы коллизии).
	 */
	default @Nullable BlockHitResult raycastBlock(
		Vec3d start,
		Vec3d end,
		BlockPos pos,
		VoxelShape shape,
		BlockState state
	) {
		BlockHitResult hit = shape.raycast(start, end, pos);
		if (hit == null) {
			return null;
		}

		BlockHitResult refinedHit = state.getRaycastShape(this, pos).raycast(start, end, pos);
		if (refinedHit != null
			&& refinedHit.getPos().subtract(start).lengthSquared() < hit.getPos().subtract(start).lengthSquared()
		) {
			return hit.withSide(refinedHit.getSide());
		}

		return hit;
	}

	/**
	 * Вычисляет высоту для спешивания (dismount) с учётом формы блока под ногами.
	 * Если текущий блок пустой — берёт высоту блока снизу минус 1.
	 */
	default double getDismountHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockShapeGetter) {
		if (!blockCollisionShape.isEmpty()) {
			return blockCollisionShape.getMax(Direction.Axis.Y);
		}

		double belowMax = belowBlockShapeGetter.get().getMax(Direction.Axis.Y);
		return belowMax >= 1.0 ? belowMax - 1.0 : Double.NEGATIVE_INFINITY;
	}

	default double getDismountHeight(BlockPos pos) {
		return getDismountHeight(
			getBlockState(pos).getCollisionShape(this, pos),
			() -> {
				BlockPos below = pos.down();
				return getBlockState(below).getCollisionShape(this, below);
			}
		);
	}

	/**
	 * Универсальный алгоритм рейкаста по сетке блоков (DDA — Digital Differential Analyzer).
	 * Проходит по всем блокам вдоль луча от start до end, вызывая blockHitFactory для каждого.
	 * Возвращает первый ненулевой результат или результат missFactory при промахе.
	 */
	static <T, C> T raycast(
		Vec3d start,
		Vec3d end,
		C context,
		BiFunction<C, BlockPos, @Nullable T> blockHitFactory,
		Function<C, T> missFactory
	) {
		if (start.equals(end)) {
			return missFactory.apply(context);
		}

		// Небольшое смещение внутрь для избежания граничных артефактов
		double endX = MathHelper.lerp(-1.0E-7, end.x, start.x);
		double endY = MathHelper.lerp(-1.0E-7, end.y, start.y);
		double endZ = MathHelper.lerp(-1.0E-7, end.z, start.z);
		double startX = MathHelper.lerp(-1.0E-7, start.x, end.x);
		double startY = MathHelper.lerp(-1.0E-7, start.y, end.y);
		double startZ = MathHelper.lerp(-1.0E-7, start.z, end.z);

		int blockX = MathHelper.floor(startX);
		int blockY = MathHelper.floor(startY);
		int blockZ = MathHelper.floor(startZ);
		BlockPos.Mutable mutable = new BlockPos.Mutable(blockX, blockY, blockZ);

		T initialHit = blockHitFactory.apply(context, mutable);
		if (initialHit != null) {
			return initialHit;
		}

		double deltaX = endX - startX;
		double deltaY = endY - startY;
		double deltaZ = endZ - startZ;
		int stepX = MathHelper.sign(deltaX);
		int stepY = MathHelper.sign(deltaY);
		int stepZ = MathHelper.sign(deltaZ);
		double tDeltaX = stepX == 0 ? Double.MAX_VALUE : stepX / deltaX;
		double tDeltaY = stepY == 0 ? Double.MAX_VALUE : stepY / deltaY;
		double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : stepZ / deltaZ;
		double tMaxX = tDeltaX * (stepX > 0 ? 1.0 - MathHelper.fractionalPart(startX) : MathHelper.fractionalPart(startX));
		double tMaxY = tDeltaY * (stepY > 0 ? 1.0 - MathHelper.fractionalPart(startY) : MathHelper.fractionalPart(startY));
		double tMaxZ = tDeltaZ * (stepZ > 0 ? 1.0 - MathHelper.fractionalPart(startZ) : MathHelper.fractionalPart(startZ));

		while (tMaxX <= 1.0 || tMaxY <= 1.0 || tMaxZ <= 1.0) {
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					blockX += stepX;
					tMaxX += tDeltaX;
				} else {
					blockZ += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else if (tMaxY < tMaxZ) {
				blockY += stepY;
				tMaxY += tDeltaY;
			} else {
				blockZ += stepZ;
				tMaxZ += tDeltaZ;
			}

			T hit = blockHitFactory.apply(context, mutable.set(blockX, blockY, blockZ));
			if (hit != null) {
				return hit;
			}
		}

		return missFactory.apply(context);
	}

	/**
	 * Собирает все блоки, которые пересекает движущийся AABB-бокс при перемещении из from в to.
	 * Использует двухпроходный алгоритм: сначала обходит блоки у начальной позиции,
	 * затем — у конечной, избегая дублирования через LongSet.
	 */
	static boolean collectCollisionsBetween(Vec3d from, Vec3d to, Box box, BlockView.CollisionVisitor visitor) {
		Vec3d delta = to.subtract(from);
		if (delta.lengthSquared() < MathHelper.square(COLLISION_EPSILON)) {
			for (BlockPos pos : BlockPos.iterate(box)) {
				if (!visitor.visit(pos, 0)) {
					return false;
				}
			}

			return true;
		}

		LongSet visited = new LongOpenHashSet();

		for (BlockPos pos : BlockPos.iterateCollisionOrder(box.offset(delta.multiply(-1.0)), delta)) {
			if (!visitor.visit(pos, 0)) {
				return false;
			}

			visited.add(pos.asLong());
		}

		int crossingCount = collectCollisionsBetween(visited, delta, box, visitor);
		if (crossingCount < 0) {
			return false;
		}

		for (BlockPos pos : BlockPos.iterateCollisionOrder(box, delta)) {
			if (visited.add(pos.asLong()) && !visitor.visit(pos, crossingCount + 1)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Внутренний проход: обходит блоки вдоль траектории центра AABB-бокса,
	 * собирая блоки в промежуточных позициях пересечения граней.
	 * Возвращает количество пересечений или -1 при прерывании.
	 */
	private static int collectCollisionsBetween(
		LongSet visited,
		Vec3d delta,
		Box box,
		BlockView.CollisionVisitor visitor
	) {
		double sizeX = box.getLengthX();
		double sizeY = box.getLengthY();
		double sizeZ = box.getLengthZ();
		Vec3i stepDir = toCollisionStepDirection(delta);
		Vec3d center = box.getCenter();
		Vec3d leadCorner = new Vec3d(
			center.getX() + sizeX * 0.5 * stepDir.getX(),
			center.getY() + sizeY * 0.5 * stepDir.getY(),
			center.getZ() + sizeZ * 0.5 * stepDir.getZ()
		);
		Vec3d trailCorner = leadCorner.subtract(delta);

		int blockX = MathHelper.floor(trailCorner.x);
		int blockY = MathHelper.floor(trailCorner.y);
		int blockZ = MathHelper.floor(trailCorner.z);
		int stepX = MathHelper.sign(delta.x);
		int stepY = MathHelper.sign(delta.y);
		int stepZ = MathHelper.sign(delta.z);
		double tDeltaX = stepX == 0 ? Double.MAX_VALUE : stepX / delta.x;
		double tDeltaY = stepY == 0 ? Double.MAX_VALUE : stepY / delta.y;
		double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : stepZ / delta.z;
		double tMaxX = tDeltaX * (stepX > 0
			? 1.0 - MathHelper.fractionalPart(trailCorner.x)
			: MathHelper.fractionalPart(trailCorner.x));
		double tMaxY = tDeltaY * (stepY > 0
			? 1.0 - MathHelper.fractionalPart(trailCorner.y)
			: MathHelper.fractionalPart(trailCorner.y));
		double tMaxZ = tDeltaZ * (stepZ > 0
			? 1.0 - MathHelper.fractionalPart(trailCorner.z)
			: MathHelper.fractionalPart(trailCorner.z));
		int crossingCount = 0;

		while (tMaxX <= 1.0 || tMaxY <= 1.0 || tMaxZ <= 1.0) {
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					blockX += stepX;
					tMaxX += tDeltaX;
				} else {
					blockZ += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else if (tMaxY < tMaxZ) {
				blockY += stepY;
				tMaxY += tDeltaY;
			} else {
				blockZ += stepZ;
				tMaxZ += tDeltaZ;
			}

			Optional<Vec3d> intersection = Box.raycast(
				blockX, blockY, blockZ,
				blockX + 1, blockY + 1, blockZ + 1,
				trailCorner, leadCorner
			);
			if (intersection.isEmpty()) {
				continue;
			}

			crossingCount++;
			Vec3d point = intersection.get();
			double clampedX = MathHelper.clamp(point.x, blockX + COLLISION_EPSILON, blockX + 1.0 - COLLISION_EPSILON);
			double clampedY = MathHelper.clamp(point.y, blockY + COLLISION_EPSILON, blockY + 1.0 - COLLISION_EPSILON);
			double clampedZ = MathHelper.clamp(point.z, blockZ + COLLISION_EPSILON, blockZ + 1.0 - COLLISION_EPSILON);
			int cornerX = MathHelper.floor(clampedX - sizeX * stepDir.getX());
			int cornerY = MathHelper.floor(clampedY - sizeY * stepDir.getY());
			int cornerZ = MathHelper.floor(clampedZ - sizeZ * stepDir.getZ());
			int currentCrossing = crossingCount;

			for (BlockPos pos : BlockPos.iterateCollisionOrder(blockX, blockY, blockZ, cornerX, cornerY, cornerZ, delta)) {
				if (visited.add(pos.asLong()) && !visitor.visit(pos, currentCrossing)) {
					return -1;
				}
			}
		}

		return crossingCount;
	}

	/**
	 * Определяет направление обхода коллизий на основе вектора движения.
	 * Возвращает Vec3i с компонентами ±1, указывающими ведущий угол AABB.
	 */
	private static Vec3i toCollisionStepDirection(Vec3d delta) {
		double absX = Math.abs(Vec3d.X.dotProduct(delta));
		double absY = Math.abs(Vec3d.Y.dotProduct(delta));
		double absZ = Math.abs(Vec3d.Z.dotProduct(delta));
		int signX = delta.x >= 0.0 ? 1 : -1;
		int signY = delta.y >= 0.0 ? 1 : -1;
		int signZ = delta.z >= 0.0 ? 1 : -1;

		if (absX <= absY && absX <= absZ) {
			return new Vec3i(-signX, -signZ, signY);
		}

		return absY <= absZ
			? new Vec3i(signZ, -signY, -signX)
			: new Vec3i(-signY, signX, -signZ);
	}

	/** Функциональный интерфейс для посещения блоков при обходе коллизий. */
	@FunctionalInterface
	interface CollisionVisitor {

		/**
		 * @param pos     позиция блока
		 * @param version порядковый номер пересечения (0 — начальная позиция)
		 * @return {@code true} чтобы продолжить обход, {@code false} чтобы прервать
		 */
		boolean visit(BlockPos pos, int version);
	}
}
