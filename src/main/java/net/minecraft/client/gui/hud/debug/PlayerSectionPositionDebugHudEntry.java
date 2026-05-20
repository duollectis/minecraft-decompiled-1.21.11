package net.minecraft.client.gui.hud.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PlayerSectionPositionDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Entity entity = minecraftClient.getCameraEntity();
      if (entity != null) {
         BlockPos blockPos = minecraftClient.getCameraEntity().getBlockPos();
         lines.addLineToSection(
            PlayerPositionDebugHudEntry.SECTION_ID,
            String.format(Locale.ROOT, "Section-relative: %02d %02d %02d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15)
         );
      }
   }

   @Override
   public boolean canShow(boolean reducedDebugInfo) {
      return true;
   }
}
