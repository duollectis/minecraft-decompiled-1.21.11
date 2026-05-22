package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Утоптанная грунтовая тропа — блок на 1 пиксель ниже стандартного, превращается
 * в обычный грунт при размещении сверху твёрдого блока.
 */
public class DirtPathBlock extends Block {

	public static final MapCodec<DirtPathBlock> CODEC = createCodec(DirtPathBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 15.0);

	@Override
	public MapCodec<DirtPathBlock> getCodec() {
		return CODEC;
	}

	public DirtPathBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().canPlaceAt(ctx.getWorld(), ctx.getBlockPos())
			? super.getPlacementState(ctx)
			: Block.pushEntitiesUpBeforeBlockChange(
				getDefaultState(),
				Blocks.DIRT.getDefaultState(),
				ctx.getWorld(),
				ctx.getBlockPos()
			);
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
		if (direction == Direction.UP && state.canPlaceAt(world, pos) == false) {
			tickView.scheduleBlockTick(pos, this, 1);
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
		FarmlandBlock.setToDirt(null, state, world, pos);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState above = world.getBlockState(pos.up());

		return above.isSolid() == false || above.getBlock() instanceof FenceGateBlock;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
