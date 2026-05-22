package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.AmbientDesertBlockSounds;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Блок сухой растительности (мёртвый куст и аналоги).
 * Размещается только на блоках из тега {@code DRY_VEGETATION_MAY_PLACE_ON}.
 */
public class DryVegetationBlock extends PlantBlock {

	public static final MapCodec<DryVegetationBlock> CODEC = createCodec(DryVegetationBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 13.0);

	@Override
	public MapCodec<? extends DryVegetationBlock> getCodec() {
		return CODEC;
	}

	public DryVegetationBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isIn(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		AmbientDesertBlockSounds.tryPlayDeadBushSounds(world, pos, random);
	}
}
