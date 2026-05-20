package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public abstract class RodBlock extends FacingBlock {
   private static final Map<Direction.Axis, VoxelShape> SHAPES_BY_AXIS = VoxelShapes.createAxisShapeMap(Block.createCuboidShape(4.0, 4.0, 16.0));

   protected RodBlock(AbstractBlock.Settings settings) {
      super(settings);
   }

   @Override
   protected abstract MapCodec<? extends RodBlock> getCodec();

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPES_BY_AXIS.get(state.get(FACING).getAxis());
   }

   @Override
   protected BlockState rotate(BlockState state, BlockRotation rotation) {
      return state.with(FACING, rotation.rotate(state.get(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.with(FACING, mirror.apply(state.get(FACING)));
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }
}
