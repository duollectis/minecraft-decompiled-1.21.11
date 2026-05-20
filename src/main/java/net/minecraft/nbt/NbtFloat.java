package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.math.MathHelper;

public record NbtFloat(float value) implements AbstractNbtNumber {
   private static final int SIZE = 12;
   public static final NbtFloat ZERO = new NbtFloat(0.0F);
   public static final NbtType<NbtFloat> TYPE = new NbtType.OfFixedSize<NbtFloat>() {
      public NbtFloat read(DataInput dataInput, NbtSizeTracker nbtSizeTracker) throws IOException {
         return NbtFloat.of(readFloat(dataInput, nbtSizeTracker));
      }

      @Override
      public NbtScanner.Result doAccept(DataInput input, NbtScanner visitor, NbtSizeTracker tracker) throws IOException {
         return visitor.visitFloat(readFloat(input, tracker));
      }

      private static float readFloat(DataInput input, NbtSizeTracker tracker) throws IOException {
         tracker.add(12L);
         return input.readFloat();
      }

      @Override
      public int getSizeInBytes() {
         return 4;
      }

      @Override
      public String getCrashReportName() {
         return "FLOAT";
      }

      @Override
      public String getCommandFeedbackName() {
         return "TAG_Float";
      }
   };

   @Deprecated(forRemoval = true)
   public NbtFloat(float value) {
      this.value = value;
   }

   public static NbtFloat of(float value) {
      return value == 0.0F ? ZERO : new NbtFloat(value);
   }

   @Override
   public void write(DataOutput output) throws IOException {
      output.writeFloat(this.value);
   }

   @Override
   public int getSizeInBytes() {
      return 12;
   }

   @Override
   public byte getType() {
      return 5;
   }

   @Override
   public NbtType<NbtFloat> getNbtType() {
      return TYPE;
   }

   public NbtFloat copy() {
      return this;
   }

   @Override
   public void accept(NbtElementVisitor visitor) {
      visitor.visitFloat(this);
   }

   @Override
   public long longValue() {
      return (long)this.value;
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
      return this.value;
   }

   @Override
   public Number numberValue() {
      return this.value;
   }

   @Override
   public NbtScanner.Result doAccept(NbtScanner visitor) {
      return visitor.visitFloat(this.value);
   }

   @Override
   public String toString() {
      StringNbtWriter stringNbtWriter = new StringNbtWriter();
      stringNbtWriter.visitFloat(this);
      return stringNbtWriter.getString();
   }
}
