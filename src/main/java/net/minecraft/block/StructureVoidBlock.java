package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * {@code StructureVoidBlock}.
 */
public class StructureVoidBlock extends Block {

	public static final MapCodec<StructureVoidBlock> CODEC = createCodec(StructureVoidBlock::new);
	private static final VoxelShape SHAPE = Block.createCubeShape(6.0);

	@Override
	public MapCodec<StructureVoidBlock> getCodec() {
		return CODEC;
	}

	public StructureVoidBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 1.0F;
	}
}
