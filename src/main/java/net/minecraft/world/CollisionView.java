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
 * {@code CollisionView}.
 */
public interface CollisionView extends BlockView {

	WorldBorder getWorldBorder();

	@Nullable BlockView getChunkAsView(int chunkX, int chunkZ);

	default boolean doesNotIntersectEntities(@Nullable Entity except, VoxelShape shape) {
		return true;
	}

	default boolean canPlace(BlockState state, BlockPos pos, ShapeContext context) {
		VoxelShape voxelShape = state.getCollisionShape(this, pos, context);
		return voxelShape.isEmpty() || this.doesNotIntersectEntities(null, voxelShape.offset(pos));
	}

	default boolean doesNotIntersectEntities(Entity entity) {
		return this.doesNotIntersectEntities(entity, VoxelShapes.cuboid(entity.getBoundingBox()));
	}

	default boolean isSpaceEmpty(Box box) {
		return this.isSpaceEmpty(null, box);
	}

	default boolean isSpaceEmpty(Entity entity) {
		return this.isSpaceEmpty(entity, entity.getBoundingBox());
	}

	default boolean isSpaceEmpty(@Nullable Entity entity, Box box) {
		return this.isSpaceEmpty(entity, box, false);
	}

	default boolean isSpaceEmpty(@Nullable Entity entity, Box box, boolean checkFluid) {
		return this.hasNoBlockOrFluidCollisions(entity, box, checkFluid) && this.hasNoEntityCollisions(entity, box) && this.isWithinWorldBorder(
				entity,
				box
		);
	}

	default boolean isBlockSpaceEmpty(@Nullable Entity entity, Box box) {
		return this.hasNoBlockOrFluidCollisions(entity, box, false);
	}

	default boolean hasNoBlockOrFluidCollisions(@Nullable Entity entity, Box box, boolean bl) {
		for (VoxelShape voxelShape : bl ? this.getBlockOrFluidCollisions(entity, box)
		                                : this.getBlockCollisions(entity, box)) {
			if (!voxelShape.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	default boolean hasNoEntityCollisions(@Nullable Entity entity, Box box) {
		return this.getEntityCollisions(entity, box).isEmpty();
	}

	default boolean isWithinWorldBorder(@Nullable Entity entity, Box box) {
		if (entity == null) {
			return true;
		}
		else {
			VoxelShape voxelShape = this.getWorldBorderCollisions(entity, box);
			return voxelShape == null || !VoxelShapes.matchesAnywhere(
					voxelShape,
					VoxelShapes.cuboid(box),
					BooleanBiFunction.AND
			);
		}
	}

	List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box);

	default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, Box box) {
		List<VoxelShape> list = this.getEntityCollisions(entity, box);
		Iterable<VoxelShape> iterable = this.getBlockCollisions(entity, box);
		return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
	}

	default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, Box box, Vec3d pos) {
		List<VoxelShape> list = this.getEntityCollisions(entity, box);
		Iterable<VoxelShape> iterable = this.getBlockOrFluidCollisions(ShapeContext.ofCollision(entity, pos.y), box);
		return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
	}

	default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, Box box) {
		return this.getBlockOrFluidCollisions(entity == null ? ShapeContext.absent() : ShapeContext.of(entity), box);
	}

	default Iterable<VoxelShape> getBlockOrFluidCollisions(@Nullable Entity entity, Box box) {
		return this.getBlockOrFluidCollisions(
				entity == null ? ShapeContext.absentTreatingFluidAsCube() : ShapeContext.of(entity, true),
				box
		);
	}

	private Iterable<VoxelShape> getBlockOrFluidCollisions(ShapeContext shapeContext, Box box) {
		return () -> new BlockCollisionSpliterator<VoxelShape>(this, shapeContext, box, false, (pos, shape) -> shape);
	}

	private @Nullable VoxelShape getWorldBorderCollisions(Entity entity, Box box) {
		WorldBorder worldBorder = this.getWorldBorder();
		return worldBorder.canCollide(entity, box) ? worldBorder.asVoxelShape() : null;
	}

	default BlockHitResult getCollisionsIncludingWorldBorder(RaycastContext context) {
		BlockHitResult blockHitResult = this.raycast(context);
		WorldBorder worldBorder = this.getWorldBorder();
		if (worldBorder.contains(context.getStart()) && !worldBorder.contains(blockHitResult.getPos())) {
			Vec3d vec3d = blockHitResult.getPos().subtract(context.getStart());
			Direction direction = Direction.getFacing(vec3d.x, vec3d.y, vec3d.z);
			Vec3d vec3d2 = worldBorder.clamp(blockHitResult.getPos());
			return new BlockHitResult(vec3d2, direction, BlockPos.ofFloored(vec3d2), false, true);
		}
		else {
			return blockHitResult;
		}
	}

	default boolean canCollide(@Nullable Entity entity, Box box) {
		BlockCollisionSpliterator<VoxelShape> blockCollisionSpliterator = new BlockCollisionSpliterator<>(
				this, entity, box, true, (pos, voxelShape) -> voxelShape
		);

		while (blockCollisionSpliterator.hasNext()) {
			if (!((VoxelShape) blockCollisionSpliterator.next()).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	default Optional<BlockPos> findSupportingBlockPos(Entity entity, Box box) {
		BlockPos blockPos = null;
		double d = Double.MAX_VALUE;
		BlockCollisionSpliterator<BlockPos>
				blockCollisionSpliterator =
				new BlockCollisionSpliterator<>(this, entity, box, false, (pos, voxelShape) -> pos);

		while (blockCollisionSpliterator.hasNext()) {
			BlockPos blockPos2 = (BlockPos) blockCollisionSpliterator.next();
			double e = blockPos2.getSquaredDistance(entity.getEntityPos());
			if (e < d || e == d && (blockPos == null || blockPos.compareTo(blockPos2) < 0)) {
				blockPos = blockPos2.toImmutable();
				d = e;
			}
		}

		return Optional.ofNullable(blockPos);
	}

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
		else {
			Box box = shape.getBoundingBox().expand(x, y, z);
			VoxelShape voxelShape = StreamSupport.stream(this.getBlockCollisions(entity, box).spliterator(), false)
			                                     .filter(collision -> this.getWorldBorder() == null || this
					                                     .getWorldBorder()
					                                     .contains(collision.getBoundingBox()))
			                                     .flatMap(collision -> collision.getBoundingBoxes().stream())
			                                     .map(boxx -> boxx.expand(x / 2.0, y / 2.0, z / 2.0))
			                                     .map(VoxelShapes::cuboid)
			                                     .reduce(VoxelShapes.empty(), VoxelShapes::union);
			VoxelShape voxelShape2 = VoxelShapes.combineAndSimplify(shape, voxelShape, BooleanBiFunction.ONLY_FIRST);
			return voxelShape2.getClosestPointTo(target);
		}
	}
}
