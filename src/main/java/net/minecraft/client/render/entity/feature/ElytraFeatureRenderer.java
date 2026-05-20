package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ElytraFeatureRenderer<S extends BipedEntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {
   private final ElytraEntityModel model;
   private final ElytraEntityModel babyModel;
   private final EquipmentRenderer equipmentRenderer;

   public ElytraFeatureRenderer(FeatureRendererContext<S, M> context, LoadedEntityModels loader, EquipmentRenderer equipmentRenderer) {
      super(context);
      this.model = new ElytraEntityModel(loader.getModelPart(EntityModelLayers.ELYTRA));
      this.babyModel = new ElytraEntityModel(loader.getModelPart(EntityModelLayers.ELYTRA_BABY));
      this.equipmentRenderer = equipmentRenderer;
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g) {
      ItemStack itemStack = bipedEntityRenderState.equippedChestStack;
      EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
      if (equippableComponent != null && !equippableComponent.assetId().isEmpty()) {
         Identifier identifier = getTexture(bipedEntityRenderState);
         ElytraEntityModel elytraEntityModel = bipedEntityRenderState.baby ? this.babyModel : this.model;
         matrixStack.push();
         matrixStack.translate(0.0F, 0.0F, 0.125F);
         this.equipmentRenderer
            .render(
               EquipmentModel.LayerType.WINGS,
               equippableComponent.assetId().get(),
               elytraEntityModel,
               bipedEntityRenderState,
               itemStack,
               matrixStack,
               orderedRenderCommandQueue,
               i,
               identifier,
               bipedEntityRenderState.outlineColor,
               0
            );
         matrixStack.pop();
      }
   }

   private static @Nullable Identifier getTexture(BipedEntityRenderState state) {
      if (state instanceof PlayerEntityRenderState playerEntityRenderState) {
         SkinTextures skinTextures = playerEntityRenderState.skinTextures;
         if (skinTextures.elytra() != null) {
            return skinTextures.elytra().texturePath();
         }

         if (skinTextures.cape() != null && playerEntityRenderState.capeVisible) {
            return skinTextures.cape().texturePath();
         }
      }

      return null;
   }
}
