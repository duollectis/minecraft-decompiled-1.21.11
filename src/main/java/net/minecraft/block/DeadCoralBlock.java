package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Мёртвый коралловый блок — декоративный блок, не требующий воды для существования.
 */
public class DeadCoralBlock extends AbstractCoralBlock {

	public static final MapCodec<DeadCoralBlock> CODEC = createCodec(DeadCoralBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 15.0);

	@Override
	public MapCodec<DeadCoralBlock> getCodec() {
		return CODEC;
	}

	public DeadCoralBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}
