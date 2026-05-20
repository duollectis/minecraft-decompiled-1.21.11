package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TooltipBackgroundRenderer {
   private static final Identifier DEFAULT_BACKGROUND_TEXTURE = Identifier.ofVanilla("tooltip/background");
   private static final Identifier DEFAULT_FRAME_TEXTURE = Identifier.ofVanilla("tooltip/frame");
   public static final int field_41688 = 12;
   private static final int field_41693 = 3;
   public static final int field_41689 = 3;
   public static final int field_41690 = 3;
   public static final int field_41691 = 3;
   public static final int field_41692 = 3;
   private static final int field_54153 = 9;

   public static void render(DrawContext context, int x, int y, int width, int height, @Nullable Identifier texture) {
      int i = x - 3 - 9;
      int j = y - 3 - 9;
      int k = width + 3 + 3 + 18;
      int l = height + 3 + 3 + 18;
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getBackgroundTexture(texture), i, j, k, l);
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getFrameTexture(texture), i, j, k, l);
   }

   private static Identifier getBackgroundTexture(@Nullable Identifier texture) {
      return texture == null ? DEFAULT_BACKGROUND_TEXTURE : texture.withPath(name -> "tooltip/" + name + "_background");
   }

   private static Identifier getFrameTexture(@Nullable Identifier texture) {
      return texture == null ? DEFAULT_FRAME_TEXTURE : texture.withPath(name -> "tooltip/" + name + "_frame");
   }
}
