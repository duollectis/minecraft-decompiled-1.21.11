package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Мёртвый коралловый веер на стене — крепится к горизонтальной поверхности,
 * не требует воды для существования.
 */
public class DeadCoralWallFanBlock extends DeadCoralFanBlock {

	public static final MapCodec<DeadCoralWallFanBlock> CODEC = createCodec(DeadCoralWallFanBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = VoxelShapes.createHorizontalFacingShapeMap(
			Block.createCuboidZShape(16.0, 8.0, 5.0, 16.0)
	);

	@Override
	public MapCodec<? extends DeadCoralWallFanBlock> getCodec() {
		return CODEC;
	}

	public DeadCoralWallFanBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, true));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_DIRECTION.get(state.get(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return direction.getOpposite() == state.get(FACING) && !state.canPlaceAt(world, pos)
		       ? Blocks.AIR.getDefaultState() : state;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction facing = state.get(FACING);
		BlockPos supportPos = pos.offset(facing.getOpposite());
		BlockState support = world.getBlockState(supportPos);

		return support.isSideSolidFullSquare(world, supportPos, facing);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState candidate = super.getPlacementState(ctx);
		WorldView world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();

		for (Direction direction : ctx.getPlacementDirections()) {
			if (direction.getAxis().isHorizontal() == false) {
				continue;
			}

			candidate = candidate.with(FACING, direction.getOpposite());

			if (candidate.canPlaceAt(world, pos)) {
				return candidate;
			}
		}

		return null;
	}
}
