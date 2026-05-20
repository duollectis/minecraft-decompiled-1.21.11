package net.minecraft.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

public interface HeightLimitView {
   int getHeight();

   int getBottomY();

   default int getTopYInclusive() {
      return this.getBottomY() + this.getHeight() - 1;
   }

   default int countVerticalSections() {
      return this.getTopSectionCoord() - this.getBottomSectionCoord() + 1;
   }

   default int getBottomSectionCoord() {
      return ChunkSectionPos.getSectionCoord(this.getBottomY());
   }

   default int getTopSectionCoord() {
      return ChunkSectionPos.getSectionCoord(this.getTopYInclusive());
   }

   default boolean isInHeightLimit(int y) {
      return y >= this.getBottomY() && y <= this.getTopYInclusive();
   }

   default boolean isOutOfHeightLimit(BlockPos pos) {
      return this.isOutOfHeightLimit(pos.getY());
   }

   default boolean isOutOfHeightLimit(int y) {
      return y < this.getBottomY() || y > this.getTopYInclusive();
   }

   default int getSectionIndex(int y) {
      return this.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(y));
   }

   default int sectionCoordToIndex(int coord) {
      return coord - this.getBottomSectionCoord();
   }

   default int sectionIndexToCoord(int index) {
      return index + this.getBottomSectionCoord();
   }

   static HeightLimitView create(int bottomY, int height) {
      return new HeightLimitView() {
         @Override
         public int getHeight() {
            return height;
         }

         @Override
         public int getBottomY() {
            return bottomY;
         }
      };
   }
}
