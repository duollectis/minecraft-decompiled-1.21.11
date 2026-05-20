package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.Text;

public record CoordinateArgument(boolean relative, double value) {
   private static final char TILDE = '~';
   public static final SimpleCommandExceptionType MISSING_COORDINATE = new SimpleCommandExceptionType(Text.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType MISSING_BLOCK_POSITION = new SimpleCommandExceptionType(Text.translatable("argument.pos.missing.int"));

   public double toAbsoluteCoordinate(double offset) {
      return this.relative ? this.value + offset : this.value;
   }

   public static CoordinateArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw MISSING_COORDINATE.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         int i = reader.getCursor();
         double d = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
         String string = reader.getString().substring(i, reader.getCursor());
         if (bl && string.isEmpty()) {
            return new CoordinateArgument(true, 0.0);
         } else {
            if (!string.contains(".") && !bl && centerIntegers) {
               d += 0.5;
            }

            return new CoordinateArgument(bl, d);
         }
      }
   }

   public static CoordinateArgument parse(StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw MISSING_BLOCK_POSITION.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         double d;
         if (reader.canRead() && reader.peek() != ' ') {
            d = bl ? reader.readDouble() : reader.readInt();
         } else {
            d = 0.0;
         }

         return new CoordinateArgument(bl, d);
      }
   }

   public static boolean isRelative(StringReader reader) {
      boolean bl;
      if (reader.peek() == '~') {
         bl = true;
         reader.skip();
      } else {
         bl = false;
      }

      return bl;
   }

   public boolean isRelative() {
      return this.relative;
   }
}
