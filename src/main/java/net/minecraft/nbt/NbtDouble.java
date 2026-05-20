package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.math.MathHelper;

public record NbtDouble(double value) implements AbstractNbtNumber {
   private static final int SIZE = 16;
   public static final NbtDouble ZERO = new NbtDouble(0.0);
   public static final NbtType<NbtDouble> TYPE = new NbtType.OfFixedSize<NbtDouble>() {
      public NbtDouble read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         return NbtDouble.of(readDouble(dataInput, nbtSizeTracker));
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         return visitor.visitDouble(readDouble(input, tracker));
      }

      private static double readDouble(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(16L);
         return input.readDouble();
      }

      @Override
      public int getSizeInBytes() {
         return 8;
      }

      @Override
      public String getCrashReportName() {
         return "DOUBLE";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_Double";
      }
   };

   @Deprecated(forRemoval = true)
   public NbtDouble(double value) {
      this.value = value;
   }

   public static NbtDouble of(double value) {
      return value == 0.0 ? ZERO : new NbtDouble(value);
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeDouble(this.value);
   }

   @Override
   public int getSizeInBytes() {
      return 16;
   }

   @Override
   public byte getType() {
      return 6;
   }

   @Override
   public NbtType<NbtDouble> getNbtType() {
      return TYPE;
   }

   public NbtDouble copy() {
      return this;
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitDouble(this);
   }

   @Override
   public long longValue() {
      return (long)Math.floor(this.value);
   }

   @Override
   public int intValue() {
      return MathHelper.floor(this.value);
   }

   @Override
   public short shortValue() {
      return (short)(MathHelper.floor(this.value) & 65535);
   }

   @Override
   public byte byteValue() {
      return (byte)(MathHelper.floor(this.value) & 0xFF);
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public float floatValue() {
      return (float)this.value;
   }

   @Override
   public Number numberValue() {
      return this.value;
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      return visitor.visitDouble(this.value);
   }

   @Override
   public String toString() {
      StringNbtWriter stringNbtWriter = new StringNbtWriter();
      stringNbtWriter.visitDouble(this);
      return stringNbtWriter.getString();
   }
}
