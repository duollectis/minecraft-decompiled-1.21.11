package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TabButtonWidget extends ClickableWidget.InactivityIndicatingWidget {
   private static final ButtonTextures TAB_BUTTON_TEXTURES = new ButtonTextures(
      Identifier.ofVanilla("widget/tab_selected"),
      Identifier.ofVanilla("widget/tab"),
      Identifier.ofVanilla("widget/tab_selected_highlighted"),
      Identifier.ofVanilla("widget/tab_highlighted")
   );
   private static final int field_43063 = 3;
   private static final int field_43064 = 1;
   private static final int field_43065 = 1;
   private static final int field_43066 = 4;
   private static final int field_43067 = 2;
   private final TabManager tabManager;
   private final Tab tab;

   public TabButtonWidget(TabManager tabManager, Tab tab, int width, int height) {
      super(0, 0, width, height, tab.getTitle());
      this.tabManager = tabManager;
      this.tab = tab;
   }

   @Override
   public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      context.drawGuiTexture(
         RenderPipelines.GUI_TEXTURED, TAB_BUTTON_TEXTURES.get(this.isCurrentTab(), this.isSelected()), this.getX(), this.getY(), this.width, this.height
      );
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      int i = this.active ? -1 : -6250336;
      if (this.isCurrentTab()) {
         this.renderBackgroundTexture(context, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
         this.drawCurrentTabLine(context, textRenderer, i);
      }

      this.drawMessage(context.getHoverListener(this, DrawContext.HoverType.NONE));
      this.setCursor(context);
   }

   protected void renderBackgroundTexture(DrawContext context, int left, int top, int right, int bottom) {
      Screen.renderBackgroundTexture(context, Screen.MENU_BACKGROUND_TEXTURE, left, top, 0.0F, 0.0F, right - left, bottom - top);
   }

   private void drawMessage(DrawnTextConsumer textConsumer) {
      int i = this.getX() + 1;
      int j = this.getY() + (this.isCurrentTab() ? 0 : 3);
      int k = this.getX() + this.getWidth() - 1;
      int l = this.getY() + this.getHeight();
      textConsumer.text(this.getMessage(), i, k, j, l);
   }

   private void drawCurrentTabLine(DrawContext context, TextRenderer textRenderer, int color) {
      int i = Math.min(textRenderer.getWidth(this.getMessage()), this.getWidth() - 4);
      int j = this.getX() + (this.getWidth() - i) / 2;
      int k = this.getY() + this.getHeight() - 2;
      context.fill(j, k, j + i, k + 1, color);
   }

   @Override
   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, Text.translatable("gui.narrate.tab", this.tab.getTitle()));
      builder.put(NarrationPart.HINT, this.tab.getNarratedHint());
   }

   @Override
   public void playDownSound(SoundManager soundManager) {
   }

   public Tab getTab() {
      return this.tab;
   }

   public boolean isCurrentTab() {
      return this.tabManager.getCurrentTab() == this.tab;
   }
}
