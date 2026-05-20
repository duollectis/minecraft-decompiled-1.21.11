package net.minecraft.network.message;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

public enum ChatVisibility {
   FULL(0, "options.chat.visibility.full"),
   SYSTEM(1, "options.chat.visibility.system"),
   HIDDEN(2, "options.chat.visibility.hidden");

   private static final IntFunction<ChatVisibility> BY_ID = ValueLists.createIndexToValueFunction(
      (ChatVisibility chatVisibility) -> chatVisibility.id, values(), ValueLists.OutOfBoundsHandling.WRAP
   );
   public static final Codec<ChatVisibility> CODEC = Codec.INT.xmap(BY_ID::apply, visibility -> visibility.id);
   private final int id;
   private final Text text;

   private ChatVisibility(final int id, final String translationKey) {
      this.id = id;
      this.text = Text.translatable(translationKey);
   }

   public Text getText() {
      return this.text;
   }
}
