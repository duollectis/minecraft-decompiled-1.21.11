package net.minecraft.client.gui.hud.debug;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LightLevelsDebugHudEntry implements DebugHudEntry {
   public static final Identifier SECTION_ID = Identifier.ofVanilla("light");

   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Entity entity = minecraftClient.getCameraEntity();
      if (entity != null && minecraftClient.world != null) {
         BlockPos blockPos = entity.getBlockPos();
         int i = minecraftClient.world.getChunkManager().getLightingProvider().getLight(blockPos, 0);
         int j = minecraftClient.world.getLightLevel(LightType.SKY, blockPos);
         int k = minecraftClient.world.getLightLevel(LightType.BLOCK, blockPos);
         String string = "Client Light: " + i + " (" + j + " sky, " + k + " block)";
         if (SharedConstants.SHOW_SERVER_DEBUG_VALUES) {
            String string2;
            if (chunk != null) {
               LightingProvider lightingProvider = chunk.getWorld().getLightingProvider();
               string2 = "Server Light: ("
                  + lightingProvider.get(LightType.SKY).getLightLevel(blockPos)
                  + " sky, "
                  + lightingProvider.get(LightType.BLOCK).getLightLevel(blockPos)
                  + " block)";
            } else {
               string2 = "Server Light: (?? sky, ?? block)";
            }

            lines.addLinesToSection(SECTION_ID, List.of(string, string2));
         } else {
            lines.addLineToSection(SECTION_ID, string);
         }
      }
   }
}
