package net.minecraft.world.event.listener;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;

public interface GameEventListener {
   PositionSource getPositionSource();

   int getRange();

   boolean listen(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, Vec3d emitterPos);

   default GameEventListener.TriggerOrder getTriggerOrder() {
      return GameEventListener.TriggerOrder.UNSPECIFIED;
   }

   public interface Holder<T extends GameEventListener> {
      T getEventListener();
   }

   public static enum TriggerOrder {
      UNSPECIFIED,
      BY_DISTANCE;
   }
}
