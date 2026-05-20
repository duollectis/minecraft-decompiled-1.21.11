package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.SwingAnimationType;

@Environment(EnvType.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
   public Arm mainArm = Arm.RIGHT;
   public BipedEntityModel.ArmPose rightArmPose = BipedEntityModel.ArmPose.EMPTY;
   public final ItemRenderState rightHandItemState = new ItemRenderState();
   public ItemStack rightHandItem = ItemStack.EMPTY;
   public BipedEntityModel.ArmPose leftArmPose = BipedEntityModel.ArmPose.EMPTY;
   public final ItemRenderState leftHandItemState = new ItemRenderState();
   public ItemStack leftHandItem = ItemStack.EMPTY;
   public SwingAnimationType swingAnimationType = SwingAnimationType.WHACK;
   public float handSwingProgress;

   public ItemRenderState getMainHandItemState() {
      return this.mainArm == Arm.RIGHT ? this.rightHandItemState : this.leftHandItemState;
   }

   public ItemStack getMainHandItemStack() {
      return this.mainArm == Arm.RIGHT ? this.rightHandItem : this.leftHandItem;
   }

   public ItemStack getItemStackForArm(Arm arm) {
      return arm == Arm.RIGHT ? this.rightHandItem : this.leftHandItem;
   }

   public float method_75468(Arm arm) {
      return 0.0F;
   }

   public static void updateRenderState(LivingEntity entity, ArmedEntityRenderState state, ItemModelManager itemModelManager, float tickProgress) {
      state.mainArm = entity.getMainArm();
      ItemStack itemStack = entity.getMainHandStack();
      state.swingAnimationType = itemStack.getSwingAnimation().type();
      state.handSwingProgress = entity.getHandSwingProgress(tickProgress);
      itemModelManager.updateForLivingEntity(state.rightHandItemState, entity.getStackInArm(Arm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
      itemModelManager.updateForLivingEntity(state.leftHandItemState, entity.getStackInArm(Arm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
      state.leftHandItem = entity.getStackInArm(Arm.LEFT).copy();
      state.rightHandItem = entity.getStackInArm(Arm.RIGHT).copy();
   }
}
