package net.minecraft.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;

public interface BlockRenderView extends BlockView {
   float getBrightness(Direction direction, boolean shaded);

   LightingProvider getLightingProvider();

   int getColor(BlockPos pos, ColorResolver colorResolver);

   default int getLightLevel(LightType type, BlockPos pos) {
      return this.getLightingProvider().get(type).getLightLevel(pos);
   }

   default int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
      return this.getLightingProvider().getLight(pos, ambientDarkness);
   }

   default boolean isSkyVisible(BlockPos pos) {
      return this.getLightLevel(LightType.SKY, pos) >= 15;
   }
}
