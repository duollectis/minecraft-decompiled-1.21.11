package net.minecraft.nbt;

public class NbtTypes {
   private static final NbtType<?>[] VALUES = new NbtType[]{
      NbtEnd.TYPE,
      NbtByte.TYPE,
      NbtShort.TYPE,
      NbtInt.TYPE,
      NbtLong.TYPE,
      NbtFloat.TYPE,
      NbtDouble.TYPE,
      NbtByteArray.TYPE,
      NbtString.TYPE,
      NbtList.TYPE,
      NbtCompound.TYPE,
      NbtIntArray.TYPE,
      NbtLongArray.TYPE
   };

   public static NbtType<?> byId(int id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : NbtType.createInvalid(id);
   }
}
