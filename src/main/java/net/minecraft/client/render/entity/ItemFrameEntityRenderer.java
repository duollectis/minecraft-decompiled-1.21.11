package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.render.model.BlockStateManagers;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ItemFrameEntityRenderer<T extends ItemFrameEntity> extends EntityRenderer<T, ItemFrameEntityRenderState> {
   public static final int GLOW_FRAME_BLOCK_LIGHT = 5;
   public static final int field_32933 = 30;
   private final ItemModelManager itemModelManager;
   private final MapRenderer mapRenderer;
   private final BlockRenderManager blockRenderManager;

   public ItemFrameEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.itemModelManager = context.getItemModelManager();
      this.mapRenderer = context.getMapRenderer();
      this.blockRenderManager = context.getBlockRenderManager();
   }

   protected int getBlockLight(T itemFrameEntity, BlockPos blockPos) {
      return itemFrameEntity.getType() == EntityType.GLOW_ITEM_FRAME
         ? Math.max(5, super.getBlockLight(itemFrameEntity, blockPos))
         : super.getBlockLight(itemFrameEntity, blockPos);
   }

   public void render(
      ItemFrameEntityRenderState itemFrameEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      super.render(itemFrameEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
      matrixStack.push();
      Direction direction = itemFrameEntityRenderState.facing;
      Vec3d vec3d = this.getPositionOffset(itemFrameEntityRenderState);
      matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
      double d = 0.46875;
      matrixStack.translate(direction.getOffsetX() * 0.46875, direction.getOffsetY() * 0.46875, direction.getOffsetZ() * 0.46875);
      float f;
      float g;
      if (direction.getAxis().isHorizontal()) {
         f = 0.0F;
         g = 180.0F - direction.getPositiveHorizontalDegrees();
      } else {
         f = -90 * direction.getDirection().offset();
         g = 180.0F;
      }

      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
      if (!itemFrameEntityRenderState.invisible) {
         BlockState blockState = BlockStateManagers.getStateForItemFrame(itemFrameEntityRenderState.glow, itemFrameEntityRenderState.mapId != null);
         BlockStateModel blockStateModel = this.blockRenderManager.getModel(blockState);
         matrixStack.push();
         matrixStack.translate(-0.5F, -0.5F, -0.5F);
         orderedRenderCommandQueue.submitBlockStateModel(
            matrixStack,
            RenderLayers.entitySolidZOffsetForward(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE),
            blockStateModel,
            1.0F,
            1.0F,
            1.0F,
            itemFrameEntityRenderState.light,
            OverlayTexture.DEFAULT_UV,
            itemFrameEntityRenderState.outlineColor
         );
         matrixStack.pop();
      }

      if (itemFrameEntityRenderState.invisible) {
         matrixStack.translate(0.0F, 0.0F, 0.5F);
      } else {
         matrixStack.translate(0.0F, 0.0F, 0.4375F);
      }

      if (itemFrameEntityRenderState.mapId != null) {
         int i = itemFrameEntityRenderState.rotation % 4 * 2;
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 360.0F / 8.0F));
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         float h = 0.0078125F;
         matrixStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
         matrixStack.translate(-64.0F, -64.0F, 0.0F);
         matrixStack.translate(0.0F, 0.0F, -1.0F);
         int j = this.getLight(itemFrameEntityRenderState.glow, 15728850, itemFrameEntityRenderState.light);
         this.mapRenderer.draw(itemFrameEntityRenderState.mapRenderState, matrixStack, orderedRenderCommandQueue, true, j);
      } else if (!itemFrameEntityRenderState.itemRenderState.isEmpty()) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(itemFrameEntityRenderState.rotation * 360.0F / 8.0F));
         int i = this.getLight(itemFrameEntityRenderState.glow, 15728880, itemFrameEntityRenderState.light);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         itemFrameEntityRenderState.itemRenderState
            .render(matrixStack, orderedRenderCommandQueue, i, OverlayTexture.DEFAULT_UV, itemFrameEntityRenderState.outlineColor);
      }

      matrixStack.pop();
   }

   private int getLight(boolean glow, int glowLight, int regularLight) {
      return glow ? glowLight : regularLight;
   }

   public Vec3d getPositionOffset(ItemFrameEntityRenderState itemFrameEntityRenderState) {
      return new Vec3d(itemFrameEntityRenderState.facing.getOffsetX() * 0.3F, -0.25, itemFrameEntityRenderState.facing.getOffsetZ() * 0.3F);
   }

   protected boolean hasLabel(T itemFrameEntity, double d) {
      return MinecraftClient.isHudEnabled() && this.dispatcher.targetedEntity == itemFrameEntity && itemFrameEntity.getHeldItemStack().getCustomName() != null;
   }

   protected Text getDisplayName(T itemFrameEntity) {
      return itemFrameEntity.getHeldItemStack().getName();
   }

   public ItemFrameEntityRenderState createRenderState() {
      return new ItemFrameEntityRenderState();
   }

   public void updateRenderState(T itemFrameEntity, ItemFrameEntityRenderState itemFrameEntityRenderState, float f) {
      super.updateRenderState(itemFrameEntity, itemFrameEntityRenderState, f);
      itemFrameEntityRenderState.facing = itemFrameEntity.getHorizontalFacing();
      ItemStack itemStack = itemFrameEntity.getHeldItemStack();
      this.itemModelManager.updateForNonLivingEntity(itemFrameEntityRenderState.itemRenderState, itemStack, ItemDisplayContext.FIXED, itemFrameEntity);
      itemFrameEntityRenderState.rotation = itemFrameEntity.getRotation();
      itemFrameEntityRenderState.glow = itemFrameEntity.getType() == EntityType.GLOW_ITEM_FRAME;
      itemFrameEntityRenderState.mapId = null;
      if (!itemStack.isEmpty()) {
         MapIdComponent mapIdComponent = itemFrameEntity.getMapId(itemStack);
         if (mapIdComponent != null) {
            MapState mapState = itemFrameEntity.getEntityWorld().getMapState(mapIdComponent);
            if (mapState != null) {
               this.mapRenderer.update(mapIdComponent, mapState, itemFrameEntityRenderState.mapRenderState);
               itemFrameEntityRenderState.mapId = mapIdComponent;
            }
         }
      }
   }
}
