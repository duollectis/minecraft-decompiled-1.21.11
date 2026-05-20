package net.minecraft.text;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.jspecify.annotations.Nullable;

public interface TextContent {
   default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
      return Optional.empty();
   }

   default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
      return Optional.empty();
   }

   default MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
      return MutableText.of(this);
   }

   MapCodec<? extends TextContent> getCodec();
}
