package net.minecraft.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public enum EmptyBlockView implements BlockView {
   INSTANCE;

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
      return null;
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      return Blocks.AIR.getDefaultState();
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      return Fluids.EMPTY.getDefaultState();
   }

   @Override
   public int getBottomY() {
      return 0;
   }

   @Override
   public int getHeight() {
      return 0;
   }
}
