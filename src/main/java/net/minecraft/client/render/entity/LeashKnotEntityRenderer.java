package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LeashKnotEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class LeashKnotEntityRenderer extends EntityRenderer<LeashKnotEntity, EntityRenderState> {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/lead_knot.png");
   private final LeashKnotEntityModel field_53192;

   public LeashKnotEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.field_53192 = new LeashKnotEntityModel(context.getPart(EntityModelLayers.LEASH_KNOT));
   }

   @Override
   public void render(EntityRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
      matrices.push();
      matrices.scale(-1.0F, -1.0F, 1.0F);
      queue.submitModel(
         this.field_53192,
         renderState,
         matrices,
         this.field_53192.getLayer(TEXTURE),
         renderState.light,
         OverlayTexture.DEFAULT_UV,
         renderState.outlineColor,
         null
      );
      matrices.pop();
      super.render(renderState, matrices, queue, cameraState);
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
