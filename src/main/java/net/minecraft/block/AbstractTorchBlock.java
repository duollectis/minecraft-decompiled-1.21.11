package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * {@code AbstractTorchBlock}.
 */
public abstract class AbstractTorchBlock extends Block {

	private static final VoxelShape SHAPE = Block.createColumnShape(4.0, 0.0, 10.0);

	protected AbstractTorchBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected abstract MapCodec<? extends AbstractTorchBlock> getCodec();

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
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
		return direction == Direction.DOWN && !this.canPlaceAt(state, world, pos)
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
		return sideCoversSmallSquare(world, pos.down(), Direction.UP);
	}
}
