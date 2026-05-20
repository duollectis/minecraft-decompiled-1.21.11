package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public abstract class HorizontalConnectingBlock extends Block implements Waterloggable {
   public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
   public static final BooleanProperty EAST = ConnectingBlock.EAST;
   public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
   public static final BooleanProperty WEST = ConnectingBlock.WEST;
   public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
   public static final Map<Direction, BooleanProperty> FACING_PROPERTIES = ConnectingBlock.FACING_PROPERTIES
      .entrySet()
      .stream()
      .filter(entry -> entry.getKey().getAxis().isHorizontal())
      .collect(Util.toMap());
   private final Function<BlockState, VoxelShape> collisionShapeFunction;
   private final Function<BlockState, VoxelShape> outlineShapeFunction;

   protected HorizontalConnectingBlock(
      float radius1, float radius2, float boundingHeight1, float boundingHeight2, float collisionHeight, AbstractBlock.Settings settings
   ) {
      super(settings);
      this.collisionShapeFunction = this.createShapeFunction(radius1, collisionHeight, boundingHeight1, 0.0F, collisionHeight);
      this.outlineShapeFunction = this.createShapeFunction(radius1, radius2, boundingHeight1, 0.0F, boundingHeight2);
   }

   @Override
   protected abstract MapCodec<? extends HorizontalConnectingBlock> getCodec();

   protected Function<BlockState, VoxelShape> createShapeFunction(float radius1, float radius2, float height1, float offset2, float height2) {
      VoxelShape voxelShape = Block.createColumnShape(radius1, 0.0, radius2);
      Map<Direction, VoxelShape> map = VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(height1, offset2, height2, 0.0, 8.0));
      return this.createShapeFunction(state -> {
         VoxelShape voxelShape2 = voxelShape;

         for (Entry<Direction, BooleanProperty> entry : FACING_PROPERTIES.entrySet()) {
            if (state.get(entry.getValue())) {
               voxelShape2 = VoxelShapes.union(voxelShape2, map.get(entry.getKey()));
            }
         }

         return voxelShape2;
      }, WATERLOGGED);
   }

   @Override
   protected boolean isTransparent(BlockState state) {
      return !state.get(WATERLOGGED);
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return this.outlineShapeFunction.apply(state);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return this.collisionShapeFunction.apply(state);
   }

   @Override
   protected FluidState getFluidState(BlockState state) {
      return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }

   @Override
   protected BlockState rotate(BlockState state, BlockRotation rotation) {
      switch (rotation) {
         case CLOCKWISE_180:
            return state.with(NORTH, state.get(SOUTH)).with(EAST, state.get(WEST)).with(SOUTH, state.get(NORTH)).with(WEST, state.get(EAST));
         case COUNTERCLOCKWISE_90:
            return state.with(NORTH, state.get(EAST)).with(EAST, state.get(SOUTH)).with(SOUTH, state.get(WEST)).with(WEST, state.get(NORTH));
         case CLOCKWISE_90:
            return state.with(NORTH, state.get(WEST)).with(EAST, state.get(NORTH)).with(SOUTH, state.get(EAST)).with(WEST, state.get(SOUTH));
         default:
            return state;
      }
   }

   @Override
   protected BlockState mirror(BlockState state, BlockMirror mirror) {
      switch (mirror) {
         case LEFT_RIGHT:
            return state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
         case FRONT_BACK:
            return state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
         default:
            return super.mirror(state, mirror);
      }
   }
}
