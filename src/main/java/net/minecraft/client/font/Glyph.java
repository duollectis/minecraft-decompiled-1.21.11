package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Glyph {
   GlyphMetrics getMetrics();

   BakedGlyph bake(Glyph.AbstractGlyphBaker baker);

   @Environment(EnvType.CLIENT)
   public interface AbstractGlyphBaker {
      BakedGlyph bake(GlyphMetrics metrics, UploadableGlyph renderable);

      BakedGlyph getBlankGlyph();
   }
}
