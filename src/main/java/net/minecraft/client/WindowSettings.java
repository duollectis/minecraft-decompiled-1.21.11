package net.minecraft.client;

import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record WindowSettings(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean fullscreen) {
   public WindowSettings withDimensions(int width, int height) {
      return new WindowSettings(width, height, this.fullscreenWidth, this.fullscreenHeight, this.fullscreen);
   }

   public WindowSettings withFullscreen(boolean fullscreen) {
      return new WindowSettings(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, fullscreen);
   }
}
