package net.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface ServerWorldAccess extends WorldAccess {
   ServerWorld toServerWorld();

   LocalDifficulty getLocalDifficulty(BlockPos pos);

   default void spawnEntityAndPassengers(Entity entity) {
      entity.streamSelfAndPassengers().forEach(this::spawnEntity);
   }
}
