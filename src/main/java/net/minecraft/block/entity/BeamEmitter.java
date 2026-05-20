package net.minecraft.block.entity;

import java.util.List;

public interface BeamEmitter {
   List<BeamEmitter.BeamSegment> getBeamSegments();

   public static class BeamSegment {
      private final int color;
      private int height;

      public BeamSegment(int color) {
         this.color = color;
         this.height = 1;
      }

      public void increaseHeight() {
         this.height++;
      }

      public int getColor() {
         return this.color;
      }

      public int getHeight() {
         return this.height;
      }
   }
}
