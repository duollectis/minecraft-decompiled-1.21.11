package net.minecraft.client.gui.render;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.OversizedItemGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class OversizedItemGuiElementRenderer extends SpecialGuiElementRenderer<OversizedItemGuiElementRenderState> {
   private boolean oversized;
   private @Nullable Object modelKey;

   public OversizedItemGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
      super(immediate);
   }

   public boolean isOversized() {
      return this.oversized;
   }

   public void clearOversized() {
      this.oversized = false;
   }

   public void clearModel() {
      this.modelKey = null;
   }

   @Override
   public Class<OversizedItemGuiElementRenderState> getElementClass() {
      return OversizedItemGuiElementRenderState.class;
   }

   protected void render(OversizedItemGuiElementRenderState oversizedItemGuiElementRenderState, MatrixStack matrixStack) {
      matrixStack.scale(1.0F, -1.0F, -1.0F);
      ItemGuiElementRenderState itemGuiElementRenderState = oversizedItemGuiElementRenderState.guiItemRenderState();
      ScreenRect screenRect = itemGuiElementRenderState.oversizedBounds();
      Objects.requireNonNull(screenRect);
      float f = (screenRect.getLeft() + screenRect.getRight()) / 2.0F;
      float g = (screenRect.getTop() + screenRect.getBottom()) / 2.0F;
      float h = itemGuiElementRenderState.x() + 8.0F;
      float i = itemGuiElementRenderState.y() + 8.0F;
      matrixStack.translate((h - f) / 16.0F, (g - i) / 16.0F, 0.0F);
      KeyedItemRenderState keyedItemRenderState = itemGuiElementRenderState.state();
      boolean bl = !keyedItemRenderState.isSideLit();
      if (bl) {
         MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);
      } else {
         MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
      }

      RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
      OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl = renderDispatcher.getQueue();
      keyedItemRenderState.render(matrixStack, orderedRenderCommandQueueImpl, 15728880, OverlayTexture.DEFAULT_UV, 0);
      renderDispatcher.render();
      this.modelKey = keyedItemRenderState.getModelKey();
   }

   public void renderElement(OversizedItemGuiElementRenderState oversizedItemGuiElementRenderState, GuiRenderState guiRenderState) {
      super.renderElement(oversizedItemGuiElementRenderState, guiRenderState);
      this.oversized = true;
   }

   public boolean shouldBypassScaling(OversizedItemGuiElementRenderState oversizedItemGuiElementRenderState) {
      KeyedItemRenderState keyedItemRenderState = oversizedItemGuiElementRenderState.guiItemRenderState().state();
      return !keyedItemRenderState.isAnimated() && keyedItemRenderState.getModelKey().equals(this.modelKey);
   }

   @Override
   protected float getYOffset(int height, int windowScaleFactor) {
      return height / 2.0F;
   }

   @Override
   protected String getName() {
      return "oversized_item";
   }
}
