package net.minecraft.client.render.state;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.render.WeatherRendering;

@Environment(EnvType.CLIENT)
public class WeatherRenderState implements FabricRenderState {
   public final List<WeatherRendering.Piece> rainPieces = new ArrayList<>();
   public final List<WeatherRendering.Piece> snowPieces = new ArrayList<>();
   public float intensity;
   public int radius;

   public void clear() {
      this.rainPieces.clear();
      this.snowPieces.clear();
      this.intensity = 0.0F;
      this.radius = 0;
   }
}
