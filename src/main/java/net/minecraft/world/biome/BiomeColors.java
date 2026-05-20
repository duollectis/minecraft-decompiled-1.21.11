package net.minecraft.world.biome;

public interface BiomeColors {
   static int getColor(double temperature, double downfall, int[] colormap, int fallback) {
      downfall *= temperature;
      int i = (int)((1.0 - temperature) * 255.0);
      int j = (int)((1.0 - downfall) * 255.0);
      int k = j << 8 | i;
      return k >= colormap.length ? fallback : colormap[k];
   }
}
