package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Блок кактуса. Растёт вверх при случайных тиках, достигая максимальной высоты 3 блока.
 * При достижении возраста {@link #FLOWER_GROWTH_AGE} может вырастить цветок кактуса.
 * Наносит урон сущностям при столкновении.
 */
public class CactusBlock extends Block {

	public static final MapCodec<CactusBlock> CODEC = createCodec(CactusBlock::new);
	public static final IntProperty AGE = Properties.AGE_15;
	public static final int MAX_AGE = 15;
	private static final VoxelShape OUTLINE_SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);
	private static final VoxelShape COLLISION_SHAPE = Block.createColumnShape(14.0, 0.0, 15.0);
	private static final int TALL_THRESHOLD = 3;
	private static final int FLOWER_GROWTH_AGE = 8;
	private static final double FLOWER_CHANCE_WHEN_SHORT = 0.1;
	private static final double FLOWER_CHANCE_WHEN_TALL = 0.25;

	@Override
	public MapCodec<CactusBlock> getCodec() {
		return CODEC;
	}

	public CactusBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(AGE, 0));
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.canPlaceAt(world, pos)) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos above = pos.up();

		if (world.isAir(above) == false) {
			return;
		}

		int height = 1;
		int age = state.get(AGE);

		while (world.getBlockState(pos.down(height)).isOf(this)) {
			if (++height == TALL_THRESHOLD && age == MAX_AGE) {
				return;
			}
		}

		if (age == FLOWER_GROWTH_AGE && canPlaceAt(getDefaultState(), world, above)) {
			double flowerChance = height >= TALL_THRESHOLD ? FLOWER_CHANCE_WHEN_TALL : FLOWER_CHANCE_WHEN_SHORT;

			if (random.nextDouble() <= flowerChance) {
				world.setBlockState(above, Blocks.CACTUS_FLOWER.getDefaultState());
			}
		} else if (age == MAX_AGE && height < TALL_THRESHOLD) {
			world.setBlockState(above, getDefaultState());
			BlockState resetState = state.with(AGE, 0);
			world.setBlockState(pos, resetState, 260);
			world.updateNeighbor(resetState, above, this, null, false);
		}

		if (age < MAX_AGE) {
			world.setBlockState(pos, state.with(AGE, age + 1), 260);
		}
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION_SHAPE;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE_SHAPE;
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
		if (state.canPlaceAt(world, pos) == false) {
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
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockState neighbor = world.getBlockState(pos.offset(direction));

			if (neighbor.isSolid() || world.getFluidState(pos.offset(direction)).isIn(FluidTags.LAVA)) {
				return false;
			}
		}

		BlockState below = world.getBlockState(pos.down());
		return (below.isOf(Blocks.CACTUS) || below.isIn(BlockTags.SAND))
			&& world.getBlockState(pos.up()).isLiquid() == false;
	}

	@Override
	protected void onEntityCollision(
		BlockState state,
		World world,
		BlockPos pos,
		Entity entity,
		EntityCollisionHandler handler,
		boolean isAboveSurface
	) {
		entity.serverDamage(world.getDamageSources().cactus(), 1.0F);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
