package net.minecraft.block;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Map;
import java.util.function.Function;

/**
 * Интерфейс сегментированных блоков (например, цветочные горшки с несколькими
 * секциями). Определяет логику добавления сегментов при повторном размещении
 * и построения формы столкновения по количеству активных сегментов.
 */
public interface Segmented {

	int SEGMENTS_PER_PLACEMENT = 1;

	int MAX_SEGMENTS = 4;

	IntProperty SEGMENT_AMOUNT = Properties.SEGMENT_AMOUNT;

	default Function<BlockState, VoxelShape> createShapeFunction(
			EnumProperty<Direction> directionProperty,
			IntProperty segmentAmountProperty
	) {
		Map<Direction, VoxelShape>
				map =
				VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidShape(
						0.0,
						0.0,
						0.0,
						8.0,
						this.getHeight(),
						8.0
				));
		return state -> {
			VoxelShape voxelShape = VoxelShapes.empty();
			Direction direction = state.get(directionProperty);
			int segmentCount = state.get(segmentAmountProperty);

			for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
				voxelShape = VoxelShapes.union(voxelShape, map.get(direction));
				direction = direction.rotateYCounterclockwise();
			}

			return voxelShape.asCuboid();
		};
	}

	default IntProperty getAmountProperty() {
		return SEGMENT_AMOUNT;
	}

	default double getHeight() {
		return 1.0;
	}

	default boolean shouldAddSegment(BlockState state, ItemPlacementContext context, IntProperty property) {
		return !context.shouldCancelInteraction() && context.getStack().isOf(state.getBlock().asItem())
				&& state.get(property) < MAX_SEGMENTS;
	}

	default BlockState getPlacementState(
			ItemPlacementContext context,
			Block block,
			IntProperty amountProperty,
			EnumProperty<Direction> directionProperty
	) {
		BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
		return blockState.isOf(block)
		       ? blockState.with(amountProperty, Math.min(MAX_SEGMENTS, blockState.get(amountProperty) + 1))
		       : block.getDefaultState().with(directionProperty, context.getHorizontalPlayerFacing().getOpposite());
	}
}
