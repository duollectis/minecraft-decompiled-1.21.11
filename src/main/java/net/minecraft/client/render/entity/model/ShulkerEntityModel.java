package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.ShulkerEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ShulkerEntityModel extends EntityModel<ShulkerEntityRenderState> {
   public static final String LID = "lid";
   private static final String BASE = "base";
   private final ModelPart lid;
   private final ModelPart head;

   public ShulkerEntityModel(ModelPart modelPart) {
      super(modelPart, RenderLayers::entityCutoutNoCullZOffset);
      this.lid = modelPart.getChild("lid");
      this.head = modelPart.getChild("head");
   }

   private static ModelData getModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "lid", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F), ModelTransform.origin(0.0F, 24.0F, 0.0F)
      );
      modelPartData.addChild(
         "base", ModelPartBuilder.create().uv(0, 28).cuboid(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), ModelTransform.origin(0.0F, 24.0F, 0.0F)
      );
      return modelData;
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = getModelData();
      modelData.getRoot()
         .addChild("head", ModelPartBuilder.create().uv(0, 52).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F), ModelTransform.origin(0.0F, 12.0F, 0.0F));
      return TexturedModelData.of(modelData, 64, 64);
   }

   public static TexturedModelData getShulkerBoxTexturedModelData() {
      ModelData modelData = getModelData();
      return TexturedModelData.of(modelData, 64, 64);
   }

   public void setAngles(ShulkerEntityRenderState shulkerEntityRenderState) {
      super.setAngles(shulkerEntityRenderState);
      float f = (0.5F + shulkerEntityRenderState.openProgress) * (float) Math.PI;
      float g = -1.0F + MathHelper.sin(f);
      float h = 0.0F;
      if (f > (float) Math.PI) {
         h = MathHelper.sin(shulkerEntityRenderState.age * 0.1F) * 0.7F;
      }

      this.lid.setOrigin(0.0F, 16.0F + MathHelper.sin(f) * 8.0F + h, 0.0F);
      if (shulkerEntityRenderState.openProgress > 0.3F) {
         this.lid.yaw = g * g * g * g * (float) Math.PI * 0.125F;
      } else {
         this.lid.yaw = 0.0F;
      }

      this.head.pitch = shulkerEntityRenderState.pitch * (float) (Math.PI / 180.0);
      this.head.yaw = (shulkerEntityRenderState.headYaw - 180.0F - shulkerEntityRenderState.shellYaw) * (float) (Math.PI / 180.0);
   }
}
