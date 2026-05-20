package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GpuUtilizationDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      String string = "GPU: "
         + (minecraftClient.getGpuUtilizationPercentage() > 100.0 ? Formatting.RED + "100%" : Math.round(minecraftClient.getGpuUtilizationPercentage()) + "%");
      lines.addLine(string);
   }

   @Override
   public boolean canShow(boolean reducedDebugInfo) {
      return true;
   }
}
