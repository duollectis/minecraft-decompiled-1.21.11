package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MessageScreen extends Screen {
   private @Nullable NarratedMultilineTextWidget textWidget;

   public MessageScreen(Text text) {
      super(text);
   }

   @Override
   protected void init() {
      this.textWidget = this.addDrawableChild(
         NarratedMultilineTextWidget.builder(this.title, this.textRenderer, 12).innerWidth(this.textRenderer.getWidth(this.title)).build()
      );
      this.refreshWidgetPositions();
   }

   @Override
   protected void refreshWidgetPositions() {
      if (this.textWidget != null) {
         this.textWidget.setPosition(this.width / 2 - this.textWidget.getWidth() / 2, this.height / 2 - 9 / 2);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean hasUsageText() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.renderPanoramaBackground(context, deltaTicks);
      this.applyBlur(context);
      this.renderDarkening(context);
   }
}
