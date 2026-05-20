package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record OversizedItemGuiElementRenderState(ItemGuiElementRenderState guiItemRenderState, int x1, int y1, int x2, int y2)
   implements SpecialGuiElementRenderState {
   @Override
   public float scale() {
      return 16.0F;
   }

   @Override
   public Matrix3x2f pose() {
      return this.guiItemRenderState.pose();
   }

   @Override
   public @Nullable ScreenRect scissorArea() {
      return this.guiItemRenderState.scissorArea();
   }

   @Override
   public @Nullable ScreenRect bounds() {
      return this.guiItemRenderState.bounds();
   }
}
