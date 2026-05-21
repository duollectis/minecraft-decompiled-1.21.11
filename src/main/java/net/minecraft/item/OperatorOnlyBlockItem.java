package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.jspecify.annotations.Nullable;

/**
 * {@code OperatorOnlyBlockItem}.
 */
public class OperatorOnlyBlockItem extends BlockItem {

	public OperatorOnlyBlockItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
		PlayerEntity playerEntity = context.getPlayer();
		return playerEntity != null && !playerEntity.isCreativeLevelTwoOp() ? null : super.getPlacementState(context);
	}
}
