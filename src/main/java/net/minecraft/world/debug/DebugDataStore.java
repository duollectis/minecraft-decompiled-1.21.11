package net.minecraft.world.debug;

import java.util.function.BiConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface DebugDataStore {
   <T> void forEachChunkData(DebugSubscriptionType<T> type, BiConsumer<ChunkPos, T> action);

   <T> @Nullable T getChunkData(DebugSubscriptionType<T> type, ChunkPos chunkPos);

   <T> void forEachBlockData(DebugSubscriptionType<T> type, BiConsumer<BlockPos, T> action);

   <T> @Nullable T getBlockData(DebugSubscriptionType<T> type, BlockPos pos);

   <T> void forEachEntityData(DebugSubscriptionType<T> type, BiConsumer<Entity, T> action);

   <T> @Nullable T getEntityData(DebugSubscriptionType<T> type, Entity entity);

   <T> void forEachEvent(DebugSubscriptionType<T> type, DebugDataStore.EventConsumer<T> action);

   @FunctionalInterface
   public interface EventConsumer<T> {
      void accept(T value, int remainingTime, int expiry);
   }
}
