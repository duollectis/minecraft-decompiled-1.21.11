package net.minecraft.client.gui.widget;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class EditBoxWidget extends ScrollableTextFieldWidget {
   private static final int CURSOR_PADDING = 1;
   private static final int CURSOR_COLOR = -3092272;
   private static final String UNDERSCORE = "_";
   private static final int UNFOCUSED_BOX_TEXT_COLOR = ColorHelper.withAlpha(204, -2039584);
   private static final int CURSOR_BLINK_INTERVAL = 300;
   private final TextRenderer textRenderer;
   private final Text placeholder;
   private final EditBox editBox;
   private final int textColor;
   private final boolean textShadow;
   private final int cursorColor;
   private long lastSwitchFocusTime = Util.getMeasuringTimeMs();

   EditBoxWidget(
      TextRenderer textRenderer,
      int x,
      int y,
      int width,
      int height,
      Text placeholder,
      Text message,
      int textColor,
      boolean textShadow,
      int cursorColor,
      boolean hasBackground,
      boolean hasOverlay
   ) {
      super(x, y, width, height, message, hasBackground, hasOverlay);
      this.textRenderer = textRenderer;
      this.textShadow = textShadow;
      this.textColor = textColor;
      this.cursorColor = cursorColor;
      this.placeholder = placeholder;
      this.editBox = new EditBox(textRenderer, width - this.getPadding());
      this.editBox.setCursorChangeListener(this::onCursorChange);
   }

   public void setMaxLength(int maxLength) {
      this.editBox.setMaxLength(maxLength);
   }

   public void setMaxLines(int maxLines) {
      this.editBox.setMaxLines(maxLines);
   }

   public void setChangeListener(Consumer<String> changeListener) {
      this.editBox.setChangeListener(changeListener);
   }

   public void setText(String text) {
      this.setText(text, false);
   }

   public void setText(String text, boolean allowOverflow) {
      this.editBox.setText(text, allowOverflow);
   }

   public String getText() {
      return this.editBox.getText();
   }

   @Override
   public void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, Text.translatable("gui.narrate.editBox", this.getMessage(), this.getText()));
   }

   @Override
   public void onClick(Click click, boolean doubled) {
      if (doubled) {
         this.editBox.selectWord();
      } else {
         this.editBox.setSelecting(click.hasShift());
         this.moveCursor(click.x(), click.y());
      }
   }

   @Override
   protected void onDrag(Click click, double offsetX, double offsetY) {
      this.editBox.setSelecting(true);
      this.moveCursor(click.x(), click.y());
      this.editBox.setSelecting(click.hasShift());
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      return this.editBox.handleSpecialKey(input);
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.visible && this.isFocused() && input.isValidChar()) {
         this.editBox.replaceSelection(input.asString());
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      String string = this.editBox.getText();
      if (string.isEmpty() && !this.isFocused()) {
         context.drawWrappedTextWithShadow(
            this.textRenderer, this.placeholder, this.getTextX(), this.getTextY(), this.width - this.getPadding(), UNFOCUSED_BOX_TEXT_COLOR
         );
      } else {
         int i = this.editBox.getCursor();
         boolean bl = this.isFocused() && (Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L;
         boolean bl2 = i < string.length();
         int j = 0;
         int k = 0;
         int l = this.getTextY();
         boolean bl3 = false;

         for (EditBox.Substring substring : this.editBox.getLines()) {
            boolean bl4 = this.isVisible(l, l + 9);
            int m = this.getTextX();
            if (bl && bl2 && i >= substring.beginIndex() && i <= substring.endIndex()) {
               if (bl4) {
                  String string2 = string.substring(substring.beginIndex(), i);
                  context.drawText(this.textRenderer, string2, m, l, this.textColor, this.textShadow);
                  j = m + this.textRenderer.getWidth(string2);
                  if (!bl3) {
                     context.fill(j, l - 1, j + 1, l + 1 + 9, this.cursorColor);
                     bl3 = true;
                  }

                  context.drawText(this.textRenderer, string.substring(i, substring.endIndex()), j, l, this.textColor, this.textShadow);
               }
            } else {
               if (bl4) {
                  String string2 = string.substring(substring.beginIndex(), substring.endIndex());
                  context.drawText(this.textRenderer, string2, m, l, this.textColor, this.textShadow);
                  j = m + this.textRenderer.getWidth(string2) - 1;
               }

               k = l;
            }

            l += 9;
         }

         if (bl && !bl2 && this.isVisible(k, k + 9)) {
            context.drawText(this.textRenderer, "_", j + 1, k, this.cursorColor, this.textShadow);
         }

         if (this.editBox.hasSelection()) {
            EditBox.Substring substring2 = this.editBox.getSelection();
            int n = this.getTextX();
            l = this.getTextY();

            for (EditBox.Substring substring3 : this.editBox.getLines()) {
               if (substring2.beginIndex() > substring3.endIndex()) {
                  l += 9;
               } else {
                  if (substring3.beginIndex() > substring2.endIndex()) {
                     break;
                  }

                  if (this.isVisible(l, l + 9)) {
                     int o = this.textRenderer.getWidth(string.substring(substring3.beginIndex(), Math.max(substring2.beginIndex(), substring3.beginIndex())));
                     int p;
                     if (substring2.endIndex() > substring3.endIndex()) {
                        p = this.width - this.getTextMargin();
                     } else {
                        p = this.textRenderer.getWidth(string.substring(substring3.beginIndex(), substring2.endIndex()));
                     }

                     context.drawSelection(n + o, l, n + p, l + 9, true);
                  }

                  l += 9;
               }
            }
         }

         if (this.isHovered()) {
            context.setCursor(StandardCursors.IBEAM);
         }
      }
   }

   @Override
   protected void renderOverlay(DrawContext context) {
      super.renderOverlay(context);
      if (this.editBox.hasMaxLength()) {
         int i = this.editBox.getMaxLength();
         Text text = Text.translatable("gui.multiLineEditBox.character_limit", this.editBox.getText().length(), i);
         context.drawTextWithShadow(
            this.textRenderer, text, this.getX() + this.width - this.textRenderer.getWidth(text), this.getY() + this.height + 4, -6250336
         );
      }
   }

   @Override
   public int getContentsHeight() {
      return 9 * this.editBox.getLineCount();
   }

   @Override
   protected double getDeltaYPerScroll() {
      return 9.0 / 2.0;
   }

   private void onCursorChange() {
      double d = this.getScrollY();
      EditBox.Substring substring = this.editBox.getLine((int)(d / 9.0));
      if (this.editBox.getCursor() <= substring.beginIndex()) {
         d = this.editBox.getCurrentLineIndex() * 9;
      } else {
         EditBox.Substring substring2 = this.editBox.getLine((int)((d + this.height) / 9.0) - 1);
         if (this.editBox.getCursor() > substring2.endIndex()) {
            d = this.editBox.getCurrentLineIndex() * 9 - this.height + 9 + this.getPadding();
         }
      }

      this.setScrollY(d);
   }

   private void moveCursor(double mouseX, double mouseY) {
      double d = mouseX - this.getX() - this.getTextMargin();
      double e = mouseY - this.getY() - this.getTextMargin() + this.getScrollY();
      this.editBox.moveCursor(d, e);
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (focused) {
         this.lastSwitchFocusTime = Util.getMeasuringTimeMs();
      }
   }

   public static EditBoxWidget.Builder builder() {
      return new EditBoxWidget.Builder();
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private int x;
      private int y;
      private Text placeholder = ScreenTexts.EMPTY;
      private int textColor = -2039584;
      private boolean textShadow = true;
      private int cursorColor = -3092272;
      private boolean hasBackground = true;
      private boolean hasOverlay = true;

      public EditBoxWidget.Builder x(int x) {
         this.x = x;
         return this;
      }

      public EditBoxWidget.Builder y(int y) {
         this.y = y;
         return this;
      }

      public EditBoxWidget.Builder placeholder(Text placeholder) {
         this.placeholder = placeholder;
         return this;
      }

      public EditBoxWidget.Builder textColor(int textColor) {
         this.textColor = textColor;
         return this;
      }

      public EditBoxWidget.Builder textShadow(boolean textShadow) {
         this.textShadow = textShadow;
         return this;
      }

      public EditBoxWidget.Builder cursorColor(int cursorColor) {
         this.cursorColor = cursorColor;
         return this;
      }

      public EditBoxWidget.Builder hasBackground(boolean hasBackground) {
         this.hasBackground = hasBackground;
         return this;
      }

      public EditBoxWidget.Builder hasOverlay(boolean hasOverlay) {
         this.hasOverlay = hasOverlay;
         return this;
      }

      public EditBoxWidget build(TextRenderer textRenderer, int width, int height, Text message) {
         return new EditBoxWidget(
            textRenderer,
            this.x,
            this.y,
            width,
            height,
            this.placeholder,
            message,
            this.textColor,
            this.textShadow,
            this.cursorColor,
            this.hasBackground,
            this.hasOverlay
         );
      }
   }
}
