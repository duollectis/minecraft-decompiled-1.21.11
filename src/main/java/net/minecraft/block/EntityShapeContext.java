package net.minecraft.block;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.CollisionView;
import org.jspecify.annotations.Nullable;

/**
 * {@code EntityShapeContext}.
 */
public class EntityShapeContext implements ShapeContext {

	private final boolean descending;
	private final double minY;
	private final boolean placement;
	private final ItemStack heldItem;
	private final boolean shouldTreatFluidAsCube;
	private final @Nullable Entity entity;

	protected EntityShapeContext(
			boolean descending,
			boolean placement,
			double minY,
			ItemStack heldItem,
			boolean shouldTreatFluidAsCube,
			@Nullable Entity entity
	) {
		this.descending = descending;
		this.placement = placement;
		this.minY = minY;
		this.heldItem = heldItem;
		this.shouldTreatFluidAsCube = shouldTreatFluidAsCube;
		this.entity = entity;
	}

	@Deprecated
	protected EntityShapeContext(Entity entity, boolean shouldTreatFluidAsCube, boolean placement) {
		this(
				entity.isDescending(),
				placement,
				entity.getY(),
				entity instanceof LivingEntity livingEntity ? livingEntity.getMainHandStack() : ItemStack.EMPTY,
				shouldTreatFluidAsCube,
				entity
		);
	}

	@Override
	public boolean isHolding(Item item) {
		return this.heldItem.isOf(item);
	}

	@Override
	public boolean shouldTreatFluidAsCube() {
		return this.shouldTreatFluidAsCube;
	}

	@Override
	public boolean canWalkOnFluid(FluidState stateAbove, FluidState state) {
		return !(this.entity instanceof LivingEntity livingEntity)
		       ? false
		       : livingEntity.canWalkOnFluid(state) && !stateAbove.getFluid().matchesType(state.getFluid());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, CollisionView world, BlockPos pos) {
		return state.getCollisionShape(world, pos, this);
	}

	@Override
	public boolean isDescending() {
		return this.descending;
	}

	@Override
	public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
		return this.minY > pos.getY() + shape.getMax(Direction.Axis.Y) - 1.0E-5F;
	}

	public @Nullable Entity getEntity() {
		return this.entity;
	}

	@Override
	public boolean isPlacement() {
		return this.placement;
	}

	/**
	 * {@code Absent}.
	 */
	protected static class Absent extends EntityShapeContext {

		protected static final ShapeContext INSTANCE = new EntityShapeContext.Absent(false);
		protected static final ShapeContext TREAT_FLUID_AS_CUBE = new EntityShapeContext.Absent(true);

		public Absent(boolean shouldTreatFluidAsCube) {
			super(false, false, -Double.MAX_VALUE, ItemStack.EMPTY, shouldTreatFluidAsCube, null);
		}

		@Override
		public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
			return defaultValue;
		}
	}
}
