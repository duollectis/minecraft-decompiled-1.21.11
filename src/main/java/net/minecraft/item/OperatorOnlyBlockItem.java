package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.jspecify.annotations.Nullable;

/**
 * Предмет-блок, который могут размещать только операторы уровня 2 и выше.
 * <p>Используется для командных блоков и других привилегированных блоков.</p>
 */
public class OperatorOnlyBlockItem extends BlockItem {

	public OperatorOnlyBlockItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
		PlayerEntity player = context.getPlayer();
		return player != null && !player.isCreativeLevelTwoOp() ? null : super.getPlacementState(context);
	}
}
