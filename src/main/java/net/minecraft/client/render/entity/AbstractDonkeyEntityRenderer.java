package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.DonkeyEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HorseSaddleEntityModel;
import net.minecraft.client.render.entity.state.DonkeyEntityRenderState;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AbstractDonkeyEntityRenderer<T extends AbstractDonkeyEntity> extends AbstractHorseEntityRenderer<T, DonkeyEntityRenderState, DonkeyEntityModel> {
   private final Identifier texture;

   public AbstractDonkeyEntityRenderer(EntityRendererFactory.Context context, AbstractDonkeyEntityRenderer.Type type) {
      super(context, new DonkeyEntityModel(context.getPart(type.adultModelLayer)), new DonkeyEntityModel(context.getPart(type.babyModelLayer)));
      this.texture = type.texture;
      this.addFeature(
         new SaddleFeatureRenderer<>(
            this,
            context.getEquipmentRenderer(),
            type.saddleLayerType,
            state -> state.saddleStack,
            new HorseSaddleEntityModel(context.getPart(type.adultSaddleModelLayer)),
            new HorseSaddleEntityModel(context.getPart(type.babySaddleModelLayer))
         )
      );
   }

   public Identifier getTexture(DonkeyEntityRenderState donkeyEntityRenderState) {
      return this.texture;
   }

   public DonkeyEntityRenderState createRenderState() {
      return new DonkeyEntityRenderState();
   }

   public void updateRenderState(T abstractDonkeyEntity, DonkeyEntityRenderState donkeyEntityRenderState, float f) {
      super.updateRenderState(abstractDonkeyEntity, donkeyEntityRenderState, f);
      donkeyEntityRenderState.hasChest = abstractDonkeyEntity.hasChest();
   }

   @Environment(EnvType.CLIENT)
   public static enum Type {
      DONKEY(
         Identifier.ofVanilla("textures/entity/horse/donkey.png"),
         EntityModelLayers.DONKEY,
         EntityModelLayers.DONKEY_BABY,
         EquipmentModel.LayerType.DONKEY_SADDLE,
         EntityModelLayers.DONKEY_SADDLE,
         EntityModelLayers.DONKEY_BABY_SADDLE
      ),
      MULE(
         Identifier.ofVanilla("textures/entity/horse/mule.png"),
         EntityModelLayers.MULE,
         EntityModelLayers.MULE_BABY,
         EquipmentModel.LayerType.MULE_SADDLE,
         EntityModelLayers.MULE_SADDLE,
         EntityModelLayers.MULE_BABY_SADDLE
      );

      final Identifier texture;
      final EntityModelLayer adultModelLayer;
      final EntityModelLayer babyModelLayer;
      final EquipmentModel.LayerType saddleLayerType;
      final EntityModelLayer adultSaddleModelLayer;
      final EntityModelLayer babySaddleModelLayer;

      private Type(
         final Identifier texture,
         final EntityModelLayer adultModelLayer,
         final EntityModelLayer babyModelLayer,
         final EquipmentModel.LayerType saddleLayerType,
         final EntityModelLayer adultSaddleModelLayer,
         final EntityModelLayer babySaddleModelLayer
      ) {
         this.texture = texture;
         this.adultModelLayer = adultModelLayer;
         this.babyModelLayer = babyModelLayer;
         this.saddleLayerType = saddleLayerType;
         this.adultSaddleModelLayer = adultSaddleModelLayer;
         this.babySaddleModelLayer = babySaddleModelLayer;
      }
   }
}
