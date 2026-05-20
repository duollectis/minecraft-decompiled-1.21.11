package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class ArmorFeatureRenderer<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> extends FeatureRenderer<S, M> {
   private final EquipmentModelData<A> field_61804;
   private final EquipmentModelData<A> field_61805;
   private final EquipmentRenderer equipmentRenderer;

   public ArmorFeatureRenderer(
      FeatureRendererContext<S, M> featureRendererContext, EquipmentModelData<A> equipmentModelData, EquipmentRenderer equipmentRenderer
   ) {
      this(featureRendererContext, equipmentModelData, equipmentModelData, equipmentRenderer);
   }

   public ArmorFeatureRenderer(
      FeatureRendererContext<S, M> featureRendererContext,
      EquipmentModelData<A> equipmentModelData,
      EquipmentModelData<A> equipmentModelData2,
      EquipmentRenderer equipmentRenderer
   ) {
      super(featureRendererContext);
      this.field_61804 = equipmentModelData;
      this.field_61805 = equipmentModelData2;
      this.equipmentRenderer = equipmentRenderer;
   }

   public static boolean hasModel(ItemStack stack, EquipmentSlot slot) {
      EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
      return equippableComponent != null && hasModel(equippableComponent, slot);
   }

   private static boolean hasModel(EquippableComponent component, EquipmentSlot slot) {
      return component.assetId().isPresent() && component.slot() == slot;
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g) {
      this.renderArmor(matrixStack, orderedRenderCommandQueue, bipedEntityRenderState.equippedChestStack, EquipmentSlot.CHEST, i, bipedEntityRenderState);
      this.renderArmor(matrixStack, orderedRenderCommandQueue, bipedEntityRenderState.equippedLegsStack, EquipmentSlot.LEGS, i, bipedEntityRenderState);
      this.renderArmor(matrixStack, orderedRenderCommandQueue, bipedEntityRenderState.equippedFeetStack, EquipmentSlot.FEET, i, bipedEntityRenderState);
      this.renderArmor(matrixStack, orderedRenderCommandQueue, bipedEntityRenderState.equippedHeadStack, EquipmentSlot.HEAD, i, bipedEntityRenderState);
   }

   private void renderArmor(
      MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ItemStack stack, EquipmentSlot slot, int light, S bipedEntityRenderState
   ) {
      EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
      if (equippableComponent != null && hasModel(equippableComponent, slot)) {
         A bipedEntityModel = this.getModel(bipedEntityRenderState, slot);
         EquipmentModel.LayerType layerType = this.usesInnerModel(slot) ? EquipmentModel.LayerType.HUMANOID_LEGGINGS : EquipmentModel.LayerType.HUMANOID;
         this.equipmentRenderer
            .render(
               layerType,
               equippableComponent.assetId().orElseThrow(),
               bipedEntityModel,
               bipedEntityRenderState,
               stack,
               matrices,
               orderedRenderCommandQueue,
               light,
               bipedEntityRenderState.outlineColor
            );
      }
   }

   private A getModel(S state, EquipmentSlot slot) {
      return (state.baby ? this.field_61805 : this.field_61804).getModelData(slot);
   }

   private boolean usesInnerModel(EquipmentSlot slot) {
      return slot == EquipmentSlot.LEGS;
   }
}
