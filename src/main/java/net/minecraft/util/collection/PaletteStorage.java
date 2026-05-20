package net.minecraft.util.collection;

import java.util.function.IntConsumer;

public interface PaletteStorage {
   int swap(int index, int value);

   void set(int index, int value);

   int get(int index);

   long[] getData();

   int getSize();

   int getElementBits();

   void forEach(IntConsumer action);

   void writePaletteIndices(int[] out);

   PaletteStorage copy();
}
