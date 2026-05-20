package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

@Environment(EnvType.CLIENT)
public record EmptyGlyphRect(float x, float y, float advance, float ascent, float height, Style style) implements GlyphRect {
   public static final float DEFAULT_HEIGHT = 9.0F;
   public static final float DEFAULT_ASCENT = 7.0F;

   @Override
   public float getLeft() {
      return this.x;
   }

   @Override
   public float getTop() {
      return this.y + 7.0F - this.ascent;
   }

   @Override
   public float getRight() {
      return this.x + this.advance;
   }

   @Override
   public float getBottom() {
      return this.getTop() + this.height;
   }
}
