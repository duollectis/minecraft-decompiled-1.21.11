package net.minecraft.entity.passive;

import java.util.function.IntFunction;
import net.minecraft.util.function.ValueLists;

public enum HorseMarking {
   NONE(0),
   WHITE(1),
   WHITE_FIELD(2),
   WHITE_DOTS(3),
   BLACK_DOTS(4);

   private static final IntFunction<HorseMarking> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
      HorseMarking::getIndex, values(), ValueLists.OutOfBoundsHandling.WRAP
   );
   private final int index;

   private HorseMarking(final int index) {
      this.index = index;
   }

   public int getIndex() {
      return this.index;
   }

   public static HorseMarking byIndex(int index) {
      return INDEX_MAPPER.apply(index);
   }
}
