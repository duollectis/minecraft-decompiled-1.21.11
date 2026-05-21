package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * {@code ShortPlantBlock}.
 */
public class ShortPlantBlock extends PlantBlock implements Fertilizable {

	public static final MapCodec<ShortPlantBlock> CODEC = createCodec(ShortPlantBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 13.0);

	@Override
	public MapCodec<ShortPlantBlock> getCodec() {
		return CODEC;
	}

	public ShortPlantBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return getLargeVariant(state).getDefaultState().canPlaceAt(world, pos) && world.isAir(pos.up());
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		TallPlantBlock.placeAt(world, getLargeVariant(state).getDefaultState(), pos, 2);
	}

	private static TallPlantBlock getLargeVariant(BlockState state) {
		return (TallPlantBlock) (state.isOf(Blocks.FERN) ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
	}
}
