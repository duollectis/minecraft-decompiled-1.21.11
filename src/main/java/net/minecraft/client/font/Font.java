package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface Font extends AutoCloseable {
   float DEFAULT_ASCENT = 7.0F;

   @Override
   default void close() {
   }

   default @Nullable Glyph getGlyph(int codePoint) {
      return null;
   }

   IntSet getProvidedGlyphs();

   @Environment(EnvType.CLIENT)
   public record FontFilterPair(Font provider, FontFilterType.FilterMap filter) implements AutoCloseable {
      @Override
      public void close() {
         this.provider.close();
      }
   }
}
