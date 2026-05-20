package net.minecraft.world.debug;

import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

public interface DebugTrackable {
   void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker);

   public interface DebugDataSupplier<T> {
      @Nullable T get();
   }

   public interface Tracker {
      <T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier);
   }
}
