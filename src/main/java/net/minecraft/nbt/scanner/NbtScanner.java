package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

public interface NbtScanner {
   NbtScanner.Result visitEnd();

   NbtScanner.Result visitString(String value);

   NbtScanner.Result visitByte(byte value);

   NbtScanner.Result visitShort(short value);

   NbtScanner.Result visitInt(int value);

   NbtScanner.Result visitLong(long value);

   NbtScanner.Result visitFloat(float value);

   NbtScanner.Result visitDouble(double value);

   NbtScanner.Result visitByteArray(byte[] value);

   NbtScanner.Result visitIntArray(int[] value);

   NbtScanner.Result visitLongArray(long[] value);

   NbtScanner.Result visitListMeta(NbtType<?> entryType, int length);

   NbtScanner.NestedResult visitSubNbtType(NbtType<?> type);

   NbtScanner.NestedResult startSubNbt(NbtType<?> type, String key);

   NbtScanner.NestedResult startListItem(NbtType<?> type, int index);

   NbtScanner.Result endNested();

   NbtScanner.Result start(NbtType<?> rootType);

   public static enum NestedResult {
      ENTER,
      SKIP,
      BREAK,
      HALT;
   }

   public static enum Result {
      CONTINUE,
      BREAK,
      HALT;
   }
}
