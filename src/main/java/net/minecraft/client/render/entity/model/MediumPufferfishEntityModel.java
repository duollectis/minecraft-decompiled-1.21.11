package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class MediumPufferfishEntityModel extends EntityModel<EntityRenderState> {
   private final ModelPart leftBlueFin;
   private final ModelPart rightBlueFin;

   public MediumPufferfishEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.leftBlueFin = modelPart.getChild("left_blue_fin");
      this.rightBlueFin = modelPart.getChild("right_blue_fin");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      int i = 22;
      modelPartData.addChild(
         "body", ModelPartBuilder.create().uv(12, 22).cuboid(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F), ModelTransform.origin(0.0F, 22.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_blue_fin", ModelPartBuilder.create().uv(24, 0).cuboid(-2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F), ModelTransform.origin(-2.5F, 18.0F, -1.5F)
      );
      modelPartData.addChild(
         "left_blue_fin", ModelPartBuilder.create().uv(24, 3).cuboid(0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 2.0F), ModelTransform.origin(2.5F, 18.0F, -1.5F)
      );
      modelPartData.addChild(
         "top_front_fin",
         ModelPartBuilder.create().uv(19, 17).cuboid(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         ModelTransform.of(0.0F, 17.0F, -2.5F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "top_back_fin",
         ModelPartBuilder.create().uv(11, 17).cuboid(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         ModelTransform.of(0.0F, 17.0F, 2.5F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_front_fin",
         ModelPartBuilder.create().uv(5, 17).cuboid(-1.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         ModelTransform.of(-2.5F, 22.0F, -2.5F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      modelPartData.addChild(
         "right_back_fin",
         ModelPartBuilder.create().uv(9, 17).cuboid(-1.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         ModelTransform.of(-2.5F, 22.0F, 2.5F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      modelPartData.addChild(
         "left_back_fin",
         ModelPartBuilder.create().uv(1, 17).cuboid(0.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         ModelTransform.of(2.5F, 22.0F, 2.5F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      modelPartData.addChild(
         "left_front_fin",
         ModelPartBuilder.create().uv(1, 17).cuboid(0.0F, -5.0F, 0.0F, 1.0F, 5.0F, 0.0F),
         ModelTransform.of(2.5F, 22.0F, -2.5F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      modelPartData.addChild(
         "bottom_back_fin",
         ModelPartBuilder.create().uv(18, 20).cuboid(0.0F, 0.0F, 0.0F, 5.0F, 1.0F, 0.0F),
         ModelTransform.of(-2.5F, 22.0F, 2.5F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "bottom_front_fin",
         ModelPartBuilder.create().uv(17, 19).cuboid(-2.5F, 0.0F, 0.0F, 5.0F, 1.0F, 1.0F),
         ModelTransform.of(0.0F, 22.0F, -2.5F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      return TexturedModelData.of(modelData, 32, 32);
   }

   public void setAngles(EntityRenderState entityRenderState) {
      super.setAngles(entityRenderState);
      this.rightBlueFin.roll = -0.2F + 0.4F * MathHelper.sin(entityRenderState.age * 0.2F);
      this.leftBlueFin.roll = 0.2F - 0.4F * MathHelper.sin(entityRenderState.age * 0.2F);
   }
}
