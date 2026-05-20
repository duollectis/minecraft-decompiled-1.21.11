package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.state.EnchantingTableBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EnchantingTableBlockEntityRenderer implements BlockEntityRenderer<EnchantingTableBlockEntity, EnchantingTableBlockEntityRenderState> {
   public static final SpriteIdentifier BOOK_TEXTURE = TexturedRenderLayers.ENTITY_SPRITE_MAPPER.mapVanilla("enchanting_table_book");
   private final SpriteHolder spriteHolder;
   private final BookModel book;

   public EnchantingTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
      this.spriteHolder = ctx.spriteHolder();
      this.book = new BookModel(ctx.getLayerModelPart(EntityModelLayers.BOOK));
   }

   public EnchantingTableBlockEntityRenderState createRenderState() {
      return new EnchantingTableBlockEntityRenderState();
   }

   public void updateRenderState(
      EnchantingTableBlockEntity enchantingTableBlockEntity,
      EnchantingTableBlockEntityRenderState enchantingTableBlockEntityRenderState,
      float f,
      Vec3d vec3d,
      ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
   ) {
      BlockEntityRenderer.super.updateRenderState(enchantingTableBlockEntity, enchantingTableBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
      enchantingTableBlockEntityRenderState.pageAngle = MathHelper.lerp(f, enchantingTableBlockEntity.pageAngle, enchantingTableBlockEntity.nextPageAngle);
      enchantingTableBlockEntityRenderState.pageTurningSpeed = MathHelper.lerp(
         f, enchantingTableBlockEntity.pageTurningSpeed, enchantingTableBlockEntity.nextPageTurningSpeed
      );
      enchantingTableBlockEntityRenderState.ticks = enchantingTableBlockEntity.ticks + f;
      float g = enchantingTableBlockEntity.bookRotation - enchantingTableBlockEntity.lastBookRotation;

      while (g >= (float) Math.PI) {
         g -= (float) (Math.PI * 2);
      }

      while (g < (float) -Math.PI) {
         g += (float) (Math.PI * 2);
      }

      enchantingTableBlockEntityRenderState.bookRotationDegrees = enchantingTableBlockEntity.lastBookRotation + g * f;
   }

   public void render(
      EnchantingTableBlockEntityRenderState enchantingTableBlockEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      matrixStack.push();
      matrixStack.translate(0.5F, 0.75F, 0.5F);
      matrixStack.translate(0.0F, 0.1F + MathHelper.sin(enchantingTableBlockEntityRenderState.ticks * 0.1F) * 0.01F, 0.0F);
      float f = enchantingTableBlockEntityRenderState.bookRotationDegrees;
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(-f));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(80.0F));
      float g = MathHelper.fractionalPart(enchantingTableBlockEntityRenderState.pageAngle + 0.25F) * 1.6F - 0.3F;
      float h = MathHelper.fractionalPart(enchantingTableBlockEntityRenderState.pageAngle + 0.75F) * 1.6F - 0.3F;
      BookModel.BookModelState bookModelState = new BookModel.BookModelState(
         enchantingTableBlockEntityRenderState.ticks,
         MathHelper.clamp(g, 0.0F, 1.0F),
         MathHelper.clamp(h, 0.0F, 1.0F),
         enchantingTableBlockEntityRenderState.pageTurningSpeed
      );
      orderedRenderCommandQueue.submitModel(
         this.book,
         bookModelState,
         matrixStack,
         BOOK_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
         enchantingTableBlockEntityRenderState.lightmapCoordinates,
         OverlayTexture.DEFAULT_UV,
         -1,
         this.spriteHolder.getSprite(BOOK_TEXTURE),
         0,
         enchantingTableBlockEntityRenderState.crumblingOverlay
      );
      matrixStack.pop();
   }
}
