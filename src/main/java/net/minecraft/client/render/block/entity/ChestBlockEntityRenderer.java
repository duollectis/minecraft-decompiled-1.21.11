package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CopperChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.ChestBlockModel;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Holidays;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChestBlockEntityRenderer<T extends BlockEntity & LidOpenable> implements BlockEntityRenderer<T, ChestBlockEntityRenderState> {
   private final SpriteHolder materials;
   private final ChestBlockModel singleChest;
   private final ChestBlockModel doubleChestLeft;
   private final ChestBlockModel doubleChestRight;
   private final boolean christmas;

   public ChestBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
      this.materials = context.spriteHolder();
      this.christmas = isAroundChristmas();
      this.singleChest = new ChestBlockModel(context.getLayerModelPart(EntityModelLayers.CHEST));
      this.doubleChestLeft = new ChestBlockModel(context.getLayerModelPart(EntityModelLayers.DOUBLE_CHEST_LEFT));
      this.doubleChestRight = new ChestBlockModel(context.getLayerModelPart(EntityModelLayers.DOUBLE_CHEST_RIGHT));
   }

   public static boolean isAroundChristmas() {
      return Holidays.isAroundChristmas();
   }

   public ChestBlockEntityRenderState createRenderState() {
      return new ChestBlockEntityRenderState();
   }

   public void updateRenderState(
      T blockEntity,
      ChestBlockEntityRenderState chestBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(blockEntity, chestBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      boolean bl = blockEntity.getWorld() != null;
      BlockState blockState = bl ? blockEntity.getCachedState() : Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.SOUTH);
      chestBlockEntityRenderState.chestType = blockState.contains(ChestBlock.CHEST_TYPE) ? blockState.get(ChestBlock.CHEST_TYPE) : ChestType.SINGLE;
      chestBlockEntityRenderState.yaw = blockState.get(ChestBlock.FACING).getPositiveHorizontalDegrees();
      chestBlockEntityRenderState.variant = this.getVariant(blockEntity, this.christmas);
      DoubleBlockProperties.PropertySource<? extends ChestBlockEntity> propertySource;
      if (bl && blockState.getBlock() instanceof ChestBlock chestBlock) {
         propertySource = chestBlock.getBlockEntitySource(blockState, blockEntity.getWorld(), blockEntity.getPos(), true);
      } else {
         propertySource = DoubleBlockProperties.PropertyRetriever::getFallback;
      }

      chestBlockEntityRenderState.lidAnimationProgress = propertySource.apply(ChestBlock.getAnimationProgressRetriever(blockEntity)).get(f);
      if (chestBlockEntityRenderState.chestType != ChestType.SINGLE) {
         chestBlockEntityRenderState.lightmapCoordinates = propertySource.apply(new LightmapCoordinatesRetriever<>())
            .applyAsInt(chestBlockEntityRenderState.lightmapCoordinates);
      }
   }

   public void render(
      ChestBlockEntityRenderState chestBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      matrixStack.push();
      matrixStack.translate(0.5F, 0.5F, 0.5F);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-chestBlockEntityRenderState.yaw));
      matrixStack.translate(-0.5F, -0.5F, -0.5F);
      float f = chestBlockEntityRenderState.lidAnimationProgress;
      f = 1.0F - f;
      f = 1.0F - f * f * f;
      SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getChestTextureId(chestBlockEntityRenderState.variant, chestBlockEntityRenderState.chestType);
      RenderLayer renderLayer = spriteIdentifier.getRenderLayer(RenderLayers::entityCutout);
      Sprite sprite = this.materials.getSprite(spriteIdentifier);
      if (chestBlockEntityRenderState.chestType != ChestType.SINGLE) {
         if (chestBlockEntityRenderState.chestType == ChestType.LEFT) {
            orderedRenderCommandQueue.submitModel(
               this.doubleChestLeft,
               f,
               matrixStack,
               renderLayer,
               chestBlockEntityRenderState.lightmapCoordinates,
               OverlayTexture.DEFAULT_UV,
               -1,
               sprite,
               0,
               chestBlockEntityRenderState.crumblingOverlay
            );
         } else {
            orderedRenderCommandQueue.submitModel(
               this.doubleChestRight,
               f,
               matrixStack,
               renderLayer,
               chestBlockEntityRenderState.lightmapCoordinates,
               OverlayTexture.DEFAULT_UV,
               -1,
               sprite,
               0,
               chestBlockEntityRenderState.crumblingOverlay
            );
         }
      } else {
         orderedRenderCommandQueue.submitModel(
            this.singleChest,
            f,
            matrixStack,
            renderLayer,
            chestBlockEntityRenderState.lightmapCoordinates,
            OverlayTexture.DEFAULT_UV,
            -1,
            sprite,
            0,
            chestBlockEntityRenderState.crumblingOverlay
         );
      }

      matrixStack.pop();
   }

   private ChestBlockEntityRenderState.Variant getVariant(BlockEntity blockEntity, boolean christmas) {
      if (blockEntity instanceof EnderChestBlockEntity) {
         return ChestBlockEntityRenderState.Variant.ENDER_CHEST;
      } else if (christmas) {
         return ChestBlockEntityRenderState.Variant.CHRISTMAS;
      } else if (blockEntity instanceof TrappedChestBlockEntity) {
         return ChestBlockEntityRenderState.Variant.TRAPPED;
      } else if (blockEntity.getCachedState().getBlock() instanceof CopperChestBlock copperChestBlock) {
         return switch (copperChestBlock.getOxidationLevel()) {
            case UNAFFECTED -> ChestBlockEntityRenderState.Variant.COPPER_UNAFFECTED;
            case EXPOSED -> ChestBlockEntityRenderState.Variant.COPPER_EXPOSED;
            case WEATHERED -> ChestBlockEntityRenderState.Variant.COPPER_WEATHERED;
            case OXIDIZED -> ChestBlockEntityRenderState.Variant.COPPER_OXIDIZED;
         };
      } else {
         return ChestBlockEntityRenderState.Variant.REGULAR;
      }
   }
}
