package net.minecraft.util.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public interface CursorExceptionType<T extends Exception> {
   T create(String input, int cursor);

   static CursorExceptionType<CommandSyntaxException> create(SimpleCommandExceptionType type) {
      return (input, cursor) -> type.createWithContext(Literals.createReader(input, cursor));
   }

   static CursorExceptionType<CommandSyntaxException> create(DynamicCommandExceptionType type, String arg) {
      return (input, cursor) -> type.createWithContext(Literals.createReader(input, cursor), arg);
   }
}
