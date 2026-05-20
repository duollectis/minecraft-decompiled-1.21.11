package net.minecraft.nbt;

public sealed interface NbtPrimitive extends NbtElement permits AbstractNbtNumber, NbtString {
   @Override
   default NbtElement copy() {
      return this;
   }
}
