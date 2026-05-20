package net.minecraft.client.gui.hud.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.TextureFilteringMode;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SimplePerformanceImpactorsDebugHudEntry implements DebugHudEntry {
   @Override
   public void render(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      GameOptions gameOptions = minecraftClient.options;
      lines.addLine(
         String.format(
            Locale.ROOT,
            "%s%s B: %d",
            gameOptions.getImprovedTransparency().getValue() ? "improved-transparency" : "",
            gameOptions.getCloudRenderMode().getValue() == CloudRenderMode.OFF
               ? ""
               : (gameOptions.getCloudRenderMode().getValue() == CloudRenderMode.FAST ? " fast-clouds" : " fancy-clouds"),
            gameOptions.getBiomeBlendRadius().getValue()
         )
      );
      TextureFilteringMode textureFilteringMode = gameOptions.getTextureFiltering().getValue();
      if (textureFilteringMode == TextureFilteringMode.ANISOTROPIC) {
         lines.addLine(String.format(Locale.ROOT, "Filtering: %s %dx", textureFilteringMode.getText().getString(), gameOptions.getEffectiveAnisotropy()));
      } else {
         lines.addLine(String.format(Locale.ROOT, "Filtering: %s", textureFilteringMode.getText().getString()));
      }
   }

   @Override
   public boolean canShow(boolean reducedDebugInfo) {
      return true;
   }
}
