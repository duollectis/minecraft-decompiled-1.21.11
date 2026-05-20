package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Texts;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class PressableTextWidget extends ButtonWidget {
   private final TextRenderer textRenderer;
   private final net.minecraft.text.Text text;
   private final net.minecraft.text.Text hoverText;

   public PressableTextWidget(int x, int y, int width, int height, net.minecraft.text.Text text, ButtonWidget.PressAction onPress, TextRenderer textRenderer) {
      super(x, y, width, height, text, onPress, DEFAULT_NARRATION_SUPPLIER);
      this.textRenderer = textRenderer;
      this.text = text;
      this.hoverText = Texts.withStyle(text, Style.EMPTY.withUnderline(true));
   }

   @Override
   public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      net.minecraft.text.Text text = this.isSelected() ? this.hoverText : this.text;
      context.drawTextWithShadow(this.textRenderer, text, this.getX(), this.getY(), 16777215 | MathHelper.ceil(this.alpha * 255.0F) << 24);
   }
}
