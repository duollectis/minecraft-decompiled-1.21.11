package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
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
 * Росток бамбука — начальная стадия роста до полноценного {@link BambooBlock}.
 * Превращается в первый сегмент бамбука с маленькими листьями при росте.
 * Если сверху появляется бамбук, немедленно заменяется на него.
 */
public class BambooShootBlock extends Block implements Fertilizable {

	public static final MapCodec<BambooShootBlock> CODEC = createCodec(BambooShootBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(8.0, 0.0, 12.0);
	private static final int GROWTH_RANDOM_BOUND = 3;
	private static final int MIN_GROWTH_LIGHT = 9;

	@Override
	public MapCodec<BambooShootBlock> getCodec() {
		return CODEC;
	}

	public BambooShootBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE.offset(state.getModelOffset(pos));
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos above = pos.up();

		if (random.nextInt(GROWTH_RANDOM_BOUND) == 0
			&& world.isAir(above)
			&& world.getBaseLightLevel(above, 0) >= MIN_GROWTH_LIGHT
		) {
			grow(world, pos);
		}
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isIn(BlockTags.BAMBOO_PLANTABLE_ON);
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
			return Blocks.AIR.getDefaultState();
		}

		return direction == Direction.UP && neighborState.isOf(Blocks.BAMBOO)
			? Blocks.BAMBOO.getDefaultState()
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
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Items.BAMBOO);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return world.getBlockState(pos.up()).isAir();
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		grow(world, pos);
	}

	protected void grow(World world, BlockPos pos) {
		world.setBlockState(pos.up(), Blocks.BAMBOO.getDefaultState().with(BambooBlock.LEAVES, BambooLeaves.SMALL), 3);
	}
}
