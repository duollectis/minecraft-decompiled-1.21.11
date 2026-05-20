package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public interface ModifiableWorld {
   boolean setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags, int maxUpdateDepth);

   default boolean setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
      return this.setBlockState(pos, state, flags, 512);
   }

   boolean removeBlock(BlockPos pos, boolean move);

   default boolean breakBlock(BlockPos pos, boolean drop) {
      return this.breakBlock(pos, drop, null);
   }

   default boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity) {
      return this.breakBlock(pos, drop, breakingEntity, 512);
   }

   boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth);

   default boolean spawnEntity(Entity entity) {
      return false;
   }
}
