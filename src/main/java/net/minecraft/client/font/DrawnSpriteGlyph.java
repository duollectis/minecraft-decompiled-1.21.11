package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public interface DrawnSpriteGlyph extends TextDrawable.DrawnGlyphRect {
   float field_63888 = 8.0F;
   float field_63889 = 8.0F;
   float field_63890 = 8.0F;

   @Override
   default void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth) {
      float f = 0.0F;
      if (this.shadowColor() != 0) {
         this.draw(matrix4f, consumer, light, this.shadowOffset(), this.shadowOffset(), 0.0F, this.shadowColor());
         if (!noDepth) {
            f += 0.03F;
         }
      }

      this.draw(matrix4f, consumer, light, 0.0F, 0.0F, f, this.color());
   }

   void draw(Matrix4f matrix, VertexConsumer vertexConsumer, int light, float x, float y, float z, int color);

   float x();

   float y();

   int color();

   int shadowColor();

   float shadowOffset();

   default float getWidth() {
      return 8.0F;
   }

   default float getHeight() {
      return 8.0F;
   }

   default float getAscent() {
      return 8.0F;
   }

   @Override
   default float getEffectiveMinX() {
      return this.x();
   }

   @Override
   default float getEffectiveMaxX() {
      return this.getEffectiveMinX() + this.getWidth();
   }

   @Override
   default float getEffectiveMinY() {
      return this.y() + 7.0F - this.getAscent();
   }

   @Override
   default float getEffectiveMaxY() {
      return this.getTop() + this.getHeight();
   }
}
