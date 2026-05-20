package net.minecraft.client.toast;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TutorialToast implements Toast {
   private static final Identifier TEXTURE = Identifier.ofVanilla("toast/tutorial");
   public static final int PROGRESS_BAR_WIDTH = 154;
   public static final int PROGRESS_BAR_HEIGHT = 1;
   public static final int PROGRESS_BAR_X = 3;
   public static final int field_55091 = 4;
   private static final int field_55092 = 7;
   private static final int field_55093 = 3;
   private static final int field_55094 = 11;
   private static final int field_55095 = 30;
   private static final int field_55096 = 126;
   private final TutorialToast.Type type;
   private final List<OrderedText> text;
   private Toast.Visibility visibility = Toast.Visibility.SHOW;
   private long lastTime;
   private float lastProgress;
   private float progress;
   private final boolean hasProgressBar;
   private final int displayDuration;

   public TutorialToast(TextRenderer textRenderer, TutorialToast.Type type, Text title, @Nullable Text description, boolean hasProgressBar, int displayDuration) {
      this.type = type;
      this.text = new ArrayList<>(2);
      this.text.addAll(textRenderer.wrapLines(title.copy().withColor(-11534256), 126));
      if (description != null) {
         this.text.addAll(textRenderer.wrapLines(description, 126));
      }

      this.hasProgressBar = hasProgressBar;
      this.displayDuration = displayDuration;
   }

   public TutorialToast(TextRenderer textRenderer, TutorialToast.Type type, Text title, @Nullable Text description, boolean hasProgressBar) {
      this(textRenderer, type, title, description, hasProgressBar, 0);
   }

   @Override
   public Toast.Visibility getVisibility() {
      return this.visibility;
   }

   @Override
   public void update(ToastManager manager, long time) {
      if (this.displayDuration > 0) {
         this.progress = Math.min((float)time / this.displayDuration, 1.0F);
         this.lastProgress = this.progress;
         this.lastTime = time;
         if (time > this.displayDuration) {
            this.hide();
         }
      } else if (this.hasProgressBar) {
         this.lastProgress = MathHelper.clampedLerp((float)(time - this.lastTime) / 100.0F, this.lastProgress, this.progress);
         this.lastTime = time;
      }
   }

   @Override
   public int getHeight() {
      return 7 + this.getTextHeight() + 3;
   }

   private int getTextHeight() {
      return Math.max(this.text.size(), 2) * 11;
   }

   @Override
   public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
      int i = this.getHeight();
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, this.getWidth(), i);
      this.type.drawIcon(context, 6, 6);
      int j = this.text.size() * 11;
      int k = 7 + (this.getTextHeight() - j) / 2;

      for (int l = 0; l < this.text.size(); l++) {
         context.drawText(textRenderer, this.text.get(l), 30, k + l * 11, -16777216, false);
      }

      if (this.hasProgressBar) {
         int l = i - 4;
         context.fill(3, l, 157, l + 1, -1);
         int m;
         if (this.progress >= this.lastProgress) {
            m = -16755456;
         } else {
            m = -11206656;
         }

         context.fill(3, l, (int)(3.0F + 154.0F * this.lastProgress), l + 1, m);
      }
   }

   public void hide() {
      this.visibility = Toast.Visibility.HIDE;
   }

   public void setProgress(float progress) {
      this.progress = progress;
   }

   @Environment(EnvType.CLIENT)
   public static enum Type {
      MOVEMENT_KEYS(Identifier.ofVanilla("toast/movement_keys")),
      MOUSE(Identifier.ofVanilla("toast/mouse")),
      TREE(Identifier.ofVanilla("toast/tree")),
      RECIPE_BOOK(Identifier.ofVanilla("toast/recipe_book")),
      WOODEN_PLANKS(Identifier.ofVanilla("toast/wooden_planks")),
      SOCIAL_INTERACTIONS(Identifier.ofVanilla("toast/social_interactions")),
      RIGHT_CLICK(Identifier.ofVanilla("toast/right_click"));

      private final Identifier texture;

      private Type(final Identifier texture) {
         this.texture = texture;
      }

      public void drawIcon(DrawContext context, int x, int y) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, this.texture, x, y, 20, 20);
      }
   }
}
