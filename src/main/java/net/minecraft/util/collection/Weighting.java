package net.minecraft.util.collection;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

public class Weighting {
   private Weighting() {
   }

   public static <T> int getWeightSum(List<T> pool, ToIntFunction<T> weightGetter) {
      long l = 0L;

      for (T object : pool) {
         l += weightGetter.applyAsInt(object);
      }

      if (l > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)l;
      }
   }

   public static <T> Optional<T> getRandom(Random random, List<T> pool, int totalWeight, ToIntFunction<T> weightGetter) {
      if (totalWeight < 0) {
         throw (IllegalArgumentException)Util.getFatalOrPause(new IllegalArgumentException("Negative total weight in getRandomItem"));
      } else if (totalWeight == 0) {
         return Optional.empty();
      } else {
         int i = random.nextInt(totalWeight);
         return getAt(pool, i, weightGetter);
      }
   }

   public static <T> Optional<T> getAt(List<T> pool, int totalWeight, ToIntFunction<T> weightGetter) {
      for (T object : pool) {
         totalWeight -= weightGetter.applyAsInt(object);
         if (totalWeight < 0) {
            return Optional.of(object);
         }
      }

      return Optional.empty();
   }

   public static <T> Optional<T> getRandom(Random random, List<T> pool, ToIntFunction<T> weightGetter) {
      return getRandom(random, pool, getWeightSum(pool, weightGetter), weightGetter);
   }
}
