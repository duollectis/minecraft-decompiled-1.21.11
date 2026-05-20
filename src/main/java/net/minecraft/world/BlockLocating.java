package net.minecraft.world;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockLocating {
   public static BlockLocating.Rectangle getLargestRectangle(
      BlockPos center, Direction.Axis primaryAxis, int primaryMaxBlocks, Direction.Axis secondaryAxis, int secondaryMaxBlocks, Predicate<BlockPos> predicate
   ) {
      BlockPos.Mutable mutable = center.mutableCopy();
      Direction direction = Direction.get(Direction.AxisDirection.NEGATIVE, primaryAxis);
      Direction direction2 = direction.getOpposite();
      Direction direction3 = Direction.get(Direction.AxisDirection.NEGATIVE, secondaryAxis);
      Direction direction4 = direction3.getOpposite();
      int i = moveWhile(predicate, mutable.set(center), direction, primaryMaxBlocks);
      int j = moveWhile(predicate, mutable.set(center), direction2, primaryMaxBlocks);
      int k = i;
      BlockLocating.IntBounds[] intBoundss = new BlockLocating.IntBounds[i + 1 + j];
      intBoundss[i] = new BlockLocating.IntBounds(
         moveWhile(predicate, mutable.set(center), direction3, secondaryMaxBlocks), moveWhile(predicate, mutable.set(center), direction4, secondaryMaxBlocks)
      );
      int l = intBoundss[i].min;

      for (int m = 1; m <= i; m++) {
         BlockLocating.IntBounds intBounds = intBoundss[k - (m - 1)];
         intBoundss[k - m] = new BlockLocating.IntBounds(
            moveWhile(predicate, mutable.set(center).move(direction, m), direction3, intBounds.min),
            moveWhile(predicate, mutable.set(center).move(direction, m), direction4, intBounds.max)
         );
      }

      for (int m = 1; m <= j; m++) {
         BlockLocating.IntBounds intBounds = intBoundss[k + m - 1];
         intBoundss[k + m] = new BlockLocating.IntBounds(
            moveWhile(predicate, mutable.set(center).move(direction2, m), direction3, intBounds.min),
            moveWhile(predicate, mutable.set(center).move(direction2, m), direction4, intBounds.max)
         );
      }

      int m = 0;
      int n = 0;
      int o = 0;
      int p = 0;
      int[] is = new int[intBoundss.length];

      for (int q = l; q >= 0; q--) {
         for (int r = 0; r < intBoundss.length; r++) {
            BlockLocating.IntBounds intBounds2 = intBoundss[r];
            int s = l - intBounds2.min;
            int t = l + intBounds2.max;
            is[r] = q >= s && q <= t ? t + 1 - q : 0;
         }

         Pair<BlockLocating.IntBounds, Integer> pair = findLargestRectangle(is);
         BlockLocating.IntBounds intBounds2 = (BlockLocating.IntBounds)pair.getFirst();
         int s = 1 + intBounds2.max - intBounds2.min;
         int t = (Integer)pair.getSecond();
         if (s * t > o * p) {
            m = intBounds2.min;
            n = q;
            o = s;
            p = t;
         }
      }

      return new BlockLocating.Rectangle(center.offset(primaryAxis, m - k).offset(secondaryAxis, n - l), o, p);
   }

   private static int moveWhile(Predicate<BlockPos> predicate, BlockPos.Mutable pos, Direction direction, int max) {
      int i = 0;

      while (i < max && predicate.test(pos.move(direction))) {
         i++;
      }

      return i;
   }

   @VisibleForTesting
   static Pair<BlockLocating.IntBounds, Integer> findLargestRectangle(int[] heights) {
      int i = 0;
      int j = 0;
      int k = 0;
      IntStack intStack = new IntArrayList();
      intStack.push(0);

      for (int l = 1; l <= heights.length; l++) {
         int m = l == heights.length ? 0 : heights[l];

         while (!intStack.isEmpty()) {
            int n = heights[intStack.topInt()];
            if (m >= n) {
               intStack.push(l);
               break;
            }

            intStack.popInt();
            int o = intStack.isEmpty() ? 0 : intStack.topInt() + 1;
            if (n * (l - o) > k * (j - i)) {
               j = l;
               i = o;
               k = n;
            }
         }

         if (intStack.isEmpty()) {
            intStack.push(l);
         }
      }

      return new Pair(new BlockLocating.IntBounds(i, j - 1), k);
   }

   public static Optional<BlockPos> findColumnEnd(BlockView world, BlockPos pos, Block intermediateBlock, Direction direction, Block endBlock) {
      BlockPos.Mutable mutable = pos.mutableCopy();

      BlockState blockState;
      do {
         mutable.move(direction);
         blockState = world.getBlockState(mutable);
      } while (blockState.isOf(intermediateBlock));

      return blockState.isOf(endBlock) ? Optional.of(mutable) : Optional.empty();
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(int min, int max) {
         this.min = min;
         this.max = max;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }

   public static class Rectangle {
      public final BlockPos lowerLeft;
      public final int width;
      public final int height;

      public Rectangle(BlockPos lowerLeft, int width, int height) {
         this.lowerLeft = lowerLeft;
         this.width = width;
         this.height = height;
      }
   }
}
