package net.minecraft.client.font;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface DrawnTextConsumer {
   double MARQUEE_PERIOD_PER_EXCESS_WIDTH = 0.5;
   double MARQUEE_MIN_PERIOD = 3.0;

   DrawnTextConsumer.Transformation getTransformation();

   void setTransformation(DrawnTextConsumer.Transformation transformation);

   default void text(int x, int y, OrderedText text) {
      this.text(Alignment.LEFT, x, y, this.getTransformation(), text);
   }

   default void text(int x, int y, Text text) {
      this.text(Alignment.LEFT, x, y, this.getTransformation(), text.asOrderedText());
   }

   default void text(Alignment alignment, int x, int y, DrawnTextConsumer.Transformation transformation, Text text) {
      this.text(alignment, x, y, transformation, text.asOrderedText());
   }

   void text(Alignment alignment, int x, int y, DrawnTextConsumer.Transformation transformation, OrderedText text);

   default void text(Alignment alignment, int x, int y, Text text) {
      this.text(alignment, x, y, text.asOrderedText());
   }

   default void text(Alignment alignment, int x, int y, OrderedText text) {
      this.text(alignment, x, y, this.getTransformation(), text);
   }

   void marqueedText(Text text, int x, int left, int right, int top, int bottom, DrawnTextConsumer.Transformation transformation);

   default void marqueedText(Text text, int x, int left, int right, int top, int bottom) {
      this.marqueedText(text, x, left, right, top, bottom, this.getTransformation());
   }

   default void text(Text text, int left, int right, int top, int bottom) {
      this.marqueedText(text, (left + right) / 2, left, right, top, bottom);
   }

   default void marqueedText(
      Text text, int x, int left, int right, int top, int bottom, int width, int lineHeight, DrawnTextConsumer.Transformation transformation
   ) {
      int i = (top + bottom - lineHeight) / 2 + 1;
      int j = right - left;
      if (width > j) {
         int k = width - j;
         double d = Util.getMeasuringTimeMs() / 1000.0;
         double e = Math.max(k * 0.5, 3.0);
         double f = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d / e)) / 2.0 + 0.5;
         double g = MathHelper.lerp(f, 0.0, (double)k);
         DrawnTextConsumer.Transformation transformation2 = transformation.withScissor(left, right, top, bottom);
         this.text(Alignment.LEFT, left - (int)g, i, transformation2, text.asOrderedText());
      } else {
         int k = MathHelper.clamp(x, left + width / 2, right - width / 2);
         this.text(Alignment.CENTER, k, i, text);
      }
   }

   static void handleHover(TextGuiElementRenderState renderState, float mouseX, float mouseY, Consumer<Style> styleCallback) {
      ScreenRect screenRect = renderState.bounds();
      if (screenRect != null && screenRect.contains((int)mouseX, (int)mouseY)) {
         Vector2fc vector2fc = renderState.matrix.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
         final float f = vector2fc.x();
         final float g = vector2fc.y();
         renderState.prepare().draw(new TextRenderer.GlyphDrawer() {
            @Override
            public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
               this.addGlyphInternal(glyph);
            }

            @Override
            public void drawEmptyGlyphRect(EmptyGlyphRect rect) {
               this.addGlyphInternal(rect);
            }

            private void addGlyphInternal(GlyphRect glyph) {
               if (DrawnTextConsumer.isWithinBounds(f, g, glyph.getLeft(), glyph.getTop(), glyph.getRight(), glyph.getBottom())) {
                  styleCallback.accept(glyph.style());
               }
            }
         });
      }
   }

   static boolean isWithinBounds(float x, float y, float left, float top, float right, float bottom) {
      return x >= left && x < right && y >= top && y < bottom;
   }

   @Environment(EnvType.CLIENT)
   public static class ClickHandler implements DrawnTextConsumer {
      private static final DrawnTextConsumer.Transformation DEFAULT_TRANSFORMATION = new DrawnTextConsumer.Transformation(new Matrix3x2f());
      private final TextRenderer textRenderer;
      private final int clickX;
      private final int clickY;
      private DrawnTextConsumer.Transformation transformation = DEFAULT_TRANSFORMATION;
      private boolean insert;
      private @Nullable Style style;
      private final Consumer<Style> setStyleCallback = style -> {
         if (style.getClickEvent() != null || this.insert && style.getInsertion() != null) {
            this.style = style;
         }
      };

      public ClickHandler(TextRenderer textRenderer, int clickX, int clickY) {
         this.textRenderer = textRenderer;
         this.clickX = clickX;
         this.clickY = clickY;
      }

      @Override
      public DrawnTextConsumer.Transformation getTransformation() {
         return this.transformation;
      }

      @Override
      public void setTransformation(DrawnTextConsumer.Transformation transformation) {
         this.transformation = transformation;
      }

      @Override
      public void text(Alignment alignment, int x, int y, DrawnTextConsumer.Transformation transformation, OrderedText text) {
         int i = alignment.getAdjustedX(x, this.textRenderer, text);
         TextGuiElementRenderState textGuiElementRenderState = new TextGuiElementRenderState(
            this.textRenderer, text, transformation.pose(), i, y, ColorHelper.getWhite(transformation.opacity()), 0, true, true, transformation.scissor()
         );
         DrawnTextConsumer.handleHover(textGuiElementRenderState, this.clickX, this.clickY, this.setStyleCallback);
      }

      @Override
      public void marqueedText(Text text, int x, int left, int right, int top, int bottom, DrawnTextConsumer.Transformation transformation) {
         int i = this.textRenderer.getWidth(text);
         int j = 9;
         this.marqueedText(text, x, left, right, top, bottom, i, j, transformation);
      }

      public DrawnTextConsumer.ClickHandler insert(boolean insert) {
         this.insert = insert;
         return this;
      }

      public @Nullable Style getStyle() {
         return this.style;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Transformation(Matrix3x2fc pose, float opacity, @Nullable ScreenRect scissor) {
      public Transformation(Matrix3x2fc pose) {
         this(pose, 1.0F, null);
      }

      public DrawnTextConsumer.Transformation withPose(Matrix3x2fc pose) {
         return new DrawnTextConsumer.Transformation(pose, this.opacity, this.scissor);
      }

      public DrawnTextConsumer.Transformation scaled(float scale) {
         return this.withPose(this.pose.scale(scale, scale, new Matrix3x2f()));
      }

      public DrawnTextConsumer.Transformation withOpacity(float opacity) {
         return this.opacity == opacity ? this : new DrawnTextConsumer.Transformation(this.pose, opacity, this.scissor);
      }

      public DrawnTextConsumer.Transformation withScissor(ScreenRect scissor) {
         return scissor.equals(this.scissor) ? this : new DrawnTextConsumer.Transformation(this.pose, this.opacity, scissor);
      }

      public DrawnTextConsumer.Transformation withScissor(int left, int right, int top, int bottom) {
         ScreenRect screenRect = new ScreenRect(left, top, right - left, bottom - top).transform(this.pose);
         if (this.scissor != null) {
            screenRect = Objects.requireNonNullElse(this.scissor.intersection(screenRect), ScreenRect.empty());
         }

         return this.withScissor(screenRect);
      }
   }
}
