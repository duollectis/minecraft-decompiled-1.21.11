package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * {@code CoralBlock}.
 */
public class CoralBlock extends AbstractCoralBlock {

	public static final MapCodec<CoralBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(CoralBlockBlock.DEAD_FIELD.forGetter(block -> block.deadCoralBlock), createSettingsCodec())
					.apply(instance, CoralBlock::new)
	);
	private final Block deadCoralBlock;
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 15.0);

	@Override
	public MapCodec<CoralBlock> getCodec() {
		return CODEC;
	}

	public CoralBlock(Block deadCoralBlock, AbstractBlock.Settings settings) {
		super(settings);
		this.deadCoralBlock = deadCoralBlock;
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		this.checkLivingConditions(state, world, world, world.random, pos);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!isInWater(state, world, pos)) {
			world.setBlockState(pos, this.deadCoralBlock.getDefaultState().with(WATERLOGGED, false), 2);
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
		if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		else {
			this.checkLivingConditions(state, world, tickView, random, pos);
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
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}
