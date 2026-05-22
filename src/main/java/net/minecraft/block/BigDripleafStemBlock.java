package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;
import java.util.Optional;

/**
 * Стебель большого листа капли. Промежуточный блок колонии {@link BigDripleafBlock}:
 * должен иметь снизу другой стебель или подходящий блок ({@code BIG_DRIPLEAF_PLACEABLE}),
 * а сверху — стебель или лист. При нарушении опоры планирует разрушение через 1 тик.
 */
public class BigDripleafStemBlock extends HorizontalFacingBlock implements Fertilizable, Waterloggable {

	public static final MapCodec<BigDripleafStemBlock> CODEC = createCodec(BigDripleafStemBlock::new);
	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = VoxelShapes.createHorizontalFacingShapeMap(
			Block.createColumnShape(6.0, 0.0, 16.0).offset(0.0, 0.0, 0.25).simplify()
	);

	@Override
	public MapCodec<BigDripleafStemBlock> getCodec() {
		return CODEC;
	}

	public BigDripleafStemBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
				.getDefaultState()
				.with(WATERLOGGED, false)
				.with(FACING, Direction.NORTH));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_DIRECTION.get(state.get(FACING));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, FACING);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState belowState = world.getBlockState(pos.down());
		BlockState aboveState = world.getBlockState(pos.up());

		boolean hasValidBase = belowState.isOf(this) || belowState.isIn(BlockTags.BIG_DRIPLEAF_PLACEABLE);
		boolean hasValidTop = aboveState.isOf(this) || aboveState.isOf(Blocks.BIG_DRIPLEAF);

		return hasValidBase && hasValidTop;
	}

	protected static boolean placeStemAt(WorldAccess world, BlockPos pos, FluidState fluidState, Direction direction) {
		BlockState stemState = Blocks.BIG_DRIPLEAF_STEM
				.getDefaultState()
				.with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))
				.with(FACING, direction);

		return world.setBlockState(pos, stemState, 3);
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
		boolean isVertical = direction == Direction.DOWN || direction == Direction.UP;

		if (isVertical && state.canPlaceAt(world, pos) == false) {
			tickView.scheduleBlockTick(pos, this, 1);
		}

		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return super.getStateForNeighborUpdate(
				state,
				world,
				tickView,
				pos,
				direction,
				neighborPos,
				neighborState,
				random
		);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.canPlaceAt(world, pos) == false) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		Optional<BlockPos> leafPos = BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);

		if (leafPos.isEmpty()) {
			return false;
		}

		BlockPos aboveLeaf = leafPos.get().up();
		BlockState aboveState = world.getBlockState(aboveLeaf);

		return BigDripleafBlock.canGrowInto(world, aboveLeaf, aboveState);
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		Optional<BlockPos> leafPos = BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);

		if (leafPos.isEmpty()) {
			return;
		}

		BlockPos topLeaf = leafPos.get();
		BlockPos aboveLeaf = topLeaf.up();
		Direction facing = state.get(FACING);

		placeStemAt(world, topLeaf, world.getFluidState(topLeaf), facing);
		BigDripleafBlock.placeDripleafAt(world, aboveLeaf, world.getFluidState(aboveLeaf), facing);
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Blocks.BIG_DRIPLEAF);
	}
}
