package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.state.EvokerFangsEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class EvokerFangsEntityModel extends EntityModel<EvokerFangsEntityRenderState> {
   private static final String BASE = "base";
   private static final String UPPER_JAW = "upper_jaw";
   private static final String LOWER_JAW = "lower_jaw";
   private final ModelPart base;
   private final ModelPart upperJaw;
   private final ModelPart lowerJaw;

   public EvokerFangsEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.base = modelPart.getChild("base");
      this.upperJaw = this.base.getChild("upper_jaw");
      this.lowerJaw = this.base.getChild("lower_jaw");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "base", ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, 0.0F, 10.0F, 12.0F, 10.0F), ModelTransform.origin(-5.0F, 24.0F, -5.0F)
      );
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(40, 0).cuboid(0.0F, 0.0F, 0.0F, 4.0F, 14.0F, 8.0F);
      modelPartData2.addChild("upper_jaw", modelPartBuilder, ModelTransform.of(6.5F, 0.0F, 1.0F, 0.0F, 0.0F, 2.042035F));
      modelPartData2.addChild("lower_jaw", modelPartBuilder, ModelTransform.of(3.5F, 0.0F, 9.0F, 0.0F, (float) Math.PI, 4.2411504F));
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(EvokerFangsEntityRenderState evokerFangsEntityRenderState) {
      super.setAngles(evokerFangsEntityRenderState);
      float f = evokerFangsEntityRenderState.animationProgress;
      float g = Math.min(f * 2.0F, 1.0F);
      g = 1.0F - g * g * g;
      this.upperJaw.roll = (float) Math.PI - g * 0.35F * (float) Math.PI;
      this.lowerJaw.roll = (float) Math.PI + g * 0.35F * (float) Math.PI;
      this.base.originY = this.base.originY - (f + MathHelper.sin(f * 2.7F)) * 7.2F;
      float h = 1.0F;
      if (f > 0.9F) {
         h *= (1.0F - f) / 0.1F;
      }

      this.root.originY = 24.0F - 20.0F * h;
      this.root.xScale = h;
      this.root.yScale = h;
      this.root.zScale = h;
   }
}
