package net.minecraft.client.render.command;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.render.FabricRenderCommandQueue;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface RenderCommandQueue extends FabricRenderCommandQueue {
   void submitShadowPieces(MatrixStack matrices, float shadowRadius, List<EntityRenderState.ShadowPiece> shadowPieces);

   void submitLabel(
      MatrixStack matrices,
      @Nullable Vec3d nameLabelPos,
      int y,
      Text label,
      boolean notSneaking,
      int light,
      double squaredDistanceToCamera,
      CameraRenderState cameraState
   );

   void submitText(
      MatrixStack matrices,
      float x,
      float y,
      OrderedText text,
      boolean dropShadow,
      TextRenderer.TextLayerType layerType,
      int light,
      int color,
      int backgroundColor,
      int outlineColor
   );

   void submitFire(MatrixStack matrices, EntityRenderState renderState, Quaternionf rotation);

   void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData);

   <S> void submitModel(
      Model<? super S> model,
      S state,
      MatrixStack matrices,
      RenderLayer renderLayer,
      int light,
      int overlay,
      int tintedColor,
      @Nullable Sprite sprite,
      int outlineColor,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
   );

   default <S> void submitModel(
      Model<? super S> model,
      S state,
      MatrixStack matrices,
      RenderLayer renderLayer,
      int light,
      int overlay,
      int outlineColor,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
   ) {
      this.submitModel(model, state, matrices, renderLayer, light, overlay, -1, null, outlineColor, crumblingOverlay);
   }

   default void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer renderLayer, int light, int overlay, @Nullable Sprite sprite) {
      this.submitModelPart(part, matrices, renderLayer, light, overlay, sprite, false, false, -1, null, 0);
   }

   default void submitModelPart(
      ModelPart part,
      MatrixStack matrices,
      RenderLayer renderLayer,
      int light,
      int overlay,
      @Nullable Sprite sprite,
      int tintedColor,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
   ) {
      this.submitModelPart(part, matrices, renderLayer, light, overlay, sprite, false, false, tintedColor, crumblingOverlay, 0);
   }

   default void submitModelPart(
      ModelPart part, MatrixStack matrices, RenderLayer renderLayer, int light, int overlay, @Nullable Sprite sprite, boolean sheeted, boolean hasGlint
   ) {
      this.submitModelPart(part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, -1, null, 0);
   }

   void submitModelPart(
      ModelPart part,
      MatrixStack matrices,
      RenderLayer renderLayer,
      int light,
      int overlay,
      @Nullable Sprite sprite,
      boolean sheeted,
      boolean hasGlint,
      int tintedColor,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
      int i
   );

   void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor);

   void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state);

   void submitBlockStateModel(
      MatrixStack matrices, RenderLayer renderLayer, BlockStateModel model, float r, float g, float b, int light, int overlay, int outlineColor
   );

   void submitItem(
      MatrixStack matrices,
      ItemDisplayContext displayContext,
      int light,
      int overlay,
      int outlineColors,
      int[] tintLayers,
      List<BakedQuad> quads,
      RenderLayer renderLayer,
      ItemRenderState.Glint glintType
   );

   void submitCustom(MatrixStack matrices, RenderLayer renderLayer, OrderedRenderCommandQueue.Custom customRenderer);

   void submitCustom(OrderedRenderCommandQueue.LayeredCustom customRenderer);
}
