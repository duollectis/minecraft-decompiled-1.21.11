package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Предмет-блок, который может размещаться как на полу/потолке (вертикально), так и на стене.
 * Примеры: знаки, баннеры, головы игроков.
 */
public class VerticallyAttachableBlockItem extends BlockItem {

	protected final Block wallBlock;
	private final Direction verticalAttachmentDirection;

	public VerticallyAttachableBlockItem(
			Block standingBlock,
			Block wallBlock,
			Direction verticalAttachmentDirection,
			Item.Settings settings
	) {
		super(standingBlock, settings);
		this.wallBlock = wallBlock;
		this.verticalAttachmentDirection = verticalAttachmentDirection;
	}

	protected boolean canPlaceAt(WorldView world, BlockState state, BlockPos pos) {
		return state.canPlaceAt(world, pos);
	}

	@Override
	protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
		BlockState wallState = wallBlock.getPlacementState(context);
		BlockState candidate = null;
		WorldView worldView = context.getWorld();
		BlockPos blockPos = context.getBlockPos();

		for (Direction direction : context.getPlacementDirections()) {
			if (direction == verticalAttachmentDirection.getOpposite()) {
				continue;
			}

			BlockState stateForDirection = direction == verticalAttachmentDirection
					? getBlock().getPlacementState(context)
					: wallState;

			if (stateForDirection != null && canPlaceAt(worldView, stateForDirection, blockPos)) {
				candidate = stateForDirection;
				break;
			}
		}

		return candidate != null && worldView.canPlace(candidate, blockPos, ShapeContext.absent())
				? candidate
				: null;
	}

	@Override
	public void appendBlocks(Map<Block, Item> map, Item item) {
		super.appendBlocks(map, item);
		map.put(this.wallBlock, item);
	}
}
