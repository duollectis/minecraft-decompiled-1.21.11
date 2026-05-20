package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.ExperienceOrbEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ExperienceOrbEntityRenderer extends EntityRenderer<ExperienceOrbEntity, ExperienceOrbEntityRenderState> {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/experience_orb.png");
   private static final RenderLayer LAYER = RenderLayers.itemEntityTranslucentCull(TEXTURE);

   public ExperienceOrbEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.shadowRadius = 0.15F;
      this.shadowOpacity = 0.75F;
   }

   protected int getBlockLight(ExperienceOrbEntity experienceOrbEntity, BlockPos blockPos) {
      return MathHelper.clamp(super.getBlockLight(experienceOrbEntity, blockPos) + 7, 0, 15);
   }

   public void render(
      ExperienceOrbEntityRenderState experienceOrbEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      matrixStack.push();
      int i = experienceOrbEntityRenderState.size;
      float f = (i % 4 * 16 + 0) / 64.0F;
      float g = (i % 4 * 16 + 16) / 64.0F;
      float h = (i / 4 * 16 + 0) / 64.0F;
      float j = (i / 4 * 16 + 16) / 64.0F;
      float k = 1.0F;
      float l = 0.5F;
      float m = 0.25F;
      float n = 255.0F;
      float o = experienceOrbEntityRenderState.age / 2.0F;
      int p = (int)((MathHelper.sin(o + 0.0F) + 1.0F) * 0.5F * 255.0F);
      int q = 255;
      int r = (int)((MathHelper.sin(o + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
      matrixStack.translate(0.0F, 0.1F, 0.0F);
      matrixStack.multiply(cameraRenderState.orientation);
      float s = 0.3F;
      matrixStack.scale(0.3F, 0.3F, 0.3F);
      orderedRenderCommandQueue.submitCustom(matrixStack, LAYER, (entry, vertexConsumer) -> {
         vertex(vertexConsumer, entry, -0.5F, -0.25F, p, 255, r, f, j, experienceOrbEntityRenderState.light);
         vertex(vertexConsumer, entry, 0.5F, -0.25F, p, 255, r, g, j, experienceOrbEntityRenderState.light);
         vertex(vertexConsumer, entry, 0.5F, 0.75F, p, 255, r, g, h, experienceOrbEntityRenderState.light);
         vertex(vertexConsumer, entry, -0.5F, 0.75F, p, 255, r, f, h, experienceOrbEntityRenderState.light);
      });
      matrixStack.pop();
      super.render(experienceOrbEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
   }

   private static void vertex(
      VertexConsumer vertexConsumer, MatrixStack.Entry matrix, float x, float y, int red, int green, int blue, float u, float v, int light
   ) {
      vertexConsumer.vertex(matrix, x, y, 0.0F)
         .color(red, green, blue, 128)
         .texture(u, v)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(light)
         .normal(matrix, 0.0F, 1.0F, 0.0F);
   }

   public ExperienceOrbEntityRenderState createRenderState() {
      return new ExperienceOrbEntityRenderState();
   }

   public void updateRenderState(ExperienceOrbEntity experienceOrbEntity, ExperienceOrbEntityRenderState experienceOrbEntityRenderState, float f) {
      super.updateRenderState(experienceOrbEntity, experienceOrbEntityRenderState, f);
      experienceOrbEntityRenderState.size = experienceOrbEntity.getOrbSize();
   }
}
