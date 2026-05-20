package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChunkRenderStatsDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      String string = MinecraftClient.getInstance().worldRenderer.getChunksDebugString();
      if (string != null) {
         lines.addLine(string);
      }
   }

   @Override
   public boolean canShow(boolean reducedDebugInfo) {
      return true;
   }
}
