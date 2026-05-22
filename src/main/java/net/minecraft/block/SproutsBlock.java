package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Блок ростков — небольшое подземное растение, которое может расти
 * на нилии, душевой почве и стандартных блоках для растений.
 */
public class SproutsBlock extends PlantBlock {

	public static final MapCodec<SproutsBlock> CODEC = createCodec(SproutsBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 3.0);

	@Override
	public MapCodec<SproutsBlock> getCodec() {
		return CODEC;
	}

	public SproutsBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isIn(BlockTags.NYLIUM) || floor.isOf(Blocks.SOUL_SOIL) || super.canPlantOnTop(floor, world, pos);
	}
}
