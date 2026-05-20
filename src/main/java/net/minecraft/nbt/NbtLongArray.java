package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.apache.commons.lang3.ArrayUtils;

public final class NbtLongArray implements AbstractNbtList {
   private static final int SIZE = 24;
   public static final NbtType<NbtLongArray> TYPE = new NbtType.OfVariableSize<NbtLongArray>() {
      public NbtLongArray read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         return new NbtLongArray(readLongArray(dataInput, nbtSizeTracker));
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         return visitor.visitLongArray(readLongArray(input, tracker));
      }

      private static long[] readLongArray(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(24L);
         int i = input.readInt();
         tracker.add(8L, i);
         long[] ls = new long[i];

         for (int j = 0; j < i; j++) {
            ls[j] = input.readLong();
         }

         return ls;
      }

      @Override
      public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
         input.skipBytes(input.readInt() * 8);
      }

      @Override
      public String getCrashReportName() {
         return "LONG[]";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_Long_Array";
      }
   };
   private long[] value;

   public NbtLongArray(long[] value) {
      this.value = value;
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeInt(this.value.length);

      for (long l : this.value) {
         output.writeLong(l);
      }
   }

   @Override
   public int getSizeInBytes() {
      return 24 + 8 * this.value.length;
   }

   @Override
   public byte getType() {
      return 12;
   }

   @Override
   public NbtType<NbtLongArray> getNbtType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringNbtWriter stringNbtWriter = new StringNbtWriter();
      stringNbtWriter.visitLongArray(this);
      return stringNbtWriter.getString();
   }

   public NbtLongArray copy() {
      long[] ls = new long[this.value.length];
      System.arraycopy(this.value, 0, ls, 0, this.value.length);
      return new NbtLongArray(ls);
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof NbtLongArray && Arrays.equals(this.value, ((NbtLongArray)o).value);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.value);
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitLongArray(this);
   }

   public long[] getLongArray() {
      return this.value;
   }

   @Override
   public int size() {
      return this.value.length;
   }

   public NbtLong method_10534(int i) {
      return NbtLong.of(this.value[i]);
   }

   @Override
   public boolean setElement(int index, NbtElement element) {
      if (element instanceof AbstractNbtNumber abstractNbtNumber) {
         this.value[index] = abstractNbtNumber.longValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addElement(int index, NbtElement element) {
      if (element instanceof AbstractNbtNumber abstractNbtNumber) {
         this.value = ArrayUtils.add(this.value, index, abstractNbtNumber.longValue());
         return true;
      } else {
         return false;
      }
   }

   public NbtLong method_10536(int i) {
      long l = this.value[i];
      this.value = ArrayUtils.remove(this.value, i);
      return NbtLong.of(l);
   }

   @Override
   public void clear() {
      this.value = new long[0];
   }

   @Override
   public Optional<long[]> asLongArray() {
      return Optional.of(this.value);
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      return visitor.visitLongArray(this.value);
   }
}
