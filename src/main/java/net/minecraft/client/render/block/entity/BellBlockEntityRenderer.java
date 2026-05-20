package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.BellBlockModel;
import net.minecraft.client.render.block.entity.state.BellBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BellBlockEntityRenderer implements BlockEntityRenderer<BellBlockEntity, BellBlockEntityRenderState> {
   public static final SpriteIdentifier BELL_BODY_TEXTURE = TexturedRenderLayers.ENTITY_SPRITE_MAPPER.mapVanilla("bell/bell_body");
   private final SpriteHolder materials;
   private final BellBlockModel bellBody;

   public BellBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
      this.materials = context.spriteHolder();
      this.bellBody = new BellBlockModel(context.getLayerModelPart(EntityModelLayers.BELL));
   }

   public BellBlockEntityRenderState createRenderState() {
      return new BellBlockEntityRenderState();
   }

   public void updateRenderState(
      BellBlockEntity bellBlockEntity,
      BellBlockEntityRenderState bellBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(bellBlockEntity, bellBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      bellBlockEntityRenderState.ringTicks = bellBlockEntity.ringTicks + f;
      bellBlockEntityRenderState.shakeDirection = bellBlockEntity.ringing ? bellBlockEntity.lastSideHit : null;
   }

   public void render(
      BellBlockEntityRenderState bellBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      BellBlockModel.BellModelState bellModelState = new BellBlockModel.BellModelState(
         bellBlockEntityRenderState.ringTicks, bellBlockEntityRenderState.shakeDirection
      );
      this.bellBody.setAngles(bellModelState);
      RenderLayer renderLayer = BELL_BODY_TEXTURE.getRenderLayer(RenderLayers::entitySolid);
      orderedRenderCommandQueue.submitModel(
         this.bellBody,
         bellModelState,
         matrixStack,
         renderLayer,
         bellBlockEntityRenderState.lightmapCoordinates,
         OverlayTexture.DEFAULT_UV,
         -1,
         this.materials.getSprite(BELL_BODY_TEXTURE),
         0,
         bellBlockEntityRenderState.crumblingOverlay
      );
   }
}
