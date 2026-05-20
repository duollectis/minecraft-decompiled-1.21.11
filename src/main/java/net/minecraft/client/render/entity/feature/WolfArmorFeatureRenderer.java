package net.minecraft.client.render.entity.feature;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.render.entity.state.WolfEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.passive.Cracks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WolfArmorFeatureRenderer extends FeatureRenderer<WolfEntityRenderState, WolfEntityModel> {
   private final WolfEntityModel model;
   private final WolfEntityModel babyModel;
   private final EquipmentRenderer equipmentRenderer;
   private static final Map<Cracks.CrackLevel, Identifier> CRACK_TEXTURES = Map.of(
      Cracks.CrackLevel.LOW,
      Identifier.ofVanilla("textures/entity/wolf/wolf_armor_crackiness_low.png"),
      Cracks.CrackLevel.MEDIUM,
      Identifier.ofVanilla("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
      Cracks.CrackLevel.HIGH,
      Identifier.ofVanilla("textures/entity/wolf/wolf_armor_crackiness_high.png")
   );

   public WolfArmorFeatureRenderer(
      FeatureRendererContext<WolfEntityRenderState, WolfEntityModel> context, LoadedEntityModels loader, EquipmentRenderer equipmentRenderer
   ) {
      super(context);
      this.model = new WolfEntityModel(loader.getModelPart(EntityModelLayers.WOLF_ARMOR));
      this.babyModel = new WolfEntityModel(loader.getModelPart(EntityModelLayers.WOLF_BABY_ARMOR));
      this.equipmentRenderer = equipmentRenderer;
   }

   public void render(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, WolfEntityRenderState wolfEntityRenderState, float f, float g
   ) {
      ItemStack itemStack = wolfEntityRenderState.bodyArmor;
      EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
      if (equippableComponent != null && !equippableComponent.assetId().isEmpty()) {
         WolfEntityModel wolfEntityModel = wolfEntityRenderState.baby ? this.babyModel : this.model;
         this.equipmentRenderer
            .render(
               EquipmentModel.LayerType.WOLF_BODY,
               equippableComponent.assetId().get(),
               wolfEntityModel,
               wolfEntityRenderState,
               itemStack,
               matrixStack,
               orderedRenderCommandQueue,
               i,
               wolfEntityRenderState.outlineColor
            );
         this.renderCracks(matrixStack, orderedRenderCommandQueue, i, itemStack, wolfEntityModel, wolfEntityRenderState);
      }
   }

   private void renderCracks(
      MatrixStack matrices,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light,
      ItemStack stack,
      Model<WolfEntityRenderState> model,
      WolfEntityRenderState wolfEntityRenderState
   ) {
      Cracks.CrackLevel crackLevel = Cracks.WOLF_ARMOR.getCrackLevel(stack);
      if (crackLevel != Cracks.CrackLevel.NONE) {
         Identifier identifier = CRACK_TEXTURES.get(crackLevel);
         orderedRenderCommandQueue.submitModel(
            model,
            wolfEntityRenderState,
            matrices,
            RenderLayers.armorTranslucent(identifier),
            light,
            OverlayTexture.DEFAULT_UV,
            wolfEntityRenderState.outlineColor,
            null
         );
      }
   }
}
