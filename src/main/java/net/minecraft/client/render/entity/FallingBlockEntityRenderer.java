package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class FallingBlockEntityRenderer extends EntityRenderer<FallingBlockEntity, FallingBlockEntityRenderState> {
   public FallingBlockEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.shadowRadius = 0.5F;
   }

   public boolean shouldRender(FallingBlockEntity fallingBlockEntity, Frustum frustum, double d, double e, double f) {
      return !super.shouldRender(fallingBlockEntity, frustum, d, e, f)
         ? false
         : fallingBlockEntity.getBlockState() != fallingBlockEntity.getEntityWorld().getBlockState(fallingBlockEntity.getBlockPos());
   }

   public void render(
      FallingBlockEntityRenderState fallingBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      BlockState blockState = fallingBlockEntityRenderState.movingBlockRenderState.blockState;
      if (blockState.getRenderType() == BlockRenderType.MODEL) {
         matrixStack.push();
         matrixStack.translate(-0.5, 0.0, -0.5);
         orderedRenderCommandQueue.submitMovingBlock(matrixStack, fallingBlockEntityRenderState.movingBlockRenderState);
         matrixStack.pop();
         super.render(fallingBlockEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
      }
   }

   public FallingBlockEntityRenderState createRenderState() {
      return new FallingBlockEntityRenderState();
   }

   public void updateRenderState(FallingBlockEntity fallingBlockEntity, FallingBlockEntityRenderState fallingBlockEntityRenderState, float f) {
      super.updateRenderState(fallingBlockEntity, fallingBlockEntityRenderState, f);
      BlockPos blockPos = BlockPos.ofFloored(fallingBlockEntity.getX(), fallingBlockEntity.getBoundingBox().maxY, fallingBlockEntity.getZ());
      fallingBlockEntityRenderState.movingBlockRenderState.fallingBlockPos = fallingBlockEntity.getFallingBlockPos();
      fallingBlockEntityRenderState.movingBlockRenderState.entityBlockPos = blockPos;
      fallingBlockEntityRenderState.movingBlockRenderState.blockState = fallingBlockEntity.getBlockState();
      fallingBlockEntityRenderState.movingBlockRenderState.biome = fallingBlockEntity.getEntityWorld().getBiome(blockPos);
      fallingBlockEntityRenderState.movingBlockRenderState.world = fallingBlockEntity.getEntityWorld();
   }
}
