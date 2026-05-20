package net.minecraft.client.gui.hud.bar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public interface Bar {
   int WIDTH = 182;
   int HEIGHT = 5;
   int VERTICAL_OFFSET = 24;
   Bar EMPTY = new Bar() {
      @Override
      public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
      }

      @Override
      public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
      }
   };

   default int getCenterX(Window window) {
      return (window.getScaledWidth() - 182) / 2;
   }

   default int getCenterY(Window window) {
      return window.getScaledHeight() - 24 - 5;
   }

   void renderBar(DrawContext context, RenderTickCounter tickCounter);

   void renderAddons(DrawContext context, RenderTickCounter tickCounter);

   static void drawExperienceLevel(DrawContext context, TextRenderer textRenderer, int level) {
      Text text = Text.translatable("gui.experience.level", level);
      int i = (context.getScaledWindowWidth() - textRenderer.getWidth(text)) / 2;
      int j = context.getScaledWindowHeight() - 24 - 9 - 2;
      context.drawText(textRenderer, text, i + 1, j, -16777216, false);
      context.drawText(textRenderer, text, i - 1, j, -16777216, false);
      context.drawText(textRenderer, text, i, j + 1, -16777216, false);
      context.drawText(textRenderer, text, i, j - 1, -16777216, false);
      context.drawText(textRenderer, text, i, j, -8323296, false);
   }
}
