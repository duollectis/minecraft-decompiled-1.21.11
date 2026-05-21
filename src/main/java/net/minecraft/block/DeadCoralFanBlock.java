package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * {@code DeadCoralFanBlock}.
 */
public class DeadCoralFanBlock extends AbstractCoralBlock {

	public static final MapCodec<DeadCoralFanBlock> CODEC = createCodec(DeadCoralFanBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 4.0);

	@Override
	public MapCodec<? extends DeadCoralFanBlock> getCodec() {
		return CODEC;
	}

	public DeadCoralFanBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}
