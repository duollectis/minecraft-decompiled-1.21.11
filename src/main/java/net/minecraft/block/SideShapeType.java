package net.minecraft.block;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * {@code SideShapeType}.
 */
public enum SideShapeType {
	FULL {
		@Override
		public boolean matches(BlockState state, BlockView world, BlockPos pos, Direction direction) {
			return Block.isFaceFullSquare(state.getSidesShape(world, pos), direction);
		}
	},
	CENTER {
		private final VoxelShape squareCuboid = Block.createColumnShape(2.0, 0.0, 10.0);

		@Override
		public boolean matches(BlockState state, BlockView world, BlockPos pos, Direction direction) {
			return !VoxelShapes.matchesAnywhere(
					state.getSidesShape(world, pos).getFace(direction),
					this.squareCuboid,
					BooleanBiFunction.ONLY_SECOND
			);
		}
	},
	RIGID {
		private final VoxelShape hollowSquareCuboid = VoxelShapes.combineAndSimplify(
				VoxelShapes.fullCube(), Block.createColumnShape(12.0, 0.0, 16.0), BooleanBiFunction.ONLY_FIRST
		);

		@Override
		public boolean matches(BlockState state, BlockView world, BlockPos pos, Direction direction) {
			return !VoxelShapes.matchesAnywhere(
					state.getSidesShape(world, pos).getFace(direction),
					this.hollowSquareCuboid,
					BooleanBiFunction.ONLY_SECOND
			);
		}
	};

	/**
	 * Matches.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 * @param direction direction
	 *
	 * @return boolean — результат операции
	 */
	public abstract boolean matches(BlockState state, BlockView world, BlockPos pos, Direction direction);
}
