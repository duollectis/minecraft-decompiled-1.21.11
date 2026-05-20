package net.minecraft.util.profiler.log;

import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugSubscriptionTypes;

public enum DebugSampleType {
   TICK_TIME(DebugSubscriptionTypes.DEDICATED_SERVER_TICK_TIME);

   private final DebugSubscriptionType<?> subscriptionType;

   private DebugSampleType(final DebugSubscriptionType<?> subscriptionType) {
      this.subscriptionType = subscriptionType;
   }

   public DebugSubscriptionType<?> getSubscriptionType() {
      return this.subscriptionType;
   }
}
