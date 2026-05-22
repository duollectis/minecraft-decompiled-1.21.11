package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Блок азалии — декоративный куст, способный превратиться в дерево азалии
 * при использовании костяной муки. Растёт только на земле или глине,
 * не пропускает сквозь себя мобов при поиске пути.
 */
public class AzaleaBlock extends PlantBlock implements Fertilizable {

	public static final MapCodec<AzaleaBlock> CODEC = createCodec(AzaleaBlock::new);
	/** Вероятность успешного роста при применении костяной муки. */
	private static final float GROWTH_CHANCE = 0.45f;
	private static final VoxelShape SHAPE = VoxelShapes.union(
		Block.createColumnShape(16.0, 8.0, 16.0),
		Block.createColumnShape(4.0, 0.0, 8.0)
	);

	@Override
	public MapCodec<AzaleaBlock> getCodec() {
		return CODEC;
	}

	public AzaleaBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOf(Blocks.CLAY) || super.canPlantOnTop(floor, world, pos);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return world.getFluidState(pos.up()).isEmpty();
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return world.random.nextFloat() < GROWTH_CHANCE;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		SaplingGenerator.AZALEA.generate(world, world.getChunkManager().getChunkGenerator(), pos, state, random);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
