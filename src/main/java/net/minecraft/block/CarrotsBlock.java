package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * {@code CarrotsBlock}.
 */
public class CarrotsBlock extends CropBlock {

	public static final MapCodec<CarrotsBlock> CODEC = createCodec(CarrotsBlock::new);
	private static final VoxelShape[]
			SHAPES_BY_AGE =
			Block.createShapeArray(7, age -> Block.createColumnShape(16.0, 0.0, 2 + age));

	@Override
	public MapCodec<CarrotsBlock> getCodec() {
		return CODEC;
	}

	public CarrotsBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected ItemConvertible getSeedsItem() {
		return Items.CARROT;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_AGE[this.getAge(state)];
	}
}
