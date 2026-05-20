package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.NautilusArmorEntityModel;
import net.minecraft.client.render.entity.model.NautilusEntityModel;
import net.minecraft.client.render.entity.model.NautilusSaddleEntityModel;
import net.minecraft.client.render.entity.state.NautilusEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NautilusEntityRenderer<T extends AbstractNautilusEntity> extends AgeableMobEntityRenderer<T, NautilusEntityRenderState, NautilusEntityModel> {
   private static final Identifier field_64455 = Identifier.ofVanilla("textures/entity/nautilus/nautilus.png");
   private static final Identifier field_64456 = Identifier.ofVanilla("textures/entity/nautilus/nautilus_baby.png");

   public NautilusEntityRenderer(EntityRendererFactory.Context context) {
      super(
         context,
         new NautilusEntityModel(context.getPart(EntityModelLayers.NAUTILUS)),
         new NautilusEntityModel(context.getPart(EntityModelLayers.NAUTILUS_BABY)),
         0.7F
      );
      this.addFeature(
         new SaddleFeatureRenderer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentModel.LayerType.NAUTILUS_BODY,
            state -> state.armorStack,
            new NautilusArmorEntityModel(context.getPart(EntityModelLayers.NAUTILUS_ARMOR)),
            null
         )
      );
      this.addFeature(
         new SaddleFeatureRenderer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentModel.LayerType.NAUTILUS_SADDLE,
            state -> state.saddleStack,
            new NautilusSaddleEntityModel(context.getPart(EntityModelLayers.NAUTILUS_SADDLE)),
            null
         )
      );
   }

   public Identifier getTexture(NautilusEntityRenderState nautilusEntityRenderState) {
      return nautilusEntityRenderState.baby ? field_64456 : field_64455;
   }

   public NautilusEntityRenderState createRenderState() {
      return new NautilusEntityRenderState();
   }

   public void updateRenderState(T abstractNautilusEntity, NautilusEntityRenderState nautilusEntityRenderState, float f) {
      super.updateRenderState(abstractNautilusEntity, nautilusEntityRenderState, f);
      nautilusEntityRenderState.saddleStack = abstractNautilusEntity.getEquippedStack(EquipmentSlot.SADDLE).copy();
      nautilusEntityRenderState.armorStack = abstractNautilusEntity.getBodyArmor().copy();
   }
}
