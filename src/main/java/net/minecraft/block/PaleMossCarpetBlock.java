package net.minecraft.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

public class PaleMossCarpetBlock extends Block implements Fertilizable {
   public static final MapCodec<PaleMossCarpetBlock> CODEC = createCodec(PaleMossCarpetBlock::new);
   public static final BooleanProperty BOTTOM = Properties.BOTTOM;
   public static final EnumProperty<WallShape> NORTH = Properties.NORTH_WALL_SHAPE;
   public static final EnumProperty<WallShape> EAST = Properties.EAST_WALL_SHAPE;
   public static final EnumProperty<WallShape> SOUTH = Properties.SOUTH_WALL_SHAPE;
   public static final EnumProperty<WallShape> WEST = Properties.WEST_WALL_SHAPE;
   public static final Map<Direction, EnumProperty<WallShape>> WALL_SHAPE_PROPERTIES_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   private final Function<BlockState, VoxelShape> shapeFunction;

   @Override
   public MapCodec<PaleMossCarpetBlock> getCodec() {
      return CODEC;
   }

   public PaleMossCarpetBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState(
         this.stateManager
            .getDefaultState()
            .with(BOTTOM, true)
            .with(NORTH, WallShape.NONE)
            .with(EAST, WallShape.NONE)
            .with(SOUTH, WallShape.NONE)
            .with(WEST, WallShape.NONE)
      );
      this.shapeFunction = this.createShapeFunction();
   }

   public Function<BlockState, VoxelShape> createShapeFunction() {
      Map<Direction, VoxelShape> map = VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(16.0, 0.0, 10.0, 0.0, 1.0));
      Map<Direction, VoxelShape> map2 = VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 0.0, 1.0));
      return this.createShapeFunction(state -> {
         VoxelShape voxelShape = state.get(BOTTOM) ? map2.get(Direction.DOWN) : VoxelShapes.empty();

         for (Entry<Direction, EnumProperty<WallShape>> entry : WALL_SHAPE_PROPERTIES_BY_DIRECTION.entrySet()) {
            switch ((WallShape)state.get(entry.getValue())) {
               case NONE:
               default:
                  break;
               case LOW:
                  voxelShape = VoxelShapes.union(voxelShape, map.get(entry.getKey()));
                  break;
               case TALL:
                  voxelShape = VoxelShapes.union(voxelShape, map2.get(entry.getKey()));
            }
         }

         return voxelShape.isEmpty() ? VoxelShapes.fullCube() : voxelShape;
      });
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return this.shapeFunction.apply(state);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return state.get(BOTTOM) ? this.shapeFunction.apply(this.getDefaultState()) : VoxelShapes.empty();
   }

   @Override
   protected boolean isTransparent(BlockState state) {
      return true;
   }

   @Override
   protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos.down());
      return state.get(BOTTOM) ? !blockState.isAir() : blockState.isOf(this) && blockState.get(BOTTOM);
   }

   private static boolean hasAnyShape(BlockState state) {
      if (state.get(BOTTOM)) {
         return true;
      } else {
         for (EnumProperty<WallShape> enumProperty : WALL_SHAPE_PROPERTIES_BY_DIRECTION.values()) {
            if (state.get(enumProperty) != WallShape.NONE) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean canGrowOnFace(BlockView world, BlockPos pos, Direction direction) {
      return direction == Direction.UP ? false : MultifaceBlock.canGrowOn(world, pos, direction);
   }

   private static BlockState updateState(BlockState state, BlockView world, BlockPos pos, boolean bl) {
      BlockState blockState = null;
      BlockState blockState2 = null;
      bl |= state.get(BOTTOM);

      for (Direction direction : Direction.Type.HORIZONTAL) {
         EnumProperty<WallShape> enumProperty = getWallShape(direction);
         WallShape wallShape = canGrowOnFace(world, pos, direction) ? (bl ? WallShape.LOW : state.get(enumProperty)) : WallShape.NONE;
         if (wallShape == WallShape.LOW) {
            if (blockState == null) {
               blockState = world.getBlockState(pos.up());
            }

            if (blockState.isOf(Blocks.PALE_MOSS_CARPET) && blockState.get(enumProperty) != WallShape.NONE && !blockState.get(BOTTOM)) {
               wallShape = WallShape.TALL;
            }

            if (!state.get(BOTTOM)) {
               if (blockState2 == null) {
                  blockState2 = world.getBlockState(pos.down());
               }

               if (blockState2.isOf(Blocks.PALE_MOSS_CARPET) && blockState2.get(enumProperty) == WallShape.NONE) {
                  wallShape = WallShape.NONE;
               }
            }
         }

         state = state.with(enumProperty, wallShape);
      }

      return state;
   }

   @Override
   public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
      return updateState(this.getDefaultState(), ctx.getWorld(), ctx.getBlockPos(), true);
   }

   public static void placeAt(WorldAccess world, BlockPos pos, Random random, @Block.SetBlockStateFlag int flags) {
      BlockState blockState = Blocks.PALE_MOSS_CARPET.getDefaultState();
      BlockState blockState2 = updateState(blockState, world, pos, true);
      world.setBlockState(pos, blockState2, flags);
      BlockState blockState3 = createUpperState(world, pos, random::nextBoolean);
      if (!blockState3.isAir()) {
         world.setBlockState(pos.up(), blockState3, flags);
         BlockState blockState4 = updateState(blockState2, world, pos, true);
         world.setBlockState(pos, blockState4, flags);
      }
   }

   @Override
   public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
      if (!world.isClient()) {
         Random random = world.getRandom();
         BlockState blockState = createUpperState(world, pos, random::nextBoolean);
         if (!blockState.isAir()) {
            world.setBlockState(pos.up(), blockState, 3);
         }
      }
   }

   private static BlockState createUpperState(BlockView world, BlockPos pos, BooleanSupplier booleanSupplier) {
      BlockPos blockPos = pos.up();
      BlockState blockState = world.getBlockState(blockPos);
      boolean bl = blockState.isOf(Blocks.PALE_MOSS_CARPET);
      if ((!bl || !blockState.get(BOTTOM)) && (bl || blockState.isReplaceable())) {
         BlockState blockState2 = Blocks.PALE_MOSS_CARPET.getDefaultState().with(BOTTOM, false);
         BlockState blockState3 = updateState(blockState2, world, pos.up(), true);

         for (Direction direction : Direction.Type.HORIZONTAL) {
            EnumProperty<WallShape> enumProperty = getWallShape(direction);
            if (blockState3.get(enumProperty) != WallShape.NONE && !booleanSupplier.getAsBoolean()) {
               blockState3 = blockState3.with(enumProperty, WallShape.NONE);
            }
         }

         return hasAnyShape(blockState3) && blockState3 != blockState ? blockState3 : Blocks.AIR.getDefaultState();
      } else {
         return Blocks.AIR.getDefaultState();
      }
   }

   @Override
   protected BlockState getStateForNeighborUpdate(
      BlockState state,
      WorldView world,
      ScheduledTickView tickView,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      Random random
   ) {
      if (!state.canPlaceAt(world, pos)) {
         return Blocks.AIR.getDefaultState();
      } else {
         BlockState blockState = updateState(state, world, pos, false);
         return !hasAnyShape(blockState) ? Blocks.AIR.getDefaultState() : blockState;
      }
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(BOTTOM, NORTH, EAST, SOUTH, WEST);
   }

   @Override
   protected BlockState rotate(BlockState state, BlockRotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_180 -> (BlockState)state.with(NORTH, state.get(SOUTH))
            .with(EAST, state.get(WEST))
            .with(SOUTH, state.get(NORTH))
            .with(WEST, state.get(EAST));
         case COUNTERCLOCKWISE_90 -> (BlockState)state.with(NORTH, state.get(EAST))
            .with(EAST, state.get(SOUTH))
            .with(SOUTH, state.get(WEST))
            .with(WEST, state.get(NORTH));
         case CLOCKWISE_90 -> (BlockState)state.with(NORTH, state.get(WEST))
            .with(EAST, state.get(NORTH))
            .with(SOUTH, state.get(EAST))
            .with(WEST, state.get(SOUTH));
         default -> state;
      };
   }

   @Override
   protected BlockState mirror(BlockState state, BlockMirror mirror) {
      return switch (mirror) {
         case LEFT_RIGHT -> (BlockState)state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
         case FRONT_BACK -> (BlockState)state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
         default -> super.mirror(state, mirror);
      };
   }

   public static @Nullable EnumProperty<WallShape> getWallShape(Direction face) {
      return WALL_SHAPE_PROPERTIES_BY_DIRECTION.get(face);
   }

   @Override
   public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
      return state.get(BOTTOM) && !createUpperState(world, pos, () -> true).isAir();
   }

   @Override
   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      BlockState blockState = createUpperState(world, pos, () -> true);
      if (!blockState.isAir()) {
         world.setBlockState(pos.up(), blockState, 3);
      }
   }
}
