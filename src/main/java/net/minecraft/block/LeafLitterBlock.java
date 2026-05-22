package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

import java.util.function.Function;

/**
 * Блок опавших листьев — декоративное растение с поддержкой сегментации (1–4 листа в одном блоке).
 * Ориентируется горизонтально через {@link #HORIZONTAL_FACING} и реализует интерфейс {@link Segmented}
 * для постепенного заполнения блока при повторном размещении.
 */
public class LeafLitterBlock extends PlantBlock implements Segmented {

	public static final MapCodec<LeafLitterBlock> CODEC = createCodec(LeafLitterBlock::new);
	public static final EnumProperty<Direction> HORIZONTAL_FACING = Properties.HORIZONTAL_FACING;
	private final Function<BlockState, VoxelShape> shapeFunction;

	public LeafLitterBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
			.getDefaultState()
			.with(HORIZONTAL_FACING, Direction.NORTH)
			.with(getAmountProperty(), 1));
		shapeFunction = createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		return createShapeFunction(createShapeFunction(HORIZONTAL_FACING, getAmountProperty()));
	}

	@Override
	protected MapCodec<LeafLitterBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(HORIZONTAL_FACING, rotation.rotate(state.get(HORIZONTAL_FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(HORIZONTAL_FACING)));
	}

	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return this.shouldAddSegment(state, context, this.getAmountProperty()) ? true
		                                                                       : super.canReplace(state, context);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		return world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getPlacementState(ctx, this, this.getAmountProperty(), HORIZONTAL_FACING);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, this.getAmountProperty());
	}
}
