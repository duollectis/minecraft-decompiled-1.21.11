package net.minecraft.util.math.random;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface RandomSplitter {
   default Random split(BlockPos pos) {
      return this.split(pos.getX(), pos.getY(), pos.getZ());
   }

   default Random split(Identifier seed) {
      return this.split(seed.toString());
   }

   Random split(String seed);

   Random split(long seed);

   Random split(int x, int y, int z);

   @VisibleForTesting
   void addDebugInfo(StringBuilder info);
}
