package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface SpecialGuiElementRenderState extends GuiElementRenderState {
   Matrix3x2f pose = new Matrix3x2f();

   int x1();

   int x2();

   int y1();

   int y2();

   float scale();

   default Matrix3x2f pose() {
      return pose;
   }

   @Nullable ScreenRect scissorArea();

   static @Nullable ScreenRect createBounds(int x1, int y1, int x2, int y2, @Nullable ScreenRect scissorArea) {
      ScreenRect screenRect = new ScreenRect(x1, y1, x2 - x1, y2 - y1);
      return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
   }
}
