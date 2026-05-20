package net.minecraft.block.entity;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public interface StructureBoxRendering {
   StructureBoxRendering.RenderMode getRenderMode();

   StructureBoxRendering.StructureBox getStructureBox();

   public static enum RenderMode {
      NONE,
      BOX,
      BOX_AND_INVISIBLE_BLOCKS;
   }

   public record StructureBox(BlockPos localPos, Vec3i size) {
      public static StructureBoxRendering.StructureBox create(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
         int i = Math.min(minX, maxX);
         int j = Math.min(minY, maxY);
         int k = Math.min(minZ, maxZ);
         return new StructureBoxRendering.StructureBox(
            new BlockPos(i, j, k), new Vec3i(Math.max(minX, maxX) - i, Math.max(minY, maxY) - j, Math.max(minZ, maxZ) - k)
         );
      }
   }
}
