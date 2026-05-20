package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.block.spawner.TrialSpawnerData;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TrialSpawnerBlockEntityRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity, MobSpawnerBlockEntityRenderState> {
   private final EntityRenderManager entityRenderDispatcher;

   public TrialSpawnerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
      this.entityRenderDispatcher = context.entityRenderDispatcher();
   }

   public MobSpawnerBlockEntityRenderState createRenderState() {
      return new MobSpawnerBlockEntityRenderState();
   }

   public void updateRenderState(
      TrialSpawnerBlockEntity trialSpawnerBlockEntity,
      MobSpawnerBlockEntityRenderState mobSpawnerBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(trialSpawnerBlockEntity, mobSpawnerBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      if (trialSpawnerBlockEntity.getWorld() != null) {
         TrialSpawnerLogic trialSpawnerLogic = trialSpawnerBlockEntity.getSpawner();
         TrialSpawnerData trialSpawnerData = trialSpawnerLogic.getData();
         Entity entity = trialSpawnerData.setDisplayEntity(trialSpawnerLogic, trialSpawnerBlockEntity.getWorld(), trialSpawnerLogic.getSpawnerState());
         updateSpawnerRenderState(
            mobSpawnerBlockEntityRenderState,
            f,
            entity,
            this.entityRenderDispatcher,
            trialSpawnerData.getLastDisplayEntityRotation(),
            trialSpawnerData.getDisplayEntityRotation()
         );
      }
   }

   static void updateSpawnerRenderState(
      MobSpawnerBlockEntityRenderState state,
      float tickProgress,
      @Nullable Entity displayEntity,
      EntityRenderManager entityRenderDispatcher,
      double lastDisplayEntityRotation,
      double displayEntityRotation
   ) {
      if (displayEntity != null) {
         state.displayEntityRenderState = entityRenderDispatcher.getAndUpdateRenderState(displayEntity, tickProgress);
         state.displayEntityRenderState.light = state.lightmapCoordinates;
         state.displayEntityRotation = (float)MathHelper.lerp((double)tickProgress, lastDisplayEntityRotation, displayEntityRotation) * 10.0F;
         state.displayEntityScale = 0.53125F;
         float f = Math.max(displayEntity.getWidth(), displayEntity.getHeight());
         if (f > 1.0) {
            state.displayEntityScale /= f;
         }
      }
   }

   public void render(
      MobSpawnerBlockEntityRenderState mobSpawnerBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      if (mobSpawnerBlockEntityRenderState.displayEntityRenderState != null) {
         MobSpawnerBlockEntityRenderer.renderDisplayEntity(
            matrixStack,
            orderedRenderCommandQueue,
            mobSpawnerBlockEntityRenderState.displayEntityRenderState,
            this.entityRenderDispatcher,
            mobSpawnerBlockEntityRenderState.displayEntityRotation,
            mobSpawnerBlockEntityRenderState.displayEntityScale,
            cameraRenderState
         );
      }
   }
}
