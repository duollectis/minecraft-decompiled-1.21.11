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

public final class NbtByteArray implements AbstractNbtList {
   private static final int SIZE = 24;
   public static final NbtType<NbtByteArray> TYPE = new NbtType.OfVariableSize<NbtByteArray>() {
      public NbtByteArray read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         return new NbtByteArray(readByteArray(dataInput, nbtSizeTracker));
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         return visitor.visitByteArray(readByteArray(input, tracker));
      }

      private static byte[] readByteArray(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(24L);
         int i = input.readInt();
         tracker.add(1L, i);
         byte[] bs = new byte[i];
         input.readFully(bs);
         return bs;
      }

      @Override
      public void skip(DataInput input, NbtSizeTracker tracker) throws IOException {
         input.skipBytes(input.readInt() * 1);
      }

      @Override
      public String getCrashReportName() {
         return "BYTE[]";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_Byte_Array";
      }
   };
   private byte[] value;

   public NbtByteArray(byte[] value) {
      this.value = value;
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeInt(this.value.length);
      output.write(this.value);
   }

   @Override
   public int getSizeInBytes() {
      return 24 + 1 * this.value.length;
   }

   @Override
   public byte getType() {
      return 7;
   }

   @Override
   public NbtType<NbtByteArray> getNbtType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringNbtWriter stringNbtWriter = new StringNbtWriter();
      stringNbtWriter.visitByteArray(this);
      return stringNbtWriter.getString();
   }

   @Override
   public NbtElement copy() {
      byte[] bs = new byte[this.value.length];
      System.arraycopy(this.value, 0, bs, 0, this.value.length);
      return new NbtByteArray(bs);
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof NbtByteArray && Arrays.equals(this.value, ((NbtByteArray)o).value);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.value);
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitByteArray(this);
   }

   public byte[] getByteArray() {
      return this.value;
   }

   @Override
   public int size() {
      return this.value.length;
   }

   public NbtByte method_10534(int i) {
      return NbtByte.of(this.value[i]);
   }

   @Override
   public boolean setElement(int index, NbtElement element) {
      if (element instanceof AbstractNbtNumber abstractNbtNumber) {
         this.value[index] = abstractNbtNumber.byteValue();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addElement(int index, NbtElement element) {
      if (element instanceof AbstractNbtNumber abstractNbtNumber) {
         this.value = ArrayUtils.add(this.value, index, abstractNbtNumber.byteValue());
         return true;
      } else {
         return false;
      }
   }

   public NbtByte method_10536(int i) {
      byte b = this.value[i];
      this.value = ArrayUtils.remove(this.value, i);
      return NbtByte.of(b);
   }

   @Override
   public void clear() {
      this.value = new byte[0];
   }

   @Override
   public Optional<byte[]> asByteArray() {
      return Optional.of(this.value);
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      return visitor.visitByteArray(this.value);
   }
}
