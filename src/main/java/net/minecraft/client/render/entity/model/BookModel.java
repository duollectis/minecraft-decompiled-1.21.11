package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class BookModel extends Model<BookModel.BookModelState> {
   private static final String LEFT_PAGES = "left_pages";
   private static final String RIGHT_PAGES = "right_pages";
   private static final String FLIP_PAGE1 = "flip_page1";
   private static final String FLIP_PAGE2 = "flip_page2";
   private final ModelPart leftCover;
   private final ModelPart rightCover;
   private final ModelPart leftPages;
   private final ModelPart rightPages;
   private final ModelPart leftFlippingPage;
   private final ModelPart rightFlippingPage;

   public BookModel(ModelPart root) {
      super(root, RenderLayers::entitySolid);
      this.leftCover = root.getChild("left_lid");
      this.rightCover = root.getChild("right_lid");
      this.leftPages = root.getChild("left_pages");
      this.rightPages = root.getChild("right_pages");
      this.leftFlippingPage = root.getChild("flip_page1");
      this.rightFlippingPage = root.getChild("flip_page2");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "left_lid", ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), ModelTransform.origin(0.0F, 0.0F, -1.0F)
      );
      modelPartData.addChild(
         "right_lid", ModelPartBuilder.create().uv(16, 0).cuboid(0.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), ModelTransform.origin(0.0F, 0.0F, 1.0F)
      );
      modelPartData.addChild(
         "seam",
         ModelPartBuilder.create().uv(12, 0).cuboid(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 0.005F),
         ModelTransform.rotation(0.0F, (float) (Math.PI / 2), 0.0F)
      );
      modelPartData.addChild("left_pages", ModelPartBuilder.create().uv(0, 10).cuboid(0.0F, -4.0F, -0.99F, 5.0F, 8.0F, 1.0F), ModelTransform.NONE);
      modelPartData.addChild("right_pages", ModelPartBuilder.create().uv(12, 10).cuboid(0.0F, -4.0F, -0.01F, 5.0F, 8.0F, 1.0F), ModelTransform.NONE);
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(24, 10).cuboid(0.0F, -4.0F, 0.0F, 5.0F, 8.0F, 0.005F);
      modelPartData.addChild("flip_page1", modelPartBuilder, ModelTransform.NONE);
      modelPartData.addChild("flip_page2", modelPartBuilder, ModelTransform.NONE);
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(BookModel.BookModelState bookModelState) {
      super.setAngles(bookModelState);
      float f = (MathHelper.sin(bookModelState.pageTurnAmount * 0.02F) * 0.1F + 1.25F) * bookModelState.pageTurnSpeed;
      this.leftCover.yaw = (float) Math.PI + f;
      this.rightCover.yaw = -f;
      this.leftPages.yaw = f;
      this.rightPages.yaw = -f;
      this.leftFlippingPage.yaw = f - f * 2.0F * bookModelState.leftFlipAmount;
      this.rightFlippingPage.yaw = f - f * 2.0F * bookModelState.rightFlipAmount;
      this.leftPages.originX = MathHelper.sin(f);
      this.rightPages.originX = MathHelper.sin(f);
      this.leftFlippingPage.originX = MathHelper.sin(f);
      this.rightFlippingPage.originX = MathHelper.sin(f);
   }

   @Environment(EnvType.CLIENT)
   public record BookModelState(float pageTurnAmount, float leftFlipAmount, float rightFlipAmount, float pageTurnSpeed) {
   }
}
