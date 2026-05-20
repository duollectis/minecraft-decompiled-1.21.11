package net.minecraft.world;

import java.util.function.Supplier;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public interface StructureWorldAccess extends ServerWorldAccess {
   long getSeed();

   default boolean isValidForSetBlock(BlockPos pos) {
      return true;
   }

   default void setCurrentlyGeneratingStructureName(@Nullable Supplier<String> structureName) {
   }
}
