package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.DelegatingDataOutput;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;

public class NbtIo {
   private static final OpenOption[] OPEN_OPTIONS = new OpenOption[]{
      StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   };

   public static NbtCompound readCompressed(Path path, NbtSizeTracker tagSizeTracker) throws IOException {
      NbtCompound var4;
      try (
         InputStream inputStream = Files.newInputStream(path);
         InputStream inputStream2 = new FixedBufferInputStream(inputStream);
      ) {
         var4 = readCompressed(inputStream2, tagSizeTracker);
      }

      return var4;
   }

   private static DataInputStream decompress(InputStream stream) throws IOException {
      return new DataInputStream(new FixedBufferInputStream(new GZIPInputStream(stream)));
   }

   private static DataOutputStream compress(OutputStream stream) throws IOException {
      return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream)));
   }

   public static NbtCompound readCompressed(InputStream stream, NbtSizeTracker tagSizeTracker) throws IOException {
      NbtCompound var3;
      try (DataInputStream dataInputStream = decompress(stream)) {
         var3 = readCompound(dataInputStream, tagSizeTracker);
      }

      return var3;
   }

   public static void scanCompressed(Path path, NbtScanner scanner, NbtSizeTracker tracker) throws IOException {
      try (
         InputStream inputStream = Files.newInputStream(path);
         InputStream inputStream2 = new FixedBufferInputStream(inputStream);
      ) {
         scanCompressed(inputStream2, scanner, tracker);
      }
   }

   public static void scanCompressed(InputStream stream, NbtScanner scanner, NbtSizeTracker tracker) throws IOException {
      try (DataInputStream dataInputStream = decompress(stream)) {
         scan(dataInputStream, scanner, tracker);
      }
   }

   public static void writeCompressed(NbtCompound nbt, Path path) throws IOException {
      try (
         OutputStream outputStream = Files.newOutputStream(path, OPEN_OPTIONS);
         OutputStream outputStream2 = new BufferedOutputStream(outputStream);
      ) {
         writeCompressed(nbt, outputStream2);
      }
   }

   public static void writeCompressed(NbtCompound nbt, OutputStream stream) throws IOException {
      try (DataOutputStream dataOutputStream = compress(stream)) {
         writeCompound(nbt, dataOutputStream);
      }
   }

   public static void write(NbtCompound nbt, Path path) throws IOException {
      try (
         OutputStream outputStream = Files.newOutputStream(path, OPEN_OPTIONS);
         OutputStream outputStream2 = new BufferedOutputStream(outputStream);
         DataOutputStream dataOutputStream = new DataOutputStream(outputStream2);
      ) {
         writeCompound(nbt, dataOutputStream);
      }
   }

   public static @Nullable NbtCompound read(Path path) throws IOException {
      if (!Files.exists(path)) {
         return null;
      } else {
         NbtCompound var3;
         try (
            InputStream inputStream = Files.newInputStream(path);
            DataInputStream dataInputStream = new DataInputStream(inputStream);
         ) {
            var3 = readCompound(dataInputStream, NbtSizeTracker.ofUnlimitedBytes());
         }

         return var3;
      }
   }

   public static NbtCompound readCompound(DataInput input) throws IOException {
      return readCompound(input, NbtSizeTracker.ofUnlimitedBytes());
   }

   public static NbtCompound readCompound(DataInput input, NbtSizeTracker tracker) throws IOException {
      NbtElement nbtElement = readElement(input, tracker);
      if (nbtElement instanceof NbtCompound) {
         return (NbtCompound)nbtElement;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void writeCompound(NbtCompound nbt, DataOutput output) throws IOException {
      write(nbt, output);
   }

   public static void scan(DataInput input, NbtScanner scanner, NbtSizeTracker tracker) throws IOException {
      NbtType<?> nbtType = NbtTypes.byId(input.readByte());
      if (nbtType == NbtEnd.TYPE) {
         if (scanner.start(NbtEnd.TYPE) == NbtScanner.Result.CONTINUE) {
            scanner.visitEnd();
         }
      } else {
         switch (scanner.start(nbtType)) {
            case HALT:
            default:
               break;
            case BREAK:
               NbtString.skip(input);
               nbtType.skip(input, tracker);
               break;
            case CONTINUE:
               NbtString.skip(input);
               nbtType.doAccept(input, scanner, tracker);
         }
      }
   }

   public static NbtElement read(DataInput input, NbtSizeTracker tracker) throws IOException {
      byte b = input.readByte();
      return (NbtElement)(b == 0 ? NbtEnd.INSTANCE : readElement(input, tracker, b));
   }

   public static void writeForPacket(NbtElement nbt, DataOutput output) throws IOException {
      output.writeByte(nbt.getType());
      if (nbt.getType() != 0) {
         nbt.write(output);
      }
   }

   public static void writeUnsafe(NbtElement nbt, DataOutput output) throws IOException {
      output.writeByte(nbt.getType());
      if (nbt.getType() != 0) {
         output.writeUTF("");
         nbt.write(output);
      }
   }

   public static void write(NbtElement nbt, DataOutput output) throws IOException {
      writeUnsafe(nbt, new NbtIo.InvalidUtfSkippingDataOutput(output));
   }

   @VisibleForTesting
   public static NbtElement readElement(DataInput input, NbtSizeTracker tracker) throws IOException {
      byte b = input.readByte();
      if (b == 0) {
         return NbtEnd.INSTANCE;
      } else {
         NbtString.skip(input);
         return readElement(input, tracker, b);
      }
   }

   private static NbtElement readElement(DataInput input, NbtSizeTracker tracker, byte typeId) {
      try {
         return NbtTypes.byId(typeId).read(input, tracker);
      } catch (IOException var6) {
         CrashReport crashReport = CrashReport.create(var6, "Loading NBT data");
         CrashReportSection crashReportSection = crashReport.addElement("NBT Tag");
         crashReportSection.add("Tag type", typeId);
         throw new NbtCrashException(crashReport);
      }
   }

   public static class InvalidUtfSkippingDataOutput extends DelegatingDataOutput {
      public InvalidUtfSkippingDataOutput(DataOutput dataOutput) {
         super(dataOutput);
      }

      @Override
      public void writeUTF(String string) throws IOException {
         try {
            super.writeUTF(string);
         } catch (UTFDataFormatException var3) {
            Util.logErrorOrPause("Failed to write NBT String", var3);
            super.writeUTF("");
         }
      }
   }
}
