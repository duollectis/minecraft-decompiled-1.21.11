package net.minecraft.client.render.item.model.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface SimpleSpecialModelRenderer extends SpecialModelRenderer<Void> {
   default @Nullable Void getData(ItemStack itemStack) {
      return null;
   }

   default void render(
      @Nullable Void void_,
      ItemDisplayContext itemDisplayContext,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      int j,
      boolean bl,
      int k
   ) {
      this.render(itemDisplayContext, matrixStack, orderedRenderCommandQueue, i, j, bl, k);
   }

   void render(ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i);
}
