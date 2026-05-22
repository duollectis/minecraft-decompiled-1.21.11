package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Предмет для двухблочных структур (например, двойные цветы, двери).
 * <p>При размещении дополнительно устанавливает верхнюю половину блока,
 * заменяя воду воздухом если верхняя позиция находится в воде.</p>
 */
public class TallBlockItem extends BlockItem {

	public TallBlockItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	protected boolean place(ItemPlacementContext context, BlockState state) {
		World world = context.getWorld();
		BlockPos upperPos = context.getBlockPos().up();
		BlockState upperState = world.isWater(upperPos)
		                        ? Blocks.WATER.getDefaultState()
		                        : Blocks.AIR.getDefaultState();
		world.setBlockState(upperPos, upperState, 27);
		return super.place(context, state);
	}
}
