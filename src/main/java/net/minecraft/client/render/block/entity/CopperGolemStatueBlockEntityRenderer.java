package net.minecraft.client.render.block.entity;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.model.CopperGolemStatueModel;
import net.minecraft.client.render.block.entity.state.CopperGolemStatueBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CopperGolemStatueBlockEntityRenderer implements BlockEntityRenderer<CopperGolemStatueBlockEntity, CopperGolemStatueBlockEntityRenderState> {
   private final Map<CopperGolemStatueBlock.Pose, CopperGolemStatueModel> models = new HashMap<>();

   public CopperGolemStatueBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
      LoadedEntityModels loadedEntityModels = context.loadedEntityModels();
      this.models.put(CopperGolemStatueBlock.Pose.STANDING, new CopperGolemStatueModel(loadedEntityModels.getModelPart(EntityModelLayers.COPPER_GOLEM)));
      this.models.put(CopperGolemStatueBlock.Pose.RUNNING, new CopperGolemStatueModel(loadedEntityModels.getModelPart(EntityModelLayers.COPPER_GOLEM_RUNNING)));
      this.models.put(CopperGolemStatueBlock.Pose.SITTING, new CopperGolemStatueModel(loadedEntityModels.getModelPart(EntityModelLayers.COPPER_GOLEM_SITTING)));
      this.models.put(CopperGolemStatueBlock.Pose.STAR, new CopperGolemStatueModel(loadedEntityModels.getModelPart(EntityModelLayers.COPPER_GOLEM_STAR)));
   }

   public CopperGolemStatueBlockEntityRenderState createRenderState() {
      return new CopperGolemStatueBlockEntityRenderState();
   }

   public void updateRenderState(
      CopperGolemStatueBlockEntity copperGolemStatueBlockEntity,
      CopperGolemStatueBlockEntityRenderState copperGolemStatueBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(copperGolemStatueBlockEntity, copperGolemStatueBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      copperGolemStatueBlockEntityRenderState.facing = copperGolemStatueBlockEntity.getCachedState().get(CopperGolemStatueBlock.FACING);
      copperGolemStatueBlockEntityRenderState.pose = copperGolemStatueBlockEntity.getCachedState().get(Properties.COPPER_GOLEM_POSE);
   }

   public void render(
      CopperGolemStatueBlockEntityRenderState copperGolemStatueBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      if (copperGolemStatueBlockEntityRenderState.blockState.getBlock() instanceof CopperGolemStatueBlock copperGolemStatueBlock) {
         matrixStack.push();
         matrixStack.translate(0.5F, 0.0F, 0.5F);
         CopperGolemStatueModel copperGolemStatueModel = this.models.get(copperGolemStatueBlockEntityRenderState.pose);
         Direction direction = copperGolemStatueBlockEntityRenderState.facing;
         RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(CopperGolemOxidationLevels.get(copperGolemStatueBlock.getOxidationLevel()).texture());
         orderedRenderCommandQueue.submitModel(
            copperGolemStatueModel,
            direction,
            matrixStack,
            renderLayer,
            copperGolemStatueBlockEntityRenderState.lightmapCoordinates,
            OverlayTexture.DEFAULT_UV,
            0,
            copperGolemStatueBlockEntityRenderState.crumblingOverlay
         );
         matrixStack.pop();
      }
   }
}
