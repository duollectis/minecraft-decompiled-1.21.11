package net.minecraft.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

public interface SpawnLocation {
   boolean isSpawnPositionOk(WorldView world, BlockPos pos, @Nullable EntityType<?> entityType);

   default BlockPos adjustPosition(WorldView world, BlockPos pos) {
      return pos;
   }
}
