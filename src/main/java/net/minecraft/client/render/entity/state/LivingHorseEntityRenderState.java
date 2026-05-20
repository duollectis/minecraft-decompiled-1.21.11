package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class LivingHorseEntityRenderState extends LivingEntityRenderState {
   public ItemStack saddleStack = ItemStack.EMPTY;
   public ItemStack armorStack = ItemStack.EMPTY;
   public boolean hasPassengers;
   public boolean waggingTail;
   public float eatingGrassAnimationProgress;
   public float angryAnimationProgress;
   public float eatingAnimationProgress;
}
