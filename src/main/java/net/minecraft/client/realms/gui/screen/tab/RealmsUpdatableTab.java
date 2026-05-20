package net.minecraft.client.realms.gui.screen.tab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.dto.RealmsServer;

@Environment(EnvType.CLIENT)
public interface RealmsUpdatableTab {
   void update(RealmsServer server);

   default void onLoaded(RealmsServer server) {
   }

   default void onUnloaded(RealmsServer server) {
   }
}
