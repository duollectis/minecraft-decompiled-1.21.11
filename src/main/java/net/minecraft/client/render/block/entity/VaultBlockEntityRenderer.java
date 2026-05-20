package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.vault.VaultClientData;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.block.entity.state.VaultBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class VaultBlockEntityRenderer implements BlockEntityRenderer<VaultBlockEntity, VaultBlockEntityRenderState> {
   private final ItemModelManager itemModelManager;
   private final Random random = Random.create();

   public VaultBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
      this.itemModelManager = context.itemModelManager();
   }

   public VaultBlockEntityRenderState createRenderState() {
      return new VaultBlockEntityRenderState();
   }

   public void updateRenderState(
      VaultBlockEntity vaultBlockEntity,
      VaultBlockEntityRenderState vaultBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(vaultBlockEntity, vaultBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      ItemStack itemStack = vaultBlockEntity.getSharedData().getDisplayItem();
      if (VaultBlockEntity.Client.hasDisplayItem(vaultBlockEntity.getSharedData()) && !itemStack.isEmpty() && vaultBlockEntity.getWorld() != null) {
         vaultBlockEntityRenderState.displayItemStackState = new ItemStackEntityRenderState();
         this.itemModelManager
            .clearAndUpdate(
               vaultBlockEntityRenderState.displayItemStackState.itemRenderState, itemStack, ItemDisplayContext.GROUND, vaultBlockEntity.getWorld(), null, 0
            );
         vaultBlockEntityRenderState.displayItemStackState.renderedAmount = ItemStackEntityRenderState.getRenderedAmount(itemStack.getCount());
         vaultBlockEntityRenderState.displayItemStackState.seed = ItemStackEntityRenderState.getSeed(itemStack);
         VaultClientData vaultClientData = vaultBlockEntity.getClientData();
         vaultBlockEntityRenderState.displayRotationDegrees = MathHelper.lerpAngleDegrees(
            f, vaultClientData.getLastDisplayRotation(), vaultClientData.getDisplayRotation()
         );
      }
   }

   public void render(
      VaultBlockEntityRenderState vaultBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      if (vaultBlockEntityRenderState.displayItemStackState != null) {
         matrixStack.push();
         matrixStack.translate(0.5F, 0.4F, 0.5F);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(vaultBlockEntityRenderState.displayRotationDegrees));
         ItemEntityRenderer.renderStack(
            matrixStack,
            orderedRenderCommandQueue,
            vaultBlockEntityRenderState.lightmapCoordinates,
            vaultBlockEntityRenderState.displayItemStackState,
            this.random
         );
         matrixStack.pop();
      }
   }
}
