package net.minecraft.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public interface Fertilizable {
   boolean isFertilizable(WorldView world, BlockPos pos, BlockState state);

   boolean canGrow(World world, Random random, BlockPos pos, BlockState state);

   void grow(ServerWorld world, Random random, BlockPos pos, BlockState state);

   static boolean canSpread(WorldView world, BlockPos pos, BlockState state) {
      return findPosToSpreadTo(Direction.Type.HORIZONTAL.stream().toList(), world, pos, state).isPresent();
   }

   static Optional<BlockPos> findPosToSpreadTo(World world, BlockPos pos, BlockState state) {
      return findPosToSpreadTo(Direction.Type.HORIZONTAL.getShuffled(world.random), world, pos, state);
   }

   private static Optional<BlockPos> findPosToSpreadTo(List<Direction> directions, WorldView world, BlockPos pos, BlockState state) {
      for (Direction direction : directions) {
         BlockPos blockPos = pos.offset(direction);
         if (world.isAir(blockPos) && state.canPlaceAt(world, blockPos)) {
            return Optional.of(blockPos);
         }
      }

      return Optional.empty();
   }

   default BlockPos getFertilizeParticlePos(BlockPos pos) {
      return switch (this.getFertilizableType()) {
         case NEIGHBOR_SPREADER -> pos.up();
         case GROWER -> pos;
      };
   }

   default Fertilizable.FertilizableType getFertilizableType() {
      return Fertilizable.FertilizableType.GROWER;
   }

   public static enum FertilizableType {
      NEIGHBOR_SPREADER,
      GROWER;
   }
}
