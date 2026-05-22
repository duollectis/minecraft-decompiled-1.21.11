package net.minecraft.item;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Предмет «Табличка». После размещения автоматически открывает экран редактирования
 * текста для игрока, если блок не был заменён (например, при размещении поверх другого блока).
 */
public class SignItem extends VerticallyAttachableBlockItem {

	public SignItem(Block standingBlock, Block wallBlock, Item.Settings settings) {
		super(standingBlock, wallBlock, Direction.DOWN, settings);
	}

	public SignItem(
		Item.Settings settings,
		Block standingBlock,
		Block wallBlock,
		Direction verticalAttachmentDirection
	) {
		super(standingBlock, wallBlock, verticalAttachmentDirection, settings);
	}

	@Override
	protected boolean postPlacement(
		BlockPos pos,
		World world,
		@Nullable PlayerEntity player,
		ItemStack stack,
		BlockState state
	) {
		boolean replaced = super.postPlacement(pos, world, player, stack, state);

		if (!world.isClient()
			&& !replaced
			&& player != null
			&& world.getBlockEntity(pos) instanceof SignBlockEntity sign
			&& world.getBlockState(pos).getBlock() instanceof AbstractSignBlock signBlock
		) {
			signBlock.openEditScreen(player, sign, true);
		}

		return replaced;
	}
}
