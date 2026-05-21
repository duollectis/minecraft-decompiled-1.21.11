package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.PistonType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@code PistonHeadBlock}.
 */
public class PistonHeadBlock extends FacingBlock {

	public static final MapCodec<PistonHeadBlock> CODEC = createCodec(PistonHeadBlock::new);
	public static final EnumProperty<PistonType> TYPE = Properties.PISTON_TYPE;
	public static final BooleanProperty SHORT = Properties.SHORT;
	public static final int HEAD_LENGTH = 4;
	private static final VoxelShape BASE_SHAPE = Block.createCuboidZShape(16.0, 0.0, 4.0);
	private static final Map<Direction, VoxelShape> SHORT_SHAPES = VoxelShapes.createFacingShapeMap(
			VoxelShapes.union(BASE_SHAPE, Block.createCuboidZShape(4.0, 4.0, 16.0))
	);
	private static final Map<Direction, VoxelShape> LONG_SHAPES = VoxelShapes.createFacingShapeMap(
			VoxelShapes.union(BASE_SHAPE, Block.createCuboidZShape(4.0, 4.0, 20.0))
	);

	@Override
	protected MapCodec<PistonHeadBlock> getCodec() {
		return CODEC;
	}

	public PistonHeadBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(TYPE, PistonType.DEFAULT)
				.with(SHORT, false));
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return (state.get(SHORT) ? SHORT_SHAPES : LONG_SHAPES).get(state.get(FACING));
	}

	private boolean isAttached(BlockState headState, BlockState pistonState) {
		Block block = headState.get(TYPE) == PistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
		return pistonState.isOf(block) && pistonState.get(PistonBlock.EXTENDED)
				&& pistonState.get(FACING) == headState.get(FACING);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient() && player.shouldSkipBlockDrops()) {
			BlockPos blockPos = pos.offset(state.get(FACING).getOpposite());
			if (this.isAttached(state, world.getBlockState(blockPos))) {
				world.breakBlock(blockPos, false);
			}
		}

		return super.onBreak(world, pos, state, player);
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		BlockPos blockPos = pos.offset(state.get(FACING).getOpposite());
		if (this.isAttached(state, world.getBlockState(blockPos))) {
			world.breakBlock(blockPos, true);
		}
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
		return direction.getOpposite() == state.get(FACING) && !state.canPlaceAt(world, pos)
		       ? Blocks.AIR.getDefaultState()
		       : super.getStateForNeighborUpdate(
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
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos.offset(state.get(FACING).getOpposite()));
		return this.isAttached(state, blockState)
				|| blockState.isOf(Blocks.MOVING_PISTON) && blockState.get(FACING) == state.get(FACING);
	}

	@Override
	protected void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable WireOrientation wireOrientation,
			boolean notify
	) {
		if (state.canPlaceAt(world, pos)) {
			world.updateNeighbor(
					pos.offset(state.get(FACING).getOpposite()),
					sourceBlock,
					OrientationHelper.withFrontNullable(wireOrientation, state.get(FACING).getOpposite())
			);
		}
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(state.get(TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
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
		builder.add(FACING, TYPE, SHORT);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
