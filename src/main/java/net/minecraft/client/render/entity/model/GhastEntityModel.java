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
import net.minecraft.client.render.entity.state.GhastEntityRenderState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class GhastEntityModel extends EntityModel<GhastEntityRenderState> {
   private final ModelPart[] tentacles = new ModelPart[9];

   public GhastEntityModel(ModelPart modelPart) {
      super(modelPart);

      for (int i = 0; i < this.tentacles.length; i++) {
         this.tentacles[i] = modelPart.getChild(EntityModelPartNames.getTentacleName(i));
      }
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "body", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), ModelTransform.origin(0.0F, 17.6F, 0.0F)
      );
      Random random = Random.create(1660L);

      for (int i = 0; i < 9; i++) {
         float f = ((i % 3 - i / 3 % 2 * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
         float g = (i / 3 / 2.0F * 2.0F - 1.0F) * 5.0F;
         int j = random.nextInt(7) + 8;
         modelPartData.addChild(
            EntityModelPartNames.getTentacleName(i),
            ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, j, 2.0F),
            ModelTransform.origin(f, 24.6F, g)
         );
      }

      return TexturedModelData.of(modelData, 64, 32).transform(ModelTransformer.scaling(4.5F));
   }

   public void setAngles(GhastEntityRenderState ghastEntityRenderState) {
      super.setAngles(ghastEntityRenderState);
      setTentacleAngles(ghastEntityRenderState, this.tentacles);
   }

   public static void setTentacleAngles(EntityRenderState state, ModelPart[] tentacles) {
      for (int i = 0; i < tentacles.length; i++) {
         tentacles[i].pitch = 0.2F * MathHelper.sin(state.age * 0.3F + i) + 0.4F;
      }
   }
}
