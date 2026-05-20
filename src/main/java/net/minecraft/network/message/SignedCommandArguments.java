package net.minecraft.network.message;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface SignedCommandArguments {
   SignedCommandArguments EMPTY = new SignedCommandArguments() {
      @Override
      public @Nullable SignedMessage getMessage(String argumentName) {
         return null;
      }
   };

   @Nullable SignedMessage getMessage(String argumentName);

   public record Impl(Map<String, SignedMessage> arguments) implements SignedCommandArguments {
      @Override
      public @Nullable SignedMessage getMessage(String argumentName) {
         return this.arguments.get(argumentName);
      }
   }
}
