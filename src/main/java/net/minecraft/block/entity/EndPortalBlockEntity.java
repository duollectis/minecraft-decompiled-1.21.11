package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * {@code EndPortalBlockEntity}.
 */
public class EndPortalBlockEntity extends BlockEntity {

	protected EndPortalBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	public EndPortalBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.END_PORTAL, pos, state);
	}

	/**
	 * Определяет, следует ли draw side.
	 *
	 * @param direction direction
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDrawSide(Direction direction) {
		return direction.getAxis() == Direction.Axis.Y;
	}
}
