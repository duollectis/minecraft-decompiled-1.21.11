package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class KeybindTextContent implements TextContent {
   public static final MapCodec<KeybindTextContent> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.STRING.fieldOf("keybind").forGetter(content -> content.key)).apply(instance, KeybindTextContent::new)
   );
   private final String key;
   private @Nullable Supplier<Text> translated;

   public KeybindTextContent(String key) {
      this.key = key;
   }

   private Text getTranslated() {
      if (this.translated == null) {
         this.translated = KeybindTranslations.factory.apply(this.key);
      }

      return this.translated.get();
   }

   @Override
   public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
      return this.getTranslated().visit(visitor);
   }

   @Override
   public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
      return this.getTranslated().visit(visitor, style);
   }

   @Override
   public boolean equals(Object o) {
      return this == o ? true : o instanceof KeybindTextContent keybindTextContent && this.key.equals(keybindTextContent.key);
   }

   @Override
   public int hashCode() {
      return this.key.hashCode();
   }

   @Override
   public String toString() {
      return "keybind{" + this.key + "}";
   }

   public String getKey() {
      return this.key;
   }

   @Override
   public MapCodec<KeybindTextContent> getCodec() {
      return CODEC;
   }
}
