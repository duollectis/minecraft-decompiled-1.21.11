package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

/**
 * Предмет кровати. Переопределяет метод размещения, чтобы принудительно
 * обновить состояние блока и уведомить соседей (флаги {@code NOTIFY_LISTENERS | FORCE_STATE}).
 */
public class BedItem extends BlockItem {

	/** Флаги установки блока: уведомить слушателей + принудительное состояние. */
	private static final int PLACE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	public BedItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	protected boolean place(ItemPlacementContext context, BlockState state) {
		return context.getWorld().setBlockState(context.getBlockPos(), state, PLACE_FLAGS);
	}
}
