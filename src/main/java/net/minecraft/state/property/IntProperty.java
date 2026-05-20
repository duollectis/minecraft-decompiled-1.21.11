package net.minecraft.state.property;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class IntProperty extends Property<Integer> {
   private final IntImmutableList values;
   private final int min;
   private final int max;

   private IntProperty(String name, int min, int max) {
      super(name, Integer.class);
      if (min < 0) {
         throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
      } else if (max <= min) {
         throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
      } else {
         this.min = min;
         this.max = max;
         this.values = IntImmutableList.toList(IntStream.range(min, max + 1));
      }
   }

   @Override
   public List<Integer> getValues() {
      return this.values;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         return object instanceof IntProperty intProperty && super.equals(object) ? this.values.equals(intProperty.values) : false;
      }
   }

   @Override
   public int computeHashCode() {
      return 31 * super.computeHashCode() + this.values.hashCode();
   }

   public static IntProperty of(String name, int min, int max) {
      return new IntProperty(name, min, max);
   }

   @Override
   public Optional<Integer> parse(String name) {
      try {
         int i = Integer.parseInt(name);
         return i >= this.min && i <= this.max ? Optional.of(i) : Optional.empty();
      } catch (NumberFormatException var3) {
         return Optional.empty();
      }
   }

   public String name(Integer integer) {
      return integer.toString();
   }

   public int ordinal(Integer integer) {
      return integer <= this.max ? integer - this.min : -1;
   }
}
