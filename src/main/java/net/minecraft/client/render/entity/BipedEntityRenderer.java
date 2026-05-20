package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SwingAnimationComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.SwingAnimationType;

@Environment(EnvType.CLIENT)
public abstract class BipedEntityRenderer<T extends MobEntity, S extends BipedEntityRenderState, M extends BipedEntityModel<S>>
   extends AgeableMobEntityRenderer<T, S, M> {
   public BipedEntityRenderer(EntityRendererFactory.Context context, M model, float shadowRadius) {
      this(context, model, model, shadowRadius);
   }

   public BipedEntityRenderer(EntityRendererFactory.Context context, M model, M babyModel, float scale) {
      this(context, model, babyModel, scale, HeadFeatureRenderer.HeadTransformation.DEFAULT);
   }

   public BipedEntityRenderer(
      EntityRendererFactory.Context context, M model, M babyModel, float scale, HeadFeatureRenderer.HeadTransformation headTransformation
   ) {
      super(context, model, babyModel, scale);
      this.addFeature(new HeadFeatureRenderer<>(this, context.getEntityModels(), context.getPlayerSkinCache(), headTransformation));
      this.addFeature(new ElytraFeatureRenderer<>(this, context.getEntityModels(), context.getEquipmentRenderer()));
      this.addFeature(new HeldItemFeatureRenderer<>(this));
   }

   protected BipedEntityModel.ArmPose getArmPose(T entity, Arm arm) {
      ItemStack itemStack = entity.getStackInArm(arm);
      SwingAnimationComponent swingAnimationComponent = itemStack.get(DataComponentTypes.SWING_ANIMATION);
      if (swingAnimationComponent != null && swingAnimationComponent.type() == SwingAnimationType.STAB && entity.handSwinging) {
         return BipedEntityModel.ArmPose.SPEAR;
      } else {
         return itemStack.isIn(ItemTags.SPEARS) ? BipedEntityModel.ArmPose.SPEAR : BipedEntityModel.ArmPose.EMPTY;
      }
   }

   public void updateRenderState(T mobEntity, S bipedEntityRenderState, float f) {
      super.updateRenderState(mobEntity, bipedEntityRenderState, f);
      updateBipedRenderState(mobEntity, bipedEntityRenderState, f, this.itemModelResolver);
      bipedEntityRenderState.leftArmPose = this.getArmPose(mobEntity, Arm.LEFT);
      bipedEntityRenderState.rightArmPose = this.getArmPose(mobEntity, Arm.RIGHT);
   }

   public static void updateBipedRenderState(LivingEntity entity, BipedEntityRenderState state, float tickProgress, ItemModelManager itemModelResolver) {
      ArmedEntityRenderState.updateRenderState(entity, state, itemModelResolver, tickProgress);
      state.isInSneakingPose = entity.isInSneakingPose();
      state.isGliding = entity.isGliding();
      state.isSwimming = entity.isInSwimmingPose();
      state.hasVehicle = entity.hasVehicle();
      state.limbAmplitudeInverse = 1.0F;
      if (state.isGliding) {
         state.limbAmplitudeInverse = (float)entity.getVelocity().lengthSquared();
         state.limbAmplitudeInverse /= 0.2F;
         state.limbAmplitudeInverse = state.limbAmplitudeInverse * (state.limbAmplitudeInverse * state.limbAmplitudeInverse);
      }

      if (state.limbAmplitudeInverse < 1.0F) {
         state.limbAmplitudeInverse = 1.0F;
      }

      state.leaningPitch = entity.getLeaningPitch(tickProgress);
      state.preferredArm = getPreferredArm(entity);
      state.activeHand = entity.getActiveHand();
      state.crossbowPullTime = CrossbowItem.getPullTime(entity.getActiveItem(), entity);
      state.itemUseTime = entity.getItemUseTime(tickProgress);
      state.isUsingItem = entity.isUsingItem();
      state.leftWingPitch = entity.elytraFlightController.leftWingPitch(tickProgress);
      state.leftWingYaw = entity.elytraFlightController.leftWingYaw(tickProgress);
      state.leftWingRoll = entity.elytraFlightController.leftWingRoll(tickProgress);
      state.equippedHeadStack = getEquippedStack(entity, EquipmentSlot.HEAD);
      state.equippedChestStack = getEquippedStack(entity, EquipmentSlot.CHEST);
      state.equippedLegsStack = getEquippedStack(entity, EquipmentSlot.LEGS);
      state.equippedFeetStack = getEquippedStack(entity, EquipmentSlot.FEET);
   }

   private static ItemStack getEquippedStack(LivingEntity entity, EquipmentSlot slot) {
      ItemStack itemStack = entity.getEquippedStack(slot);
      return ArmorFeatureRenderer.hasModel(itemStack, slot) ? itemStack.copy() : ItemStack.EMPTY;
   }

   private static Arm getPreferredArm(LivingEntity entity) {
      Arm arm = entity.getMainArm();
      return entity.preferredHand == Hand.MAIN_HAND ? arm : arm.getOpposite();
   }
}
