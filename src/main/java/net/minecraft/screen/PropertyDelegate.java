package net.minecraft.screen;

public interface PropertyDelegate {
   int get(int index);

   void set(int index, int value);

   int size();
}
