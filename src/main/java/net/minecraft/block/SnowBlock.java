package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * {@code SnowBlock}.
 */
public class SnowBlock extends Block {

	public static final MapCodec<SnowBlock> CODEC = createCodec(SnowBlock::new);
	public static final int MAX_LAYERS = 8;
	public static final IntProperty LAYERS = Properties.LAYERS;
	private static final VoxelShape[]
			SHAPES_BY_LAYERS =
			Block.createShapeArray(8, layers -> Block.createColumnShape(16.0, 0.0, layers * 2));
	public static final int PATHFINDING_MAX_LAYERS = 5;

	@Override
	public MapCodec<SnowBlock> getCodec() {
		return CODEC;
	}

	public SnowBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(LAYERS, 1));
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return type == NavigationType.LAND ? state.get(LAYERS) < 5 : false;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_LAYERS[state.get(LAYERS)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_LAYERS[state.get(LAYERS) - 1];
	}

	@Override
	protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
		return SHAPES_BY_LAYERS[state.get(LAYERS)];
	}

	@Override
	protected VoxelShape getCameraCollisionShape(
			BlockState state,
			BlockView world,
			BlockPos pos,
			ShapeContext context
	) {
		return SHAPES_BY_LAYERS[state.get(LAYERS)];
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return state.get(LAYERS) == 8 ? 0.2F : 1.0F;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos.down());
		if (blockState.isIn(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
			return false;
		}
		else {
			return blockState.isIn(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)
			       ? true
			       : Block.isFaceFullSquare(blockState.getCollisionShape(world, pos.down()), Direction.UP)
			         || blockState.isOf(this) && blockState.get(LAYERS) == 8;
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
		return !state.canPlaceAt(world, pos)
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
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getLightLevel(LightType.BLOCK, pos) > 11) {
			dropStacks(state, world, pos);
			world.removeBlock(pos, false);
		}
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		int i = state.get(LAYERS);
		if (!context.getStack().isOf(this.asItem()) || i >= 8) {
			return i == 1;
		}
		else {
			return context.canReplaceExisting() ? context.getSide() == Direction.UP : true;
		}
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (blockState.isOf(this)) {
			int i = blockState.get(LAYERS);
			return blockState.with(LAYERS, Math.min(8, i + 1));
		}
		else {
			return super.getPlacementState(ctx);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LAYERS);
	}
}
