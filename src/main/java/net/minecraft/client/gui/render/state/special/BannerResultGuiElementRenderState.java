package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record BannerResultGuiElementRenderState(
   BannerFlagBlockModel flag,
   DyeColor baseColor,
   BannerPatternsComponent resultBannerPatterns,
   int x1,
   int y1,
   int x2,
   int y2,
   @Nullable ScreenRect scissorArea,
   @Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {
   public BannerResultGuiElementRenderState(
      BannerFlagBlockModel model, DyeColor color, BannerPatternsComponent bannerPatterns, int x1, int y1, int x2, int y2, @Nullable ScreenRect scissorArea
   ) {
      this(model, color, bannerPatterns, x1, y1, x2, y2, scissorArea, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea));
   }

   @Override
   public float scale() {
      return 16.0F;
   }
}
