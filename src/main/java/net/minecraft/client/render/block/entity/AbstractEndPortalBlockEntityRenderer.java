package net.minecraft.client.render.block.entity;

import java.util.EnumSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.state.EndPortalBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractEndPortalBlockEntityRenderer<T extends EndPortalBlockEntity, S extends EndPortalBlockEntityRenderState>
   implements BlockEntityRenderer<T, S> {
   public static final Identifier SKY_TEXTURE = Identifier.ofVanilla("textures/environment/end_sky.png");
   public static final Identifier PORTAL_TEXTURE = Identifier.ofVanilla("textures/entity/end_portal.png");

   public void updateRenderState(
      T endPortalBlockEntity,
      S endPortalBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(endPortalBlockEntity, endPortalBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      endPortalBlockEntityRenderState.sides.clear();

      for (Direction direction : Direction.values()) {
         if (endPortalBlockEntity.shouldDrawSide(direction)) {
            endPortalBlockEntityRenderState.sides.add(direction);
         }
      }
   }

   public void render(
      S endPortalBlockEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState
   ) {
      orderedRenderCommandQueue.submitCustom(
         matrixStack,
         this.getLayer(),
         (matricesEntry, vertexConsumer) -> this.renderSides(endPortalBlockEntityRenderState.sides, matricesEntry.getPositionMatrix(), vertexConsumer)
      );
   }

   private void renderSides(EnumSet<Direction> sides, Matrix4f matrix, VertexConsumer vertexConsumer) {
      float f = this.getBottomYOffset();
      float g = this.getTopYOffset();
      this.renderSide(sides, matrix, vertexConsumer, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderSide(sides, matrix, vertexConsumer, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderSide(sides, matrix, vertexConsumer, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderSide(sides, matrix, vertexConsumer, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderSide(sides, matrix, vertexConsumer, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderSide(sides, matrix, vertexConsumer, 0.0F, 1.0F, g, g, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderSide(
      EnumSet<Direction> sides,
      Matrix4f model,
      VertexConsumer vertices,
      float x1,
      float x2,
      float y1,
      float y2,
      float z1,
      float z2,
      float z3,
      float z4,
      Direction side
   ) {
      if (sides.contains(side)) {
         vertices.vertex(model, x1, y1, z1);
         vertices.vertex(model, x2, y1, z2);
         vertices.vertex(model, x2, y2, z3);
         vertices.vertex(model, x1, y2, z4);
      }
   }

   protected float getTopYOffset() {
      return 0.75F;
   }

   protected float getBottomYOffset() {
      return 0.375F;
   }

   protected RenderLayer getLayer() {
      return RenderLayers.endPortal();
   }
}
