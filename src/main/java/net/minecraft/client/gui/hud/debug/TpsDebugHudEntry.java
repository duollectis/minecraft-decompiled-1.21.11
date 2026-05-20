package net.minecraft.client.gui.hud.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.TickManager;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TpsDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      IntegratedServer integratedServer = minecraftClient.getServer();
      ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
      if (clientPlayNetworkHandler != null && world != null) {
         ClientConnection clientConnection = clientPlayNetworkHandler.getConnection();
         float f = clientConnection.getAveragePacketsSent();
         float g = clientConnection.getAveragePacketsReceived();
         TickManager tickManager = world.getTickManager();
         String string;
         if (tickManager.isStepping()) {
            string = " (frozen - stepping)";
         } else if (tickManager.isFrozen()) {
            string = " (frozen)";
         } else {
            string = "";
         }

         String string3;
         if (integratedServer != null) {
            ServerTickManager serverTickManager = integratedServer.getTickManager();
            boolean bl = serverTickManager.isSprinting();
            if (bl) {
               string = " (sprinting)";
            }

            String string2 = bl ? "-" : String.format(Locale.ROOT, "%.1f", tickManager.getMillisPerTick());
            string3 = String.format(
               Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", integratedServer.getAverageTickTime(), string2, string, f, g
            );
         } else {
            string3 = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", clientPlayNetworkHandler.getBrand(), string, f, g);
         }

         lines.addLine(string3);
      }
   }

   @Override
   public boolean canShow(boolean reducedDebugInfo) {
      return true;
   }
}
